package com.okan.strudelmobile.model

/**
 * The block vocabulary: which Strudel functions the UI knows how to build,
 * and how their parameters are edited. Deliberately small; extend as needed.
 */
sealed interface ParamSpec {
    val name: String

    data class Number(
        override val name: String,
        val min: Double,
        val max: Double,
        val step: Double,
        val default: Double,
    ) : ParamSpec

    data class Text(override val name: String, val default: String, val isMini: Boolean = true) : ParamSpec

    data class Choice(override val name: String, val options: List<String>, val default: String) : ParamSpec

    /** A function argument (`x => x...`), pre-filled with [defaultInner] blocks. */
    data class Fn(override val name: String, val defaultInner: () -> List<Transform> = { emptyList() }) : ParamSpec
}

data class FunctionSpec(
    val name: String,
    val label: String,
    val category: Category,
    val params: List<ParamSpec> = emptyList(),
    /** One plain-language sentence for people who don't know Strudel. */
    val description: String = "",
    /** `layer(fn, fn, ...)`: any number of function args may be added/removed. */
    val variadicFn: Boolean = false,
) {
    enum class Category { TIME, SOUND, PITCH, FILTER, SPACE, DYNAMICS, STRUCTURE }

    fun defaultArgs(): List<Arg> = params.map {
        when (it) {
            is ParamSpec.Number -> Arg.Num(it.default)
            is ParamSpec.Text -> Arg.Str(it.default)
            is ParamSpec.Choice -> Arg.Str(it.default)
            is ParamSpec.Fn -> Arg.Fn(it.defaultInner())
        }
    }
}

object Vocabulary {
    /** Sensible starting contents for function arguments — something audible. */
    private fun inner(fn: String, value: Double): () -> List<Transform> = { listOf(Transform(fn = fn, args = listOf(Arg.Num(value)))) }
    private fun innerRev(): () -> List<Transform> = { listOf(Transform(fn = "rev")) }

    /** Functions that can start a chain and take a mini-notation string. */
    val sourceFns = listOf("s", "note", "n", "chord")

    val sourceDescriptions = mapOf(
        "s" to "sound: play samples by name — \"bd sd hh\". Unpitched; pick names in the Sounds tab.",
        "note" to "note: play pitches — \"c3 e3 g3\" or MIDI numbers. Add an .s() block to choose the instrument.",
        "n" to "n: numbers — sample index inside a sound, or scale degree when a .scale() block is present.",
        "chord" to "chord: chord symbols — \"<C^7 Am7 Dm7 G7>\". Needs a .voicing() block to turn them into notes.",
    )

    /** Chord qualities the voicing dictionary (ireal) understands, with labels for the picker. */
    val chordQualities: List<Pair<String, String>> = listOf(
        "" to "major", "m" to "minor", "7" to "dom 7", "^7" to "maj 7", "m7" to "min 7",
        "6" to "6", "m6" to "min 6", "9" to "9", "^9" to "maj 9", "m9" to "min 9",
        "add9" to "add 9", "69" to "6/9", "11" to "11", "m11" to "min 11", "13" to "13",
        "sus" to "sus 4", "7sus" to "7 sus", "o" to "dim", "o7" to "dim 7", "h7" to "half-dim",
        "aug" to "aug", "7b9" to "7 b9", "7#9" to "7 #9", "7#11" to "7 #11", "m^7" to "min/maj 7",
    )
    val chordRoots = listOf("C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B")

    /** Functions that combine child chains. */
    val groupFns = listOf("stack", "seq", "cat")

    val groupDescriptions = mapOf(
        "stack" to "stack: all layers play at the same time.",
        "seq" to "seq: layers play one after another, squeezed into one cycle.",
        "cat" to "cat: layers take turns, one full cycle each.",
    )

    val scales = listOf(
        "C:major", "C:minor", "C:pentatonic", "C:minor:pentatonic", "C:dorian", "C:mixolydian",
        "C:lydian", "C:phrygian", "C:blues", "D:major", "D:minor", "E:minor", "F:major",
        "G:major", "G:minor", "A:minor", "A:major", "Bb:major", "Eb:major",
    )

