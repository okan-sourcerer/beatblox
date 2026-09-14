package com.okan.strudelmobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.okan.strudelmobile.model.Arg
import com.okan.strudelmobile.model.ParamSpec
import com.okan.strudelmobile.model.Serializer
import com.okan.strudelmobile.model.SoundDescriptions
import com.okan.strudelmobile.model.Vocabulary
import kotlin.math.roundToInt

/** Sliders / dropdowns / text for the selected transform block's parameters. */
@Composable
fun ParamsPanel(vm: EditorViewModel) {
    val root by vm.root.collectAsStateWithLifecycle()
    val selection by vm.selection.collectAsStateWithLifecycle()
    val transform = remember(root, selection) { vm.selectedTransform() }

    if (transform == null) {
        Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Text("Tap a .block to edit its parameters.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    val spec = Vocabulary.spec(transform.fn)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(".${transform.fn}", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = {
                val chain = vm.targetChain()
                if (chain != null) vm.previewChain(chain.id)
            }) { Text("▶ preview chain") }
        }
        if (spec != null && spec.description.isNotEmpty()) {
            Text(spec.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (spec == null || spec.params.isEmpty()) {
            Text("No parameters.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return
        }
        // Explain what the current sound / bank actually is.
        if (transform.fn == "s" || transform.fn == "bank") {
            val value = (transform.args.firstOrNull() as? Arg.Str)?.value.orEmpty()
            val info = if (transform.fn == "bank") SoundDescriptions.describeBank(value) else SoundDescriptions.describe(value, "sample").text
            if (value.isNotBlank()) Text(info, style = MaterialTheme.typography.labelSmall, color = Amber)
        }
        spec.params.forEachIndexed { index, param ->
            Spacer(Modifier.height(8.dp))
            val arg = transform.args.getOrNull(index)
            when (param) {
                is ParamSpec.Number -> NumberParam(
                    param = param,
                    arg = arg,
                    onChange = { vm.setArg(transform.id, index, Arg.Num(it)) },
                    onCommit = { vm.setArgFinal(transform.id, index, Arg.Num(it)) },
                    onPattern = { vm.setArg(transform.id, index, Arg.Str(it)) },
                    onToggle = { toPattern ->
                        val current = (arg as? Arg.Num)?.value ?: param.default
                        vm.setArgFinal(transform.id, index, if (toPattern) Arg.Str(Serializer.formatNumber(current)) else Arg.Num(current))
                    },
                )
                is ParamSpec.Fn -> Text(
                    "${param.name}: edit the nested blocks inside this block in the editor above.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                is ParamSpec.Choice -> ChoiceParam(param, (arg as? Arg.Str)?.value ?: param.default) {
                    vm.setArgFinal(transform.id, index, Arg.Str(it))
                }
                is ParamSpec.Text -> TextParam(
                    param = param,
                    value = (arg as? Arg.Str)?.value ?: param.default,
                    vm = vm,
                    onChange = { vm.setArg(transform.id, index, Arg.Str(it)) },
                    onPick = { vm.setArgFinal(transform.id, index, Arg.Str(it)) },
                )
            }
        }
    }
}

/**
 * A number can also be a mini-notation *pattern* of numbers ("<400 2000>",
 * "1 0.5"), which Strudel evaluates per step. The ⌨ toggle switches between
 * the slider (Arg.Num) and a text field (Arg.Str).
 */
@Composable
private fun NumberParam(
    param: ParamSpec.Number,
    arg: Arg?,
    onChange: (Double) -> Unit,
    onCommit: (Double) -> Unit,
    onPattern: (String) -> Unit,
    onToggle: (toPattern: Boolean) -> Unit,
) {
    val isPattern = arg is Arg.Str
    val value = (arg as? Arg.Num)?.value ?: param.default
    var local by remember(param.name, value) { mutableFloatStateOf(value.toFloat()) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(param.name, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
        if (!isPattern) Text(Serializer.formatNumber(snap(local.toDouble(), param)), fontFamily = FontFamily.Monospace, color = Amber)
        TextButton(onClick = { onToggle(!isPattern) }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
            Text(if (isPattern) "slider" else "pattern", style = MaterialTheme.typography.labelSmall)
        }
    }
    if (isPattern) {
        OutlinedTextField(
            value = (arg as Arg.Str).value,
            onValueChange = onPattern,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("e.g. <${Serializer.formatNumber(param.min)} ${Serializer.formatNumber(param.max)}>") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false),
            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace),
        )
        Text(
            "a mini-notation pattern of values: \"<a b>\" alternates per cycle, \"a b\" changes per step",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    Slider(
        value = local,
        onValueChange = { local = it; onChange(snap(it.toDouble(), param)) },
        onValueChangeFinished = { onCommit(snap(local.toDouble(), param)) },
        valueRange = param.min.toFloat()..param.max.toFloat(),
    )
}

private fun snap(v: Double, p: ParamSpec.Number): Double {
    val n = ((v - p.min) / p.step).roundToInt()
    return (p.min + n * p.step).coerceIn(p.min, p.max)
}

@Composable
private fun ChoiceParam(param: ParamSpec.Choice, value: String, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(param.name, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
        Box {
            OutlinedButton(onClick = { open = true }) { Text(value, fontFamily = FontFamily.Monospace) }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                param.options.forEach { opt ->
                    DropdownMenuItem(text = { Text(opt, fontFamily = FontFamily.Monospace) }, onClick = { open = false; onPick(opt) })
                }
            }
        }
    }
}

@Composable
private fun TextParam(
    param: ParamSpec.Text,
    value: String,
    vm: EditorViewModel,
    onChange: (String) -> Unit,
    onPick: (String) -> Unit,
) {
    val sounds by vm.engine.sounds.collectAsStateWithLifecycle()
    when (param.kind) {
        ParamSpec.TextKind.SOUND -> {
            Text(param.name, style = MaterialTheme.typography.labelLarge)
            SoundPicker(sounds = sounds, current = value, onPick = onPick, onPreview = { vm.previewSound(it) })
            return
        }
        ParamSpec.TextKind.BANK -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(param.name, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                BankChooser(current = value, banks = remember(sounds) { bankNames(sounds) }) { onPick(it ?: "") }
            }
            SmallHint("Sound names in this chain (bd, sd, hh…) are taken from this machine. Use the Sounds tab to see them.")
            return
        }
        ParamSpec.TextKind.NOTE_NAME -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(param.name, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                NoteDropdown(current = value, onPick = onPick)
            }
            return
        }
        ParamSpec.TextKind.SAMPLE_INDEX -> {
            // Show the valid indices for the chain's current sound.
            val chain = vm.targetChain()
            val soundName = chain?.transforms?.firstOrNull { it.fn == "s" }?.args?.firstOrNull()?.let { (it as? Arg.Str)?.value }
                ?: (chain?.source as? com.okan.strudelmobile.model.MiniSource)?.takeIf { it.fn == "s" }?.pattern?.trim()?.split(" ")?.firstOrNull()
            val count = sounds.firstOrNull { it.name == soundName }?.count
            if (soundName != null && count != null) {
                SmallHint("\"$soundName\" has $count sample${if (count == 1) "" else "s"}: valid indices 0…${count - 1}")
                PresetChips(presets = (0 until count.coerceAtMost(16)).map { it.toString() }, current = value, onPick = onPick)
            } else {
                SmallHint("Sample number inside the sound (0 = first). With a .scale() block: the scale degree.")
            }
        }
        ParamSpec.TextKind.MINI -> Unit
    }
    if (param.presets.isNotEmpty()) PresetChips(presets = param.presets, current = value, onPick = onPick)
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(param.name) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false),
    )
    if (param.isMini) SmallHint("mini-notation: space = steps, ~ = rest, <a b> = alternate per cycle, a*2 = repeat. See ? for more.")
}
