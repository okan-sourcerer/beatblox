package com.okan.beatblox.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** The generated Strudel code plus the engine log — read-only, for transparency and debugging. */
@Composable
fun CodePanel(vm: EditorViewModel) {
    val code by vm.code.collectAsStateWithLifecycle()
    val log by vm.engine.log.collectAsStateWithLifecycle()
    val cpm by vm.cpm.collectAsStateWithLifecycle()
    val name by vm.currentName.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().background(InkRaised)) {
            Text(
                code,
                fontFamily = FontFamily.Monospace,
                color = Mint,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .padding(end = 72.dp),
            )
            Row(Modifier.align(Alignment.TopEnd)) {
                IconButton(onClick = { Share.copy(context, Share.exportCode(code, cpm, name)) }) {
                    Icon(Icons.Default.ContentCopy, "Copy code", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { Share.shareText(context, Share.exportCode(code, cpm, name), "Strudel pattern") }) {
                    Icon(Icons.Default.Share, "Share code", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        HorizontalDivider()
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            log.asReversed().forEach { line ->
                Text(
                    line,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (line.startsWith("error")) Coral else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
