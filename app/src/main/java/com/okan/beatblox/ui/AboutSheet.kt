package com.okan.beatblox.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.okan.beatblox.BuildConfig

const val STRUDEL_VERSION = "1.3.0" // keep in sync with web/package.json (npm run sync prints it)

/** Version, who made what, and the licenses this app stands on. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSheet(onDismiss: () -> Unit, onFeedback: () -> Unit) {
    val context = LocalContext.current

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            Text("BeatBlox", style = MaterialTheme.typography.titleLarge)
            Text("powered by Strudel", style = MaterialTheme.typography.labelMedium, color = Amber)
            Text(
                "version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.labelMedium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Body(
                "A block-based way to make music with Strudel on a phone: instead of typing pattern " +
                    "code you stack blocks, and the app writes the code and plays it.",
            )
            Body("Made by Okan Tanrıverdi (okan-sourcerer). Not affiliated with the Strudel or TidalCycles projects.")

            if (BuildConfig.DOWNLOAD_URL.isNotBlank()) {
                LinkRow("Latest version & updates", BuildConfig.DOWNLOAD_URL) { Share.open(context, it) }
            }
            if (BuildConfig.SOURCE_URL.isNotBlank()) {
                LinkRow("Source code", BuildConfig.SOURCE_URL) { Share.open(context, it) }
            }
            LinkRow("Send feedback", "bug reports and ideas welcome") { onFeedback() }

            Heading("Built on")
            Credit(
                "Strudel $STRUDEL_VERSION",
                "The live-coding engine, pattern language and audio (superdough) that actually make the sound. " +
                    "By Felix Roos, Alex McLean and contributors. AGPL-3.0.",
                "https://strudel.cc",
            ) { Share.open(context, it) }
            Credit(
                "TidalCycles",
                "The pattern ideas Strudel ports — cycles, mini-notation, the function vocabulary. By Alex McLean and the Tidal community.",
                "https://tidalcycles.org",
            ) { Share.open(context, it) }
            Credit(
                "Sample banks",
                "Drum machines, pianos and the rest are streamed from the community sample collections " +
                    "Strudel uses (dough-samples, tidal-drum-machines and others). Each bank carries its own license.",
                "https://github.com/felixroos/dough-samples",
            ) { Share.open(context, it) }
            Credit(
                "Android, Jetpack Compose, Kotlin",
                "The native shell. Apache-2.0.",
                null,
            ) {}

            Heading("License")
            Body(
                "BeatBlox is free software under the GNU Affero General Public License v3, " +
                    "the same license as Strudel itself. You can use, study, share and change it; if you " +
                    "distribute a changed version you must offer its source under the same terms. " +
                    "The full text is in the app's source repository (LICENSE).",
            )
            Body("No account, no analytics, no tracking. The only network traffic is fetching sample banks, and feedback when you send it.")
        }
    }
}

@Composable
private fun Heading(text: String) {
    Spacer(Modifier.height(16.dp))
    Text(text, style = MaterialTheme.typography.titleMedium, color = Amber)
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun Body(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 2.dp))
}

@Composable
private fun LinkRow(title: String, target: String, onClick: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick(target) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Mint)
            Text(target, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Credit(name: String, text: String, url: String?, onOpen: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .then(if (url != null) Modifier.clickable { onOpen(url) } else Modifier)
            .padding(vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(name, fontWeight = FontWeight.SemiBold, color = Mint, modifier = Modifier.weight(1f))
            if (url != null) Icon(Icons.AutoMirrored.Filled.OpenInNew, null, Modifier.height(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(text, style = MaterialTheme.typography.bodySmall)
        if (url != null) Text(url, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