    val transforms: List<FunctionSpec> = listOf(
        FunctionSpec("fast", "fast", FunctionSpec.Category.TIME, listOf(ParamSpec.Number("factor", 0.25, 8.0, 0.25, 2.0)), description = "Speeds the pattern up: fast(2) squeezes it into half a cycle so it plays twice per cycle."),
        FunctionSpec("slow", "slow", FunctionSpec.Category.TIME, listOf(ParamSpec.Number("factor", 0.25, 8.0, 0.25, 2.0)), description = "Slows the pattern down: slow(2) stretches it over two cycles."),
        FunctionSpec("rev", "reverse", FunctionSpec.Category.TIME, description = "Plays each cycle backwards."),
        FunctionSpec("late", "late", FunctionSpec.Category.TIME, listOf(ParamSpec.Number("cycles", 0.0, 1.0, 0.125, 0.25)), description = "Shifts the pattern later in time by a fraction of a cycle (0.25 = a quarter cycle)."),
        FunctionSpec("struct", "struct", FunctionSpec.Category.STRUCTURE, listOf(ParamSpec.Text("pattern", "x ~ x x")), description = "Imposes a rhythm: \"x ~ x x\" plays the sound on the x steps and rests on ~."),
        FunctionSpec("degradeBy", "degrade", FunctionSpec.Category.STRUCTURE, listOf(ParamSpec.Number("amount", 0.0, 1.0, 0.05, 0.5)), description = "Randomly drops events. 0.5 removes about half of them each cycle."),
        FunctionSpec("ply", "ply", FunctionSpec.Category.STRUCTURE, listOf(ParamSpec.Number("times", 1.0, 8.0, 1.0, 2.0)), description = "Repeats every event N times in place, like a fast roll."),

        FunctionSpec("s", "sound", FunctionSpec.Category.SOUND, listOf(ParamSpec.Text("sound", "piano")), description = "Which sound/sample plays the notes. Pick from the Sounds tab."),
        FunctionSpec("bank", "bank", FunctionSpec.Category.SOUND, listOf(ParamSpec.Text("bank", "RolandTR909", isMini = false)), description = "Drum machine to take bd/sd/hh… from, e.g. RolandTR909. Sounds are looked up as bank_sound."),
        FunctionSpec("n", "sample #", FunctionSpec.Category.SOUND, listOf(ParamSpec.Text("index", "0")), description = "Which sample number inside a sound (0 = first). With a scale, it is the scale degree instead."),
        FunctionSpec("speed", "speed", FunctionSpec.Category.SOUND, listOf(ParamSpec.Number("speed", -2.0, 2.0, 0.05, 1.0)), description = "Playback speed of samples: 2 = double speed & pitch, 0.5 = half, negative = reversed."),
        FunctionSpec("crush", "bitcrush", FunctionSpec.Category.SOUND, listOf(ParamSpec.Number("bits", 1.0, 16.0, 1.0, 8.0)), description = "Bitcrusher: fewer bits = more digital grit. 16 is clean, 4 is destroyed."),

        FunctionSpec("note", "note", FunctionSpec.Category.PITCH, listOf(ParamSpec.Text("notes", "c3")), description = "Sets the pitch as note names (c3, eb4…) or MIDI numbers."),
        FunctionSpec("scale", "scale", FunctionSpec.Category.PITCH, listOf(ParamSpec.Choice("scale", scales, "C:major")), description = "Maps n() numbers onto a musical scale, so 0 1 2 3 are always in key."),
        FunctionSpec("transpose", "transpose", FunctionSpec.Category.PITCH, listOf(ParamSpec.Number("semitones", -24.0, 24.0, 1.0, 12.0)), description = "Shifts pitch by semitones: 12 = one octave up, -12 = one octave down."),

        FunctionSpec("lpf", "low-pass", FunctionSpec.Category.FILTER, listOf(ParamSpec.Number("cutoff", 50.0, 8000.0, 50.0, 800.0)), description = "Low-pass filter: cuts highs above the cutoff. Low values sound muffled and dark."),
        FunctionSpec("hpf", "high-pass", FunctionSpec.Category.FILTER, listOf(ParamSpec.Number("cutoff", 20.0, 5000.0, 20.0, 200.0)), description = "High-pass filter: cuts lows below the cutoff. Thins the sound out."),
        FunctionSpec("vowel", "vowel", FunctionSpec.Category.FILTER, listOf(ParamSpec.Choice("vowel", listOf("a", "e", "i", "o", "u"), "a")), description = "Formant filter that makes the sound \"say\" a vowel."),

        FunctionSpec("room", "reverb", FunctionSpec.Category.SPACE, listOf(ParamSpec.Number("amount", 0.0, 1.0, 0.05, 0.5)), description = "Reverb amount (0 = dry, 1 = drenched). Pair with size."),
        FunctionSpec("size", "reverb size", FunctionSpec.Category.SPACE, listOf(ParamSpec.Number("size", 0.0, 1.0, 0.05, 0.5)), description = "Reverb room size: bigger = longer tail."),
        FunctionSpec("delay", "delay", FunctionSpec.Category.SPACE, listOf(ParamSpec.Number("amount", 0.0, 1.0, 0.05, 0.5)), description = "Echo amount (0 = none). Pair with delaytime / delayfeedback."),
        FunctionSpec("delaytime", "delay time", FunctionSpec.Category.SPACE, listOf(ParamSpec.Number("time", 0.0, 1.0, 0.0625, 0.25)), description = "Echo spacing in cycles (0.25 = a quarter of a cycle)."),
        FunctionSpec("delayfeedback", "delay feedback", FunctionSpec.Category.SPACE, listOf(ParamSpec.Number("feedback", 0.0, 0.95, 0.05, 0.5)), description = "How much of each echo feeds back: higher = more repeats."),
        FunctionSpec("pan", "pan", FunctionSpec.Category.SPACE, listOf(ParamSpec.Number("pan", 0.0, 1.0, 0.05, 0.5)), description = "Stereo position: 0 = left, 0.5 = centre, 1 = right."),

        FunctionSpec("gain", "gain", FunctionSpec.Category.DYNAMICS, listOf(ParamSpec.Number("gain", 0.0, 2.0, 0.05, 1.0)), description = "Volume: 1 = normal, 0 = silent, above 1 boosts."),
        FunctionSpec("attack", "attack", FunctionSpec.Category.DYNAMICS, listOf(ParamSpec.Number("seconds", 0.0, 2.0, 0.01, 0.01)), description = "Fade-in time in seconds; longer = softer start."),
        FunctionSpec("release", "release", FunctionSpec.Category.DYNAMICS, listOf(ParamSpec.Number("seconds", 0.0, 2.0, 0.01, 0.1)), description = "Fade-out time in seconds after the note ends."),
        FunctionSpec("decay", "decay", FunctionSpec.Category.DYNAMICS, listOf(ParamSpec.Number("seconds", 0.0, 2.0, 0.01, 0.2)), description = "How long the note takes to fall from the attack peak to the sustain level."),
        FunctionSpec("sustain", "sustain", FunctionSpec.Category.DYNAMICS, listOf(ParamSpec.Number("level", 0.0, 1.0, 0.05, 0.5)), description = "Level held after the decay until the note ends (0 = plucky, 1 = organ-like)."),
        FunctionSpec("velocity", "velocity", FunctionSpec.Category.DYNAMICS, listOf(ParamSpec.Number("velocity", 0.0, 1.0, 0.05, 0.8)), description = "Per-note loudness, like how hard a key is hit."),
        FunctionSpec("postgain", "post gain", FunctionSpec.Category.DYNAMICS, listOf(ParamSpec.Number("gain", 0.0, 2.0, 0.05, 1.0)), description = "Volume applied after the effects (gain is before them)."),

        // --- more time ---
        FunctionSpec("early", "early", FunctionSpec.Category.TIME, listOf(ParamSpec.Number("cycles", 0.0, 1.0, 0.125, 0.25)), description = "Shifts the pattern earlier in time by a fraction of a cycle."),
        FunctionSpec("hurry", "hurry", FunctionSpec.Category.TIME, listOf(ParamSpec.Number("factor", 0.25, 8.0, 0.25, 2.0)), description = "Like fast, but also speeds up the samples' pitch — tape-style."),
        FunctionSpec("swingBy", "swing", FunctionSpec.Category.TIME, listOf(ParamSpec.Number("amount", 0.0, 1.0, 0.05, 0.1), ParamSpec.Number("subdivision", 2.0, 16.0, 1.0, 4.0)), description = "Delays every other subdivision for a shuffled groove. amount ≈ 0.1–0.3, subdivision 4 = swing 8ths."),
        FunctionSpec("iter", "iter", FunctionSpec.Category.TIME, listOf(ParamSpec.Number("n", 2.0, 8.0, 1.0, 4.0)), description = "Each cycle starts one step later, rotating the pattern through n positions."),
        FunctionSpec("palindrome", "palindrome", FunctionSpec.Category.TIME, description = "Alternates forwards and backwards every cycle."),
        FunctionSpec("euclid", "euclid", FunctionSpec.Category.TIME, listOf(ParamSpec.Number("pulses", 1.0, 16.0, 1.0, 3.0), ParamSpec.Number("steps", 2.0, 16.0, 1.0, 8.0)), description = "Euclidean rhythm: spreads N hits as evenly as possible over M steps (3,8 = tresillo)."),
        FunctionSpec("segment", "segment", FunctionSpec.Category.TIME, listOf(ParamSpec.Number("n", 1.0, 32.0, 1.0, 8.0)), description = "Samples the pattern n times per cycle — turns continuous changes into steps."),
        FunctionSpec("linger", "linger", FunctionSpec.Category.TIME, listOf(ParamSpec.Number("fraction", 0.0625, 1.0, 0.0625, 0.25)), description = "Repeats only the first fraction of each cycle — a stutter / loop-the-start effect."),
        FunctionSpec("chop", "chop", FunctionSpec.Category.TIME, listOf(ParamSpec.Number("n", 1.0, 32.0, 1.0, 4.0)), description = "Cuts each sample into n pieces played in order — granular stutter, great on long samples."),
        FunctionSpec("striate", "striate", FunctionSpec.Category.TIME, listOf(ParamSpec.Number("n", 1.0, 32.0, 1.0, 4.0)), description = "Like chop, but interleaves the pieces of all events across the cycle."),
        FunctionSpec("loopAt", "loop at", FunctionSpec.Category.TIME, listOf(ParamSpec.Number("cycles", 1.0, 8.0, 1.0, 2.0)), description = "Time-stretches a sample (a break loop) to fit exactly N cycles."),

        // --- more structure (function arguments) ---
        FunctionSpec("every", "every", FunctionSpec.Category.STRUCTURE, listOf(ParamSpec.Number("n", 2.0, 16.0, 1.0, 4.0), ParamSpec.Fn("do", inner("fast", 2.0))), description = "Every n-th cycle, apply the nested blocks — e.g. every 4 cycles play twice as fast."),
        FunctionSpec("sometimesBy", "sometimes by", FunctionSpec.Category.STRUCTURE, listOf(ParamSpec.Number("probability", 0.0, 1.0, 0.05, 0.5), ParamSpec.Fn("do", inner("speed", 2.0))), description = "Applies the nested blocks to a random share of events each cycle."),
        FunctionSpec("sometimes", "sometimes", FunctionSpec.Category.STRUCTURE, listOf(ParamSpec.Fn("do", inner("speed", 2.0))), description = "Applies the nested blocks to about half the events, at random."),
        FunctionSpec("rarely", "rarely", FunctionSpec.Category.STRUCTURE, listOf(ParamSpec.Fn("do", inner("speed", 2.0))), description = "Applies the nested blocks to about a quarter of the events."),
        FunctionSpec("often", "often", FunctionSpec.Category.STRUCTURE, listOf(ParamSpec.Fn("do", inner("speed", 2.0))), description = "Applies the nested blocks to about three quarters of the events."),
        FunctionSpec("off", "off", FunctionSpec.Category.STRUCTURE, listOf(ParamSpec.Number("offset", 0.0, 1.0, 0.0625, 0.125), ParamSpec.Fn("do", inner("add", 12.0))), description = "Adds a delayed copy of the pattern with the nested blocks applied — e.g. an echo an octave up."),
        FunctionSpec("superimpose", "superimpose", FunctionSpec.Category.STRUCTURE, listOf(ParamSpec.Fn("do", inner("add", 12.0))), description = "Layers a modified copy on top of the original (like off with no delay)."),
        FunctionSpec("jux", "jux", FunctionSpec.Category.STRUCTURE, listOf(ParamSpec.Fn("do", innerRev())), description = "Stereo trick: the original on the left, the modified copy on the right."),
        FunctionSpec("chunk", "chunk", FunctionSpec.Category.STRUCTURE, listOf(ParamSpec.Number("n", 2.0, 8.0, 1.0, 4.0), ParamSpec.Fn("do", inner("hurry", 2.0))), description = "Divides the cycle into n chunks and applies the nested blocks to one chunk at a time, moving each cycle."),
        FunctionSpec("degrade", "degrade", FunctionSpec.Category.STRUCTURE, description = "Randomly drops about half of the events."),
        FunctionSpec("shuffle", "shuffle", FunctionSpec.Category.STRUCTURE, listOf(ParamSpec.Number("n", 2.0, 16.0, 1.0, 4.0)), description = "Cuts the cycle into n slices and plays them in a random order (each once)."),
        FunctionSpec("scramble", "scramble", FunctionSpec.Category.STRUCTURE, listOf(ParamSpec.Number("n", 2.0, 16.0, 1.0, 4.0)), description = "Cuts the cycle into n slices and picks n at random (slices may repeat)."),

        // --- more sound ---
        FunctionSpec("begin", "begin", FunctionSpec.Category.SOUND, listOf(ParamSpec.Number("position", 0.0, 1.0, 0.05, 0.0)), description = "Where playback starts inside the sample (0 = start, 0.5 = halfway)."),
        FunctionSpec("end", "end", FunctionSpec.Category.SOUND, listOf(ParamSpec.Number("position", 0.0, 1.0, 0.05, 1.0)), description = "Where playback stops inside the sample (1 = the end)."),
        FunctionSpec("cut", "cut group", FunctionSpec.Category.SOUND, listOf(ParamSpec.Number("group", 1.0, 8.0, 1.0, 1.0)), description = "Sounds in the same cut group stop each other — e.g. an open hi-hat choked by the closed one."),
        FunctionSpec("clip", "clip / legato", FunctionSpec.Category.SOUND, listOf(ParamSpec.Number("length", 0.05, 4.0, 0.05, 1.0)), description = "Note length relative to its step: 0.5 = staccato, 1 = full, 2 = overlapping."),
        FunctionSpec("coarse", "coarse", FunctionSpec.Category.SOUND, listOf(ParamSpec.Number("factor", 1.0, 32.0, 1.0, 4.0)), description = "Sample-rate reduction: higher = more lo-fi aliasing."),
        FunctionSpec("shape", "shape", FunctionSpec.Category.SOUND, listOf(ParamSpec.Number("amount", 0.0, 0.95, 0.05, 0.3)), description = "Waveshaping distortion — warms up or destroys, depending on amount."),
        FunctionSpec("distort", "distort", FunctionSpec.Category.SOUND, listOf(ParamSpec.Number("amount", 0.0, 4.0, 0.1, 1.0)), description = "Overdrive distortion; goes much harder than shape."),
        FunctionSpec("vib", "vibrato", FunctionSpec.Category.SOUND, listOf(ParamSpec.Number("hz", 0.0, 16.0, 0.5, 4.0)), description = "Vibrato speed in Hz. Pair with vibmod for depth."),
        FunctionSpec("vibmod", "vibrato depth", FunctionSpec.Category.SOUND, listOf(ParamSpec.Number("semitones", 0.0, 2.0, 0.05, 0.5)), description = "Vibrato depth in semitones."),
        FunctionSpec("fm", "fm", FunctionSpec.Category.SOUND, listOf(ParamSpec.Number("index", 0.0, 16.0, 0.5, 2.0)), description = "FM synthesis amount for synth sounds — more = brighter, more metallic."),
        FunctionSpec("fmh", "fm ratio", FunctionSpec.Category.SOUND, listOf(ParamSpec.Number("ratio", 0.25, 8.0, 0.25, 2.0)), description = "FM modulator ratio: whole numbers stay harmonic, fractions get bell-like."),
        FunctionSpec("phaser", "phaser", FunctionSpec.Category.SOUND, listOf(ParamSpec.Number("hz", 0.0, 10.0, 0.25, 2.0)), description = "Phaser sweep speed in Hz."),

        // --- more pitch ---
        FunctionSpec("add", "add", FunctionSpec.Category.PITCH, listOf(ParamSpec.Number("amount", -24.0, 24.0, 1.0, 12.0)), description = "Adds to the note/n value: 12 = an octave up, 7 = a fifth up, -12 = an octave down."),
        FunctionSpec("scaleTranspose", "scale transpose", FunctionSpec.Category.PITCH, listOf(ParamSpec.Number("steps", -7.0, 7.0, 1.0, 2.0)), description = "Moves notes by scale steps (needs a scale block), staying in key."),
        FunctionSpec("detune", "detune", FunctionSpec.Category.PITCH, listOf(ParamSpec.Number("amount", 0.0, 1.0, 0.01, 0.1)), description = "Slightly detunes synth voices for a thicker, chorused sound."),
        FunctionSpec("arp", "arpeggio", FunctionSpec.Category.PITCH, listOf(ParamSpec.Text("order", "0 1 2 3")), description = "Turns simultaneous notes (chords like \"[c3,e3,g3]\") into a sequence in this order."),

        // --- more filter ---
        FunctionSpec("lpq", "low-pass resonance", FunctionSpec.Category.FILTER, listOf(ParamSpec.Number("q", 0.0, 30.0, 0.5, 5.0)), description = "Resonance of the low-pass filter: boosts the cutoff frequency — acid squelch."),
        FunctionSpec("hpq", "high-pass resonance", FunctionSpec.Category.FILTER, listOf(ParamSpec.Number("q", 0.0, 30.0, 0.5, 5.0)), description = "Resonance of the high-pass filter."),
        FunctionSpec("bpf", "band-pass", FunctionSpec.Category.FILTER, listOf(ParamSpec.Number("center", 50.0, 8000.0, 50.0, 1000.0)), description = "Band-pass filter: keeps only frequencies around the center — telephone / radio sound."),
        FunctionSpec("bpq", "band-pass resonance", FunctionSpec.Category.FILTER, listOf(ParamSpec.Number("q", 0.0, 30.0, 0.5, 5.0)), description = "How narrow the band-pass band is."),
        FunctionSpec("lpenv", "filter envelope", FunctionSpec.Category.FILTER, listOf(ParamSpec.Number("amount", -8.0, 8.0, 0.5, 4.0)), description = "Sweeps the low-pass cutoff with an envelope: positive opens then closes. Pair with lpdecay."),
        FunctionSpec("lpdecay", "filter decay", FunctionSpec.Category.FILTER, listOf(ParamSpec.Number("seconds", 0.0, 2.0, 0.05, 0.2)), description = "How fast the filter envelope closes again."),

        // --- more space ---
        FunctionSpec("roomfade", "reverb fade", FunctionSpec.Category.SPACE, listOf(ParamSpec.Number("seconds", 0.1, 10.0, 0.1, 2.0)), description = "Reverb tail length in seconds."),
        FunctionSpec("roomlp", "reverb tone", FunctionSpec.Category.SPACE, listOf(ParamSpec.Number("cutoff", 200.0, 10000.0, 100.0, 5000.0)), description = "Darkens the reverb by low-passing its tail."),
        FunctionSpec("orbit", "orbit", FunctionSpec.Category.SPACE, listOf(ParamSpec.Number("bus", 1.0, 4.0, 1.0, 2.0)), description = "Effects bus: layers on different orbits get separate reverb/delay instead of sharing one."),

        // --- chords ---
        FunctionSpec("chord", "chord", FunctionSpec.Category.PITCH, listOf(ParamSpec.Text("symbols", "<C^7 Am7 Dm7 G7>")), description = "Chord symbols per step. On an n chain, n picks notes out of each chord. Needs .voicing() after it."),
        FunctionSpec("voicing", "voicing", FunctionSpec.Category.PITCH, description = "Turns chord symbols into actual notes, choosing smooth voice-leading between chords."),
        FunctionSpec("anchor", "anchor", FunctionSpec.Category.PITCH, listOf(ParamSpec.Text("note", "c5", isMini = false)), description = "Where voicings sit: the top note stays at or below this (default c5). Put it before .voicing()."),
        FunctionSpec("mode", "voicing mode", FunctionSpec.Category.PITCH, listOf(ParamSpec.Choice("mode", listOf("below", "above", "duck", "root"), "below")), description = "How voicings relate to the anchor: below/above it, duck (avoid it), or root (anchor is the bass note)."),

        // --- layer ---
        FunctionSpec("layer", "layer", FunctionSpec.Category.STRUCTURE, listOf(ParamSpec.Fn("a", inner("fast", 2.0)), ParamSpec.Fn("b", innerRev())), description = "Plays several versions of the pattern at once, one per function — like a stack of transformations.", variadicFn = true),
    )

    private val byName = transforms.associateBy { it.name }

    fun spec(fn: String): FunctionSpec? = byName[fn]

    fun newTransform(fn: String): Transform {
        val spec = spec(fn) ?: return Transform(fn = fn)
        return Transform(fn = fn, args = spec.defaultArgs())
    }
}
