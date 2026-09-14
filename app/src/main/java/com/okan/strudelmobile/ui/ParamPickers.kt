package com.okan.strudelmobile.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.okan.strudelmobile.engine.SoundInfo
import com.okan.strudelmobile.model.SoundDescriptions

/**
 * Pickers for text parameters, so nobody has to guess the vocabulary.
 */

/** `bank: RolandTR909 ▾` — every drum machine with a one-line description. */
@Composable
fun BankChooser(current: String?, banks: List<String>, onPick: (String?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }) {
            Text("bank: ${current ?: "none"} ▾", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("none — plain sound names") }, onClick = { open = false; onPick(null) })
            banks.forEach { b ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(b, fontFamily = FontFamily.Monospace)
                            Text(
                                SoundDescriptions.describeBank(b),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    },
                    onClick = { open = false; onPick(b) },
                )
            }
        }
    }
}

/** Bank names as Strudel writes them (CamelCase), derived from the lowercase sound-map keys. */
fun bankNames(sounds: List<SoundInfo>): List<String> =
    sounds.mapNotNull { s -> s.name.substringBefore('_', "").takeIf { it.isNotEmpty() } }
        .groupingBy { it }.eachCount().filterValues { it >= 4 }.keys.sorted()

/** Inline searchable sound list for a `.s()` block. */
@Composable
fun SoundPicker(sounds: List<SoundInfo>, current: String, onPick: (String) -> Unit, onPreview: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    val described = remember(sounds) { sounds.map { it to SoundDescriptions.describe(it.name, it.type) } }
    val filtered = remember(described, query) {
        if (query.isBlank()) described
        else described.filter { (s, d) -> s.name.contains(query, true) || d.text.contains(query, true) }
    }
    val currentInfo = described.firstOrNull { it.first.name == current }?.second
    if (currentInfo != null) {
        Text(currentInfo.text, style = MaterialTheme.typography.labelSmall, color = Amber)
    }
    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("search ${sounds.size} sounds — name or description") },
    )
    LazyColumn(Modifier.fillMaxWidth().height(220.dp)) {
        items(filtered, key = { it.first.name }) { (s, d) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onPick(s.name) }
                    .padding(start = 8.dp, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        s.name,
                        fontFamily = FontFamily.Monospace,
                        color = if (s.name == current) Amber else MaterialTheme.colorScheme.onSurface,
                    )
                    Text(d.text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                IconButton(onClick = { onPreview(s.name) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.PlayArrow, "Preview", tint = Mint)
                }
            }
        }
    }
}

/** Tappable presets above a text field. */
@Composable
fun PresetChips(presets: List<String>, current: String, onPick: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
        presets.forEach { p ->
            FilterChip(
                selected = p == current,
                onClick = { onPick(p) },
                label = { Text(p, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelSmall) },
            )
        }
    }
}

/** A dropdown of note names for pitch-valued params such as `anchor`. */
@Composable
fun NoteDropdown(current: String, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val notes = remember { (2..6).flatMap { o -> listOf("c", "d", "e", "f", "g", "a", "b").map { "$it$o" } } }
    Box {
        OutlinedButton(onClick = { open = true }) { Text(current, fontFamily = FontFamily.Monospace) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            notes.forEach { n ->
                DropdownMenuItem(
                    text = { Text(n + if (n == "c4") "  (middle C)" else "", fontFamily = FontFamily.Monospace) },
                    onClick = { open = false; onPick(n) },
                )
            }
        }
    }
}

@Composable
fun SmallHint(text: String) {
    Spacer(Modifier.height(2.dp))
    Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
