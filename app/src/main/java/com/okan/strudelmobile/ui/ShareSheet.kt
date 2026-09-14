package com.okan.strudelmobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Export the current pattern: as code (clipboard / share) or as a strudel.cc link. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareSheet(vm: EditorViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val code by vm.code.collectAsStateWithLifecycle()
    val cpm by vm.cpm.collectAsStateWithLifecycle()
    val name by vm.currentName.collectAsStateWithLifecycle()
    val exported = Share.exportCode(code, cpm, name)
    val url = Share.strudelUrl(exported)
    val subject = "Strudel pattern" + (name?.let { ": $it" } ?: "")

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Text("Share pattern", style = MaterialTheme.typography.titleLarge)
            Text(
                "The code below runs as-is on strudel.cc, and the link carries the whole pattern.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                exported.trimEnd(),
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall,
                color = Mint,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 160.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(InkRaised)
                    .verticalScroll(rememberScrollState())
                    .padding(10.dp),
            )
            Spacer(Modifier.height(8.dp))

            Action(Icons.Default.ContentCopy, "Copy code", "Paste into strudel.cc or a text file") {
                Share.copy(context, exported); onDismiss()
            }
            Action(Icons.Default.Share, "Share code…", "Send the code as text to another app") {
                Share.shareText(context, exported, subject); onDismiss()
            }
            Action(Icons.Default.Link, "Copy strudel.cc link", "Anyone opening it hears the pattern in a browser") {
                Share.copy(context, url, "Link"); onDismiss()
            }
            Action(Icons.AutoMirrored.Filled.OpenInNew, "Open in strudel.cc", "Continue editing on the full desktop REPL") {
                Share.open(context, url); onDismiss()
            }
        }
    }
}

@Composable
private fun Action(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall) },
        leadingContent = { Icon(icon, null, tint = Amber) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.clickable(onClick = onClick),
    )
}
