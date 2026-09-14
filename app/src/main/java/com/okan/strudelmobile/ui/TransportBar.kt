package com.okan.strudelmobile.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun TransportBar(
    ready: Boolean,
    playing: Boolean,
    cpm: Double,
    onTogglePlay: () -> Unit,
    onHush: () -> Unit,
    onCpm: (Double) -> Unit,
    onReset: () -> Unit,
) {
    var dragCpm by remember(cpm) { mutableFloatStateOf(cpm.toFloat()) }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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

        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "${(dragCpm * 4).roundToInt()} bpm · ${dragCpm.roundToInt()} cpm",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Slider(
                value = dragCpm,
                onValueChange = { dragCpm = it },
                onValueChangeFinished = { onCpm(dragCpm.roundToInt().toDouble()) },
                valueRange = 10f..60f,
            )
        }
    }
}
