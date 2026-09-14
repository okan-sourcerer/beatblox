package com.okan.strudelmobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
                is ParamSpec.Number -> NumberParam(param, (arg as? Arg.Num)?.value ?: param.default,
                    onChange = { vm.setArg(transform.id, index, Arg.Num(it)) },
                    onCommit = { vm.setArgFinal(transform.id, index, Arg.Num(it)) })
                is ParamSpec.Choice -> ChoiceParam(param, (arg as? Arg.Str)?.value ?: param.default) {
                    vm.setArgFinal(transform.id, index, Arg.Str(it))
                }
                is ParamSpec.Text -> TextParam(param, (arg as? Arg.Str)?.value ?: param.default) {
                    vm.setArg(transform.id, index, Arg.Str(it))
                }
            }
        }
    }
}

@Composable
private fun NumberParam(param: ParamSpec.Number, value: Double, onChange: (Double) -> Unit, onCommit: (Double) -> Unit) {
    var local by remember(param.name, value) { mutableFloatStateOf(value.toFloat()) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(param.name, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
        Text(Serializer.formatNumber(snap(local.toDouble(), param)), fontFamily = FontFamily.Monospace, color = Amber)
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
private fun TextParam(param: ParamSpec.Text, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(param.name) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false),
    )
}
