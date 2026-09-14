package com.okan.strudelmobile.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.okan.strudelmobile.engine.StrudelEngine
import com.okan.strudelmobile.model.Arg
import com.okan.strudelmobile.model.Chain
import com.okan.strudelmobile.model.GroupSource
import com.okan.strudelmobile.model.MiniSource
import com.okan.strudelmobile.model.Serializer
import com.okan.strudelmobile.model.Transform
import com.okan.strudelmobile.model.TreeOps
import com.okan.strudelmobile.model.Vocabulary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface Selection {
    data object None : Selection
    data class Source(val chainId: String) : Selection
    data class Block(val transformId: String) : Selection
}

enum class Panel(val label: String) { NOTES("Notes"), SOUNDS("Sounds"), PARAMS("Params"), CODE("Code") }

class EditorViewModel(app: Application) : AndroidViewModel(app) {

    val engine = StrudelEngine(app)

    private val _root = MutableStateFlow(defaultPattern())
    val root: StateFlow<Chain> = _root.asStateFlow()

    private val _selection = MutableStateFlow<Selection>(Selection.Source(_root.value.let { TreeOps.miniSources(it).first().id }))
    val selection: StateFlow<Selection> = _selection.asStateFlow()

    private val _panel = MutableStateFlow(Panel.NOTES)
    val panel: StateFlow<Panel> = _panel.asStateFlow()

    private val _code = MutableStateFlow(Serializer.serialize(_root.value).code)
    val code: StateFlow<String> = _code.asStateFlow()

    private val _cpm = MutableStateFlow(30.0)
    val cpm: StateFlow<Double> = _cpm.asStateFlow()

    private var pushJob: Job? = null
    private var pushedOnce = false

    init {
        // Push the initial pattern once the engine comes up, without starting it.
        viewModelScope.launch {
            engine.ready.collect { ready ->
                if (ready && !pushedOnce) {
                    pushedOnce = true
                    push(immediate = true)
                }
            }
        }
    }

    // --- transport --------------------------------------------------------------

    fun togglePlay() {
        if (engine.playing.value) engine.stop()
        else {
            // (Re)evaluate so a pattern edited while stopped is what plays.
            push(immediate = true, autostart = true)
        }
    }

    fun hush() = engine.hush()

    fun setCpm(v: Double) {
        _cpm.value = v
        engine.setCpm(v)
    }

    // --- selection / panel ----------------------------------------------------

    fun select(sel: Selection) {
        _selection.value = sel
        // Jump to the panel that makes sense for what was tapped.
        when (sel) {
            is Selection.Source -> {
                val src = TreeOps.find(_root.value, sel.chainId)?.source as? MiniSource ?: return
                _panel.value = if (src.fn == "s") Panel.SOUNDS else Panel.NOTES
            }
            is Selection.Block -> _panel.value = Panel.PARAMS
            Selection.None -> Unit
        }
    }

    fun setPanel(p: Panel) {
        _panel.value = p
    }

    /** The mini-source chain the piano/sound pickers write into. */
    fun targetChain(): Chain? = when (val s = _selection.value) {
        is Selection.Source -> TreeOps.find(_root.value, s.chainId)
        is Selection.Block -> TreeOps.chainOfTransform(_root.value, s.transformId)
        Selection.None -> null
    }?.takeIf { it.source is MiniSource }

    fun selectedTransform(): Transform? {
        val s = _selection.value as? Selection.Block ?: return null
        return TreeOps.chainOfTransform(_root.value, s.transformId)?.transforms?.firstOrNull { it.id == s.transformId }
    }

    // --- tree edits -------------------------------------------------------------

    private fun edit(immediate: Boolean = false, f: (Chain) -> Chain) {
        _root.update(f)
        push(immediate)
    }

    fun setPattern(chainId: String, pattern: String) = edit { TreeOps.setPattern(it, chainId, pattern) }

    fun setSourceFn(chainId: String, fn: String) = edit(immediate = true) { TreeOps.setSourceFn(it, chainId, fn) }

    /** Append a token (note, sound name, rest) to a chain's pattern string. */
    fun appendToken(chainId: String, token: String) = edit(immediate = true) { root ->
        val src = TreeOps.find(root, chainId)?.source as? MiniSource ?: return@edit root
        val joined = if (src.pattern.isBlank()) token else src.pattern.trimEnd() + " " + token
        TreeOps.setPattern(root, chainId, joined)
    }

