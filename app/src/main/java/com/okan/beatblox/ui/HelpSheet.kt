package com.okan.beatblox.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.okan.beatblox.model.SoundDescriptions
import com.okan.beatblox.model.Vocabulary

/** A short, practical explainer for people who have never used Strudel. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            Text("How this works", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Body(
                "A pattern is a chain of blocks. The top block makes events (sounds or notes), " +
                    "every block below it changes them, in order. Everything repeats once per cycle " +
                    "(at 120 bpm a cycle is one 4/4 bar).",
            )

            Section("Top blocks")
            Vocabulary.sourceDescriptions.forEach { (fn, text) -> Entry(fn, text) }
            Vocabulary.groupDescriptions.forEach { (fn, text) -> Entry(fn, text) }

            Section("Writing the pattern text (mini-notation)")
            Body("Space-separated steps share one cycle equally: \"bd sd hh sd\" is four even hits.")
            Entry("~", "rest (silence) for that step")
            Entry("[a b]", "sub-steps: squeeze several events into one step")
            Entry("a*2", "repeat: play that step twice as fast")
            Entry("a!2", "duplicate: the step twice in a row")
            Entry("<a b>", "alternate: a on cycle 1, b on cycle 2, …")
            Entry("a(3,8)", "euclidean rhythm: 3 hits spread over 8 steps")
            Entry("a?", "maybe: 50% chance the step plays")
            Entry("a:2", "for sounds: sample number 2 of that sound")
            Entry("a@3", "weight: make that step 3 times as long")
            Entry("c3 eb3", "notes: letter + optional # or b + octave (c4 = middle C)")

            Section("Sounds and banks")
            Body(
                "s(\"bd sd\") plays samples by name. Drum machines are packaged as banks: with a " +
                    ".bank(\"RolandTR909\") block, \"bd\" means the 909's kick. The Sounds tab shows " +
                    "what each name is, and filters to the bank you picked.",
            )
            SoundDescriptions.drumAbbrev.entries.take(14).forEach { (k, v) -> Entry(k, v) }

            Section("Chords")
            Body(
                "A chord top block holds symbols like C^7 (maj7), Am7, G7, Dm7b5 (h7), Bbaug; a .voicing() block " +
                    "turns each into notes with smooth voice-leading. Put .s(\"piano\") after it. On an n chain, " +
                    "n(\"0 1 2 3\").chord(\"<C^7 Am7>\").voicing() arpeggiates through the chord tones.",
            )

            Section("Tips")
            Body("▶ on a block auditions just that chain, even while the transport is stopped.")
            Body("Piano keys and sound names insert into the selected top block and audition through its chain.")
            Body("Blocks glow while they sound; the exact step lights up in the text.")
            Body("Drag ⋮⋮ to reorder blocks or layers. Order matters: .fast(2) before .rev() is not the same as after.")
        }
    }
}

@Composable
private fun Section(title: String) {
    Spacer(Modifier.height(16.dp))
    Text(title, style = MaterialTheme.typography.titleMedium, color = Amber)
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun Body(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 2.dp))
}

@Composable
private fun Entry(key: String, text: String) {
    Row(Modifier.padding(vertical = 2.dp)) {
        Text(key, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, color = Mint, modifier = Modifier.width(72.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
    }
}
