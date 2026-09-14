package com.okan.strudelmobile.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun TransportBar(
    ready: Boolean,
    playing: Boolean,
    cpm: Double,
    patternName: String?,
    onLibrary: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onTogglePlay: () -> Unit,
    onHush: () -> Unit,
    onCpm: (Double) -> Unit,
    onReset: () -> Unit,
) {
    var dragCpm by remember(cpm) { mutableFloatStateOf(cpm.toFloat()) }

    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (!ready) {
                CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
                Spacer(Modifier.width(12.dp))
                Text("Loading Strudel…", style = MaterialTheme.typography.bodyMedium)
                return@Row
            }
            FilledIconButton(
                onClick = onTogglePlay,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (playing) Coral else Mint,
                    contentColor = Ink,
                ),
                modifier = Modifier.size(48.dp),
            ) {
                Icon(if (playing) Icons.Default.Stop else Icons.Default.PlayArrow, if (playing) "Stop" else "Play")
            }
            IconButton(onClick = onHush) { Icon(Icons.Default.VolumeOff, "Hush") }
            IconButton(onClick = onReset) { Icon(Icons.Default.RestartAlt, "Reset pattern") }
            IconButton(onClick = onLibrary) { Icon(Icons.Default.LibraryMusic, "Patterns") }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onUndo, enabled = canUndo) { Icon(Icons.AutoMirrored.Filled.Undo, "Undo") }
            IconButton(onClick = onRedo, enabled = canRedo) { Icon(Icons.AutoMirrored.Filled.Redo, "Redo") }
        }
        if (ready) {
            Row(Modifier.fillMaxWidth().height(36.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${(dragCpm * 4).roundToInt()} bpm",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(72.dp),
                )
                Text(
                    patternName ?: "unsaved",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (patternName != null) Amber else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.width(96.dp).clickable(onClick = onLibrary),
                )
                Slider(
                    value = dragCpm,
                    onValueChange = { dragCpm = it },
                    onValueChangeFinished = { onCpm(dragCpm.roundToInt().toDouble()) },
                    valueRange = 10f..60f,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
