package com.okan.beatblox.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.okan.beatblox.model.SavedPattern
import com.okan.beatblox.model.Serializer
import java.text.DateFormat
import java.util.Date

/** The saved-patterns library: load, save, save as, rename, delete, new. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibrarySheet(vm: EditorViewModel, onDismiss: () -> Unit) {
    val library by vm.library.collectAsStateWithLifecycle()
    val currentName by vm.currentName.collectAsStateWithLifecycle()
    var nameDialog by remember { mutableStateOf<NameDialog?>(null) }
    var confirmDelete by remember { mutableStateOf<SavedPattern?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Text("Patterns", style = MaterialTheme.typography.titleLarge)
            Text(
                if (currentName != null) "editing: $currentName" else "editing: unsaved scratch pattern",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    if (currentName != null) { vm.saveCurrent(); onDismiss() } else nameDialog = NameDialog.SaveAs
                }) {
                    Icon(Icons.Default.Save, null, Modifier.size(18.dp))
                    Text(if (currentName != null) " Save" else " Save as…")
                }
                if (currentName != null) {
                    OutlinedButton(onClick = { nameDialog = NameDialog.SaveAs }) { Text("Save as…") }
                }
                OutlinedButton(onClick = { vm.newPattern(); onDismiss() }) {
                    Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                    Text(" New")
                }
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            if (library.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("Nothing saved yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(library.sortedByDescending { it.updatedAt }, key = { it.id }) { p ->
                        PatternRow(
                            pattern = p,
                            isCurrent = p.name == currentName,
                            onLoad = { vm.loadPattern(p.id); onDismiss() },
                            onRename = { nameDialog = NameDialog.Rename(p) },
                            onDelete = { confirmDelete = p },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    nameDialog?.let { dialog ->
        NamePrompt(
            title = if (dialog is NameDialog.Rename) "Rename pattern" else "Save pattern as",
            initial = (dialog as? NameDialog.Rename)?.pattern?.name ?: (currentName ?: ""),
            onDismiss = { nameDialog = null },
            onConfirm = { name ->
                when (dialog) {
                    NameDialog.SaveAs -> { vm.saveAs(name); onDismiss() }
                    is NameDialog.Rename -> vm.renamePattern(dialog.pattern.id, name)
                }
                nameDialog = null
            },
        )
    }

    confirmDelete?.let { p ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete \"${p.name}\"?") },
            text = { Text("This removes it from the library. The pattern on screen is not affected.") },
            confirmButton = { TextButton(onClick = { vm.deletePattern(p.id); confirmDelete = null }) { Text("Delete", color = Coral) } },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancel") } },
        )
    }
}

private sealed interface NameDialog {
    data object SaveAs : NameDialog
    data class Rename(val pattern: SavedPattern) : NameDialog
}

@Composable
private fun PatternRow(
    pattern: SavedPattern,
    isCurrent: Boolean,
    onLoad: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onLoad)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                pattern.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                color = if (isCurrent) Amber else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                Serializer.serialize(pattern.root).code,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Text(
                "${(pattern.cpm * 4).toInt()} bpm · ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(pattern.updatedAt))}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(4.dp))
        IconButton(onClick = onRename, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Edit, "Rename") }
        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Delete, "Delete", tint = Coral) }
    }
}

@Composable
private fun NamePrompt(title: String, initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, label = { Text("name") })
        },
        confirmButton = { TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
