package com.okan.strudelmobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.okan.strudelmobile.model.ActiveToken
import com.okan.strudelmobile.model.Arg
import com.okan.strudelmobile.model.Chain
import com.okan.strudelmobile.model.FunctionSpec
import com.okan.strudelmobile.model.GroupSource
import com.okan.strudelmobile.model.MiniSource
import com.okan.strudelmobile.model.Serializer
import com.okan.strudelmobile.model.Transform
import com.okan.strudelmobile.model.Vocabulary

private val BlockShape = RoundedCornerShape(10.dp)

/**
 * One chain rendered as a vertical list of blocks: the source on top, then
 * each transform, then "+ add". Groups render their children indented inside
 * the source block, recursively.
 */
@Composable
fun ChainEditor(
    chain: Chain,
    parentGroupId: String?,
    siblingIndex: Int,
    siblingCount: Int,
    selection: Selection,
    active: Set<ActiveToken>,
    vm: EditorViewModel,
) {
    Column(Modifier.fillMaxWidth()) {
        when (val src = chain.source) {
            is MiniSource -> MiniSourceBlock(
                chain = chain,
                src = src,
                parentGroupId = parentGroupId,
                siblingIndex = siblingIndex,
                siblingCount = siblingCount,
                selected = (selection as? Selection.Source)?.chainId == chain.id,
                activeTokens = active.filter { it.chainId == chain.id },
                vm = vm,
            )
            is GroupSource -> GroupBlock(
                chain = chain,
                src = src,
                parentGroupId = parentGroupId,
                siblingIndex = siblingIndex,
                siblingCount = siblingCount,
                selection = selection,
                active = active,
                vm = vm,
            )
        }

        if (chain.transforms.isNotEmpty()) Spacer(Modifier.height(6.dp))
        ReorderableColumn(
            items = chain.transforms,
            key = { it.id },
            onMove = { id, delta -> vm.moveTransform(id, delta) },
            spacing = 6.dp,
        ) { t, handle, dragging ->
            TransformBlock(
                transform = t,
                selected = (selection as? Selection.Block)?.transformId == t.id,
                dragging = dragging,
                handle = handle,
                vm = vm,
            )
        }

        Spacer(Modifier.height(4.dp))
        AddBlockButton(onAdd = { vm.addTransform(chain.id, it) })
        Spacer(Modifier.height(8.dp))
    }
}

// --- source -------------------------------------------------------------------

@Composable
private fun MiniSourceBlock(
    chain: Chain,
    src: MiniSource,
    parentGroupId: String?,
    siblingIndex: Int,
    siblingCount: Int,
    selected: Boolean,
    activeTokens: List<ActiveToken>,
    vm: EditorViewModel,
) {
    val sounding = activeTokens.isNotEmpty()
    val border = when {
        sounding -> Amber
        selected -> Sky
        else -> MaterialTheme.colorScheme.outline
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(BlockShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(if (sounding || selected) 2.dp else 1.dp, border, BlockShape)
            .clickable { vm.select(Selection.Source(chain.id)) }
            .padding(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FnChooser(current = src.fn, options = Vocabulary.sourceFns, color = Mint, descriptions = Vocabulary.sourceDescriptions) { vm.setSourceFn(chain.id, it) }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { vm.previewChain(chain.id) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.PlayArrow, "Preview", tint = Mint)
            }
            SourceMenu(chain, parentGroupId, siblingIndex, siblingCount, vm)
        }
        OutlinedTextField(
            value = src.pattern,
            onValueChange = { vm.setPattern(chain.id, it) },
            modifier = Modifier
                .fillMaxWidth()
                // The field swallows the tap, so select the block on focus instead.
                .onFocusChanged { if (it.isFocused) vm.select(Selection.Source(chain.id)) },
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = MaterialTheme.typography.bodyLarge.fontSize),
            singleLine = true,
            visualTransformation = HighlightTransformation(activeTokens),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false),
            placeholder = { Text(if (src.fn == "s") "bd sd hh" else "c3 e3 g3") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Sky,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            ),
        )
    }
}

/** Paints the currently-sounding tokens inside the text field. */
private class HighlightTransformation(private val tokens: List<ActiveToken>) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        if (tokens.isEmpty()) return TransformedText(text, OffsetMapping.Identity)
        val builder = AnnotatedString.Builder(text)
        for (t in tokens) {
            val from = t.from.coerceIn(0, text.length)
            val to = t.to.coerceIn(from, text.length)
            if (to > from) builder.addStyle(SpanStyle(background = Amber, color = Ink, fontWeight = FontWeight.Bold), from, to)
        }
        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}

@Composable
private fun SourceMenu(chain: Chain, parentGroupId: String?, siblingIndex: Int, siblingCount: Int, vm: EditorViewModel) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.MoreVert, "More") }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("Wrap in stack (layers)") }, onClick = { open = false; vm.wrapInGroup(chain.id, "stack") })
            DropdownMenuItem(text = { Text("Wrap in seq (one after another)") }, onClick = { open = false; vm.wrapInGroup(chain.id, "seq") })
            if (parentGroupId != null) {
                if (siblingIndex > 0) DropdownMenuItem(text = { Text("Move up") }, onClick = { open = false; vm.moveLayer(parentGroupId, chain.id, -1) })
                if (siblingIndex < siblingCount - 1) DropdownMenuItem(text = { Text("Move down") }, onClick = { open = false; vm.moveLayer(parentGroupId, chain.id, +1) })
                DropdownMenuItem(text = { Text("Remove layer", color = Coral) }, onClick = { open = false; vm.removeLayer(parentGroupId, chain.id) })
            }
        }
    }
}

