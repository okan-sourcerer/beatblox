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

    /** Functions that combine child chains. */
    val groupFns = listOf("stack", "seq", "cat")

    val scales = listOf(
        "C:major", "C:minor", "C:pentatonic", "C:minor:pentatonic", "C:dorian", "C:mixolydian",
        "C:lydian", "C:phrygian", "C:blues", "D:major", "D:minor", "E:minor", "F:major",
        "G:major", "G:minor", "A:minor", "A:major", "Bb:major", "Eb:major",
    )

    val transforms: List<FunctionSpec> = listOf(
        FunctionSpec("fast", "fast", FunctionSpec.Category.TIME, listOf(ParamSpec.Number("factor", 0.25, 8.0, 0.25, 2.0))),
        FunctionSpec("slow", "slow", FunctionSpec.Category.TIME, listOf(ParamSpec.Number("factor", 0.25, 8.0, 0.25, 2.0))),
        FunctionSpec("rev", "reverse", FunctionSpec.Category.TIME),
        FunctionSpec("late", "late", FunctionSpec.Category.TIME, listOf(ParamSpec.Number("cycles", 0.0, 1.0, 0.125, 0.25))),
        FunctionSpec("struct", "struct", FunctionSpec.Category.STRUCTURE, listOf(ParamSpec.Text("pattern", "x ~ x x"))),
        FunctionSpec("degradeBy", "degrade", FunctionSpec.Category.STRUCTURE, listOf(ParamSpec.Number("amount", 0.0, 1.0, 0.05, 0.5))),
        FunctionSpec("ply", "ply", FunctionSpec.Category.STRUCTURE, listOf(ParamSpec.Number("times", 1.0, 8.0, 1.0, 2.0))),

        FunctionSpec("s", "sound", FunctionSpec.Category.SOUND, listOf(ParamSpec.Text("sound", "piano"))),
        FunctionSpec("bank", "bank", FunctionSpec.Category.SOUND, listOf(ParamSpec.Text("bank", "RolandTR909", isMini = false))),
        FunctionSpec("n", "sample #", FunctionSpec.Category.SOUND, listOf(ParamSpec.Text("index", "0"))),
        FunctionSpec("speed", "speed", FunctionSpec.Category.SOUND, listOf(ParamSpec.Number("speed", -2.0, 2.0, 0.05, 1.0))),
        FunctionSpec("crush", "bitcrush", FunctionSpec.Category.SOUND, listOf(ParamSpec.Number("bits", 1.0, 16.0, 1.0, 8.0))),

        FunctionSpec("note", "note", FunctionSpec.Category.PITCH, listOf(ParamSpec.Text("notes", "c3"))),
        FunctionSpec("scale", "scale", FunctionSpec.Category.PITCH, listOf(ParamSpec.Choice("scale", scales, "C:major"))),
        FunctionSpec("transpose", "transpose", FunctionSpec.Category.PITCH, listOf(ParamSpec.Number("semitones", -24.0, 24.0, 1.0, 12.0))),

        FunctionSpec("lpf", "low-pass", FunctionSpec.Category.FILTER, listOf(ParamSpec.Number("cutoff", 50.0, 8000.0, 50.0, 800.0))),
        FunctionSpec("hpf", "high-pass", FunctionSpec.Category.FILTER, listOf(ParamSpec.Number("cutoff", 20.0, 5000.0, 20.0, 200.0))),
        FunctionSpec("vowel", "vowel", FunctionSpec.Category.FILTER, listOf(ParamSpec.Choice("vowel", listOf("a", "e", "i", "o", "u"), "a"))),

        FunctionSpec("room", "reverb", FunctionSpec.Category.SPACE, listOf(ParamSpec.Number("amount", 0.0, 1.0, 0.05, 0.5))),
        FunctionSpec("size", "reverb size", FunctionSpec.Category.SPACE, listOf(ParamSpec.Number("size", 0.0, 1.0, 0.05, 0.5))),
        FunctionSpec("delay", "delay", FunctionSpec.Category.SPACE, listOf(ParamSpec.Number("amount", 0.0, 1.0, 0.05, 0.5))),
        FunctionSpec("delaytime", "delay time", FunctionSpec.Category.SPACE, listOf(ParamSpec.Number("time", 0.0, 1.0, 0.0625, 0.25))),
        FunctionSpec("delayfeedback", "delay feedback", FunctionSpec.Category.SPACE, listOf(ParamSpec.Number("feedback", 0.0, 0.95, 0.05, 0.5))),
        FunctionSpec("pan", "pan", FunctionSpec.Category.SPACE, listOf(ParamSpec.Number("pan", 0.0, 1.0, 0.05, 0.5))),

        FunctionSpec("gain", "gain", FunctionSpec.Category.DYNAMICS, listOf(ParamSpec.Number("gain", 0.0, 2.0, 0.05, 1.0))),
        FunctionSpec("attack", "attack", FunctionSpec.Category.DYNAMICS, listOf(ParamSpec.Number("seconds", 0.0, 2.0, 0.01, 0.01))),
        FunctionSpec("release", "release", FunctionSpec.Category.DYNAMICS, listOf(ParamSpec.Number("seconds", 0.0, 2.0, 0.01, 0.1))),
    )

    private val byName = transforms.associateBy { it.name }

    fun spec(fn: String): FunctionSpec? = byName[fn]

    fun newTransform(fn: String): Transform {
        val spec = spec(fn) ?: return Transform(fn = fn)
        return Transform(fn = fn, args = spec.defaultArgs())
    }
}
