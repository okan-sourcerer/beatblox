package com.okan.beatblox.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.okan.beatblox.model.Arg
import com.okan.beatblox.model.MiniSource
import com.okan.beatblox.model.SoundCategory
import com.okan.beatblox.model.SoundDescriptions

/**
 * The sample/sound browser: everything superdough registered (drum machines,
 * piano, synth waveforms, ...). Tapping a name inserts it into the selected
 * `s` block, or sets/adds an `.s()` transform on a note/n chain.
 */
@Composable
fun SoundBrowser(vm: EditorViewModel) {
    val sounds by vm.engine.sounds.collectAsStateWithLifecycle()
    val root by vm.root.collectAsStateWithLifecycle()
    val selection by vm.selection.collectAsStateWithLifecycle()
    val target = remember(root, selection) { vm.targetChain() }
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf<String?>(null) } // SoundCategory name or null = all

    // `.bank("X")` makes Strudel look up "x_<sound>", so with a bank set we
    // list only that bank's sounds and insert the short names.
    val bank = target?.transforms?.firstOrNull { it.fn == "bank" }?.args?.firstOrNull()?.let { (it as? Arg.Str)?.value }
    val bankPrefix = bank?.lowercase()?.let { "${it}_" }
    val banks = remember(sounds) { bankNames(sounds) }

    // Describe once per sound list; the search also matches description text.
    val described = remember(sounds) { sounds.map { it to SoundDescriptions.describe(it.name, it.type) } }
    val filtered = remember(described, query, filter, bankPrefix) {
        described.filter { (s, d) ->
            (bankPrefix == null || s.name.startsWith(bankPrefix)) &&
                (filter == null || d.category.name == filter) &&
                (query.isBlank() || s.name.contains(query, ignoreCase = true) || d.text.contains(query, ignoreCase = true))
        }
    }
    fun shortName(name: String) = if (bankPrefix != null) name.removePrefix(bankPrefix) else name

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("search name or description", maxLines = 1) },
            )
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text("all") })
            SoundCategory.entries.forEach { c ->
                FilterChip(selected = filter == c.name, onClick = { filter = c.name }, label = { Text(c.label) })
            }
        }
        val targetLabel = when (val s = target?.source) {
            is MiniSource -> if (s.fn == "s") "→ appends to the s block" else "→ sets .s() on the ${s.fn} block"
            else -> "select a block first"
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                targetLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (target != null && (target.source as MiniSource).fn == "s") {
                BankChooser(current = bank, banks = banks) { vm.setBank(target.id, it) }
            }
        }
        if (bank != null) {
            Text(
                SoundDescriptions.describeBank(bank),
                style = MaterialTheme.typography.labelSmall,
                color = Amber,
                modifier = Modifier.padding(horizontal = 12.dp),
                maxLines = 2,
            )
        }
        HorizontalDivider()
        if (sounds.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading sound banks…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(filtered, key = { it.first.name }) { (s, d) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(enabled = target != null) {
                            val chain = target ?: return@clickable
                            val src = chain.source as MiniSource
                            if (src.fn == "s") {
                                vm.appendToken(chain.id, shortName(s.name))
                                vm.previewToken(chain.id, shortName(s.name))
                            } else {
                                vm.setSoundTransform(chain.id, s.name)
                                vm.previewChain(chain.id)
                            }
                        }
                        .padding(start = 16.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(shortName(s.name), fontFamily = FontFamily.Monospace)
                            Spacer(Modifier.size(8.dp))
                            Text(
                                (if (d.pitched) "pitched · " else "") + (if (s.type == "sample") "${s.count} smp" else s.type),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (d.pitched) Amber else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            // Under a bank filter the bank blurb is already shown above the list.
                            if (bankPrefix != null) d.text.substringBefore(" · ") else d.text,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                        )
                    }
                    Spacer(Modifier.size(4.dp))
                    IconButton(onClick = { vm.previewSound(s.name) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.PlayArrow, "Preview", tint = Mint)
                    }
                }
            }
        }
    }
}