// --- group --------------------------------------------------------------------

@Composable
private fun GroupBlock(
    chain: Chain,
    src: GroupSource,
    parentGroupId: String?,
    siblingIndex: Int,
    siblingCount: Int,
    selection: Selection,
    active: Set<ActiveToken>,
    vm: EditorViewModel,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(BlockShape)
            .border(1.dp, Coral.copy(alpha = 0.6f), BlockShape)
            .padding(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FnChooser(current = src.fn, options = Vocabulary.groupFns, color = Coral, descriptions = Vocabulary.groupDescriptions) { vm.setGroupFn(chain.id, it) }
            Text(
                "  ${src.children.size} layers",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            if (parentGroupId != null) {
                SourceMenu(chain, parentGroupId, siblingIndex, siblingCount, vm)
            }
        }
        Spacer(Modifier.height(6.dp))
        ReorderableColumn(
            items = src.children,
            key = { it.id },
            onMove = { id, delta -> vm.moveLayer(chain.id, id, delta) },
        ) { child, handle, dragging ->
            val i = src.children.indexOf(child)
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(if (dragging) InkRaised else Color.Transparent, BlockShape),
            ) {
                // The layer's grab handle.
                Box(
                    Modifier
                        .width(28.dp)
                        .padding(top = 8.dp)
                        .then(handle),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Icon(Icons.Default.DragIndicator, "Drag layer", tint = Coral.copy(alpha = 0.8f))
                }
                Column(Modifier.weight(1f)) {
                    ChainEditor(
                        chain = child,
                        parentGroupId = chain.id,
                        siblingIndex = i,
                        siblingCount = src.children.size,
                        selection = selection,
                        active = active,
                        vm = vm,
                    )
                }
            }
        }
        TextButton(onClick = { vm.addLayer(chain.id) }) {
            Icon(Icons.Default.Add, null, tint = Coral)
            Text(" Add layer", color = Coral)
        }
    }
}

// --- transform ----------------------------------------------------------------

@Composable
private fun TransformBlock(
    transform: Transform,
    selected: Boolean,
    dragging: Boolean,
    handle: Modifier,
    vm: EditorViewModel,
) {
    val spec = Vocabulary.spec(transform.fn)
    val color = categoryColor(spec?.category)
    val border = when {
        dragging -> Amber
        selected -> Sky
        else -> MaterialTheme.colorScheme.outline
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(BlockShape)
            .background(if (dragging) InkLine else MaterialTheme.colorScheme.surfaceVariant)
            .border(if (selected || dragging) 2.dp else 1.dp, border, BlockShape)
            .clickable { vm.select(Selection.Block(transform.id)) }
            .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DragHandle(handle)
        Box(
            Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(color),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            ".${transform.fn}",
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "(" + transform.args.joinToString(", ") { argLabel(it) } + ")",
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )
        IconButton(onClick = { vm.removeTransform(transform.id) }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, "Remove", tint = Coral)
        }
    }
}

private fun argLabel(arg: Arg): String = when (arg) {
    is Arg.Num -> Serializer.formatNumber(arg.value)
    is Arg.Str -> "\"${arg.value}\""
}

fun categoryColor(c: FunctionSpec.Category?): Color = when (c) {
    FunctionSpec.Category.TIME -> Sky
    FunctionSpec.Category.SOUND -> Mint
    FunctionSpec.Category.PITCH -> Amber
    FunctionSpec.Category.FILTER -> Color(0xFFB388FF)
    FunctionSpec.Category.SPACE -> Color(0xFF80DEEA)
    FunctionSpec.Category.DYNAMICS -> Coral
    FunctionSpec.Category.STRUCTURE -> Color(0xFFFFAB91)
    null -> Color.Gray
}

// --- shared bits --------------------------------------------------------------

@Composable
private fun DragHandle(handle: Modifier) {
    Icon(
        Icons.Default.DragIndicator,
        "Drag to reorder",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(32.dp).then(handle).padding(4.dp),
    )
}

@Composable
private fun FnChooser(
    current: String,
    options: List<String>,
    color: Color,
    descriptions: Map<String, String> = emptyMap(),
    onPick: (String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(color.copy(alpha = 0.18f))
                .clickable { open = true }
                .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(current, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = color)
            Icon(Icons.Default.ArrowDropDown, null, tint = color)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { fn ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(fn, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                            descriptions[fn]?.let {
                                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(240.dp))
                            }
                        }
                    },
                    onClick = { open = false; onPick(fn) },
                )
            }
        }
    }
}

@Composable
private fun AddBlockButton(onAdd: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }) {
            Icon(Icons.Default.Add, null)
            Text(" Add block")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            var lastCategory: FunctionSpec.Category? = null
            Vocabulary.transforms.forEach { spec ->
                if (spec.category != lastCategory) {
                    lastCategory = spec.category
                    Text(
                        spec.category.name.lowercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = categoryColor(spec.category),
                        modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 2.dp),
                    )
                }
                DropdownMenuItem(
                    text = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(".${spec.name}", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.width(8.dp))
                                Text(spec.label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            }
                            Text(
                                spec.description,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                modifier = Modifier.width(260.dp),
                            )
                        }
                    },
                    onClick = { open = false; onAdd(spec.name) },
                )
            }
        }
    }
}
