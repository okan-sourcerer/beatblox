package com.okan.strudelmobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.okan.strudelmobile.feedback.FeedbackKind
import com.okan.strudelmobile.feedback.SendResult
import kotlinx.coroutines.launch

/**
 * Bug / idea / anything form. Sends through [EditorViewModel.sendFeedback];
 * while no server is configured the report is kept in the on-device outbox
 * and the sheet says so honestly instead of pretending it went somewhere.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackSheet(vm: EditorViewModel, onDismiss: () -> Unit) {
    var kind by rememberSaveable { mutableStateOf(FeedbackKind.BUG) }
    var message by rememberSaveable { mutableStateOf("") }
    var contact by rememberSaveable { mutableStateOf("") }
    var attachPattern by rememberSaveable { mutableStateOf(true) }
    var sending by rememberSaveable { mutableStateOf(false) }
    var result by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            Text("Feedback", style = MaterialTheme.typography.titleLarge)
            Text(
                "Found a bug, a sound that's wrong, a block you're missing? Tell me — it goes straight to the developer.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))

            if (result != null) {
                Text(result!!, style = MaterialTheme.typography.bodyMedium, color = Mint)
                Spacer(Modifier.height(12.dp))
                Button(onClick = onDismiss) { Text("Done") }
                return@Column
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FeedbackKind.entries.forEach { k ->
                    FilterChip(selected = kind == k, onClick = { kind = k }, label = { Text(k.label) })
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text(if (kind == FeedbackKind.BUG) "What happened, and what did you expect?" else "What's on your mind?") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = contact,
                onValueChange = { contact = it },
                label = { Text("Email or handle (optional, if you want a reply)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = attachPattern, onCheckedChange = { attachPattern = it })
                Text("Attach the current pattern's code", style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                "Also sent: app version, phone model and Android version. Nothing else.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDismiss, enabled = !sending) { Text("Cancel") }
                Spacer(Modifier.size(8.dp))
                Button(
                    enabled = message.isNotBlank() && !sending,
                    onClick = {
                        sending = true
                        scope.launch {
                            val r = vm.sendFeedback(kind, message.trim(), contact.trim().ifBlank { null }, attachPattern)
                            sending = false
                            result = when (r) {
                                SendResult.Sent -> "Sent. Thank you!"
                                SendResult.Queued ->
                                    "Saved on this phone. The feedback server isn't live yet, so it will be " +
                                        "delivered by a future update — thank you for writing it anyway."
                            }
                        }
                    },
                ) {
                    if (sending) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Send")
                }
            }
        }
    }
}