    fun deleteLastToken(chainId: String) = edit(immediate = true) { root ->
        val src = TreeOps.find(root, chainId)?.source as? MiniSource ?: return@edit root
        val tokens = src.pattern.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        TreeOps.setPattern(root, chainId, tokens.dropLast(1).joinToString(" "))
    }

    fun addTransform(chainId: String, fn: String) {
        val t = Vocabulary.newTransform(fn)
        edit(immediate = true) { TreeOps.addTransform(it, chainId, t) }
        select(Selection.Block(t.id))
    }

    /** Point a note/n chain at [sound]: update its `.s()` block if it has one, else add one. */
    fun setSoundTransform(chainId: String, sound: String) = edit(immediate = true) { root ->
        val chain = TreeOps.find(root, chainId) ?: return@edit root
        val existing = chain.transforms.firstOrNull { it.fn == "s" }
        if (existing != null) TreeOps.setArg(root, existing.id, 0, Arg.Str(sound))
        else TreeOps.addTransform(root, chainId, Transform(fn = "s", args = listOf(Arg.Str(sound))))
    }

    fun removeTransform(transformId: String) {
        edit(immediate = true) { TreeOps.removeTransform(it, transformId) }
        if ((_selection.value as? Selection.Block)?.transformId == transformId) _selection.value = Selection.None
    }

    fun moveTransform(transformId: String, delta: Int) = edit(immediate = true) { TreeOps.moveTransform(it, transformId, delta) }

    /** Slider drags call this continuously; the push is debounced. */
    fun setArg(transformId: String, index: Int, arg: Arg) = edit { TreeOps.setArg(it, transformId, index, arg) }

    fun setArgFinal(transformId: String, index: Int, arg: Arg) = edit(immediate = true) { TreeOps.setArg(it, transformId, index, arg) }

    fun wrapInGroup(chainId: String, groupFn: String) = edit(immediate = true) { TreeOps.wrapInGroup(it, chainId, groupFn) }

    fun addLayer(groupId: String) {
        val child = Chain(source = MiniSource("s", "hh*4"))
        edit(immediate = true) { TreeOps.addChild(it, groupId, child) }
        select(Selection.Source(child.id))
    }

    fun removeLayer(groupId: String, childId: String) {
        edit(immediate = true) { TreeOps.removeChild(it, groupId, childId) }
        _selection.value = Selection.Source(groupId)
    }

    fun moveLayer(groupId: String, childId: String, delta: Int) = edit(immediate = true) { TreeOps.moveChild(it, groupId, childId, delta) }

    fun setGroupFn(groupId: String, fn: String) = edit(immediate = true) { TreeOps.setSourceFn(it, groupId, fn) }

    fun reset() {
        edit(immediate = true) { defaultPattern() }
        _selection.value = Selection.Source(TreeOps.miniSources(_root.value).first().id)
    }

    // --- preview ------------------------------------------------------------------

    /** Hear this chain alone (with its ancestors' transforms) for one cycle. */
    fun previewChain(chainId: String) {
        val s = Serializer.serializePreview(_root.value, chainId) ?: return
        engine.preview(s.code, 1)
    }

    /** Audition a single token through the chain — e.g. a piano key. */
    fun previewToken(chainId: String, token: String) {
        val s = Serializer.serializePreview(_root.value, chainId, sourceOverride = token) ?: return
        engine.preview(s.code, 1)
    }

    /** Audition a sound name on its own. */
    fun previewSound(name: String) = engine.preview("s(\"$name\")", 1)

    // --- push to engine -------------------------------------------------------------

    private fun push(immediate: Boolean = false, autostart: Boolean? = null) {
        val serialized = Serializer.serialize(_root.value)
        _code.value = serialized.code
        pushJob?.cancel()
        pushJob = viewModelScope.launch {
            if (!immediate) delay(200)
            engine.setPattern(serialized.code, serialized.locationsJson(), autostart ?: engine.playing.value)
        }
    }

    override fun onCleared() {
        engine.destroy()
    }

    private fun defaultPattern(): Chain = Chain(
        source = GroupSource(
            "stack",
            listOf(
                Chain(
                    source = MiniSource("s", "bd [~ sd] hh*2 sd"),
                    transforms = listOf(Transform(fn = "bank", args = listOf(Arg.Str("RolandTR909")))),
                ),
                Chain(
                    source = MiniSource("note", "c3 eb3 g3 bb3"),
                    transforms = listOf(
                        Transform(fn = "s", args = listOf(Arg.Str("piano"))),
                        Transform(fn = "room", args = listOf(Arg.Num(0.4))),
                    ),
                ),
            ),
        ),
    )
}
