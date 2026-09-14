package com.okan.strudelmobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.okan.strudelmobile.model.MiniSource
import com.okan.strudelmobile.model.Vocabulary

private val WhiteKeys = listOf("c", "d", "e", "f", "g", "a", "b")
// Black key name and which white-key gap it sits after (index into WhiteKeys).
private val BlackKeys = listOf("c#" to 0, "d#" to 1, "f#" to 3, "g#" to 4, "a#" to 5)

/**
 * Piano-style picker (for `note`) or degree pad (for `n`) that appends tokens
 * to the selected chain's pattern and auditions each one through that chain.
 */
@Composable
fun NotesPanel(vm: EditorViewModel) {
    val root by vm.root.collectAsStateWithLifecycle()
    val selection by vm.selection.collectAsStateWithLifecycle()
    // Re-resolve on every tree/selection change so the target follows edits.
    val target = remember(root, selection) { vm.targetChain() }
    val src = target?.source as? MiniSource

    if (target == null || src == null) {
        Hint("Select a note, n or chord block to pick notes.")
        return
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp)) {
        when (src.fn) {
            "s" -> {
                Hint("This block is a sound (s). Use the Sounds tab, or switch it to note / n.")
                return
            }
            "n" -> DegreePad(target.id, vm)
            "chord" -> ChordPad(target.id, target.transforms.any { it.fn == "voicing" }, vm)
            else -> Piano(target.id, vm)
        }
    }
}

@Composable
private fun Hint(text: String) {
    Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun TokenTools(chainId: String, vm: EditorViewModel, extra: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        extra()
        Spacer(Modifier.weight(1f))
        OutlinedButton(
            onClick = { vm.appendToken(chainId, "~") },
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
        ) { Text("~", fontFamily = FontFamily.Monospace) }
        OutlinedButton(
            onClick = { vm.deleteLastToken(chainId) },
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
        ) { Text("⌫") }
    }
}

@Composable
private fun Piano(chainId: String, vm: EditorViewModel) {
    var octave by rememberSaveable { mutableIntStateOf(3) }

    TokenTools(chainId, vm) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = { if (octave > 1) octave-- },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
            ) { Text("−") }
            Text(
                "oct $octave",
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 10.dp),
            )
            OutlinedButton(
                onClick = { if (octave < 7) octave++ },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
            ) { Text("+") }
        }
    }
    Spacer(Modifier.height(8.dp))

    val whiteW = 44.dp
    val blackW = 28.dp
    val keyH = 150.dp
    val octaves = listOf(octave, octave + 1)

    Box(
        Modifier
            .fillMaxWidth()
            .height(keyH)
            .horizontalScroll(rememberScrollState()),
    ) {
        Row {
            octaves.forEach { o ->
                WhiteKeys.forEach { name ->
                    val token = "$name$o"
                    Box(
                        Modifier
                            .width(whiteW)
                            .height(keyH)
                            .padding(1.dp)
                            .clip(RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
                            .background(Color(0xFFF4F4F8))
                            .clickable { vm.appendToken(chainId, token); vm.previewToken(chainId, token) },
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Text(token, color = Ink, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(bottom = 6.dp))
                    }
                }
            }
        }
        octaves.forEachIndexed { oi, o ->
            BlackKeys.forEach { (name, after) ->
                val token = "$name$o"
                val x = whiteW * (oi * 7 + after + 1) - blackW / 2
                Box(
                    Modifier
                        .offset(x = x)
                        .width(blackW)
                        .height(keyH * 0.6f)
                        .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                        .background(Color(0xFF15172B))
                        .border(1.dp, Color(0xFF3A3F6B), RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                        .clickable { vm.appendToken(chainId, token); vm.previewToken(chainId, token) },
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Text(name, color = Color(0xFFB8BBD3), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(bottom = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun DegreePad(chainId: String, vm: EditorViewModel) {
    TokenTools(chainId, vm) {
        Text("scale degrees / sample index", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Spacer(Modifier.height(8.dp))
    val rows = listOf((0..5).toList(), (6..11).toList(), listOf(-1, -2, -3, 12, 14, 16))
    rows.forEach { row ->
        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            row.forEach { n ->
                val token = n.toString()
                Box(
                    Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        .clickable { vm.appendToken(chainId, token); vm.previewToken(chainId, token) },
                    contentAlignment = Alignment.Center,
                ) { Text(token, fontFamily = FontFamily.Monospace, color = Amber) }
            }
        }
    }
}

/** Root × quality picker for `chord` chains. */
@Composable
private fun ChordPad(chainId: String, hasVoicing: Boolean, vm: EditorViewModel) {
    var rootNote by rememberSaveable { mutableStateOf("C") }
    var alternate by rememberSaveable { mutableStateOf(true) }

    TokenTools(chainId, vm) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilterChip(
                selected = alternate,
                onClick = { alternate = !alternate },
                label = { Text(if (alternate) "<one per cycle>" else "split the cycle", style = MaterialTheme.typography.labelSmall) },
            )
        }
    }
    if (!hasVoicing) {
        Text(
            "No .voicing() block on this chain — chords will be silent. Add one with + Add block.",
            color = Coral,
            style = MaterialTheme.typography.labelSmall,
        )
    }
    Spacer(Modifier.height(6.dp))
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Vocabulary.chordRoots.forEach { r ->
            FilterChip(selected = rootNote == r, onClick = { rootNote = r }, label = { Text(r, fontFamily = FontFamily.Monospace) })
        }
    }
    Spacer(Modifier.height(6.dp))
    Vocabulary.chordQualities.chunked(5).forEach { row ->
        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            row.forEach { (suffix, label) ->
                val token = rootNote + suffix
                Column(
                    Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        .clickable { vm.appendChord(chainId, token, alternate); vm.previewToken(chainId, token) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(token, fontFamily = FontFamily.Monospace, color = Amber, style = MaterialTheme.typography.bodyMedium)
                    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            // keep the grid aligned on the last, shorter row
            repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}
