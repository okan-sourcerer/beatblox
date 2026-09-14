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
}

data class FunctionSpec(
    val name: String,
    val label: String,
    val category: Category,
    val params: List<ParamSpec> = emptyList(),
    /** One plain-language sentence for people who don't know Strudel. */
    val description: String = "",
) {
    enum class Category { TIME, SOUND, PITCH, FILTER, SPACE, DYNAMICS, STRUCTURE }

    fun defaultArgs(): List<Arg> = params.map {
        when (it) {
            is ParamSpec.Number -> Arg.Num(it.default)
            is ParamSpec.Text -> Arg.Str(it.default)
            is ParamSpec.Choice -> Arg.Str(it.default)
        }
    }
}

object Vocabulary {
    /** Functions that can start a chain and take a mini-notation string. */
    val sourceFns = listOf("s", "note", "n")

    val sourceDescriptions = mapOf(
        "s" to "sound: play samples by name — \"bd sd hh\". Unpitched; pick names in the Sounds tab.",
        "note" to "note: play pitches — \"c3 e3 g3\" or MIDI numbers. Add an .s() block to choose the instrument.",
        "n" to "n: numbers — sample index inside a sound, or scale degree when a .scale() block is present.",
    )

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
    )

    private val byName = transforms.associateBy { it.name }

    fun spec(fn: String): FunctionSpec? = byName[fn]

    fun newTransform(fn: String): Transform {
        val spec = spec(fn) ?: return Transform(fn = fn)
        return Transform(fn = fn, args = spec.defaultArgs())
    }
}
