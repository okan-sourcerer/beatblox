package com.okan.strudelmobile.model

import java.util.Locale

/**
 * Where a [MiniSource]'s string sits in the generated code: [start] is the
 * offset of the opening quote, [end] the offset of the closing quote. Hap
 * locations reported by Strudel are absolute offsets into the same code, so
 * the JS side maps them back to [chainId] with a range lookup.
 */
data class SourceLocation(val chainId: String, val start: Int, val end: Int)

data class Serialized(val code: String, val locations: List<SourceLocation>) {
    fun locationsJson(): String = locations.joinToString(",", "[", "]") {
        """{"id":"${it.chainId}","start":${it.start},"end":${it.end}}"""
    }
}

/** Tree -> Strudel code. */
object Serializer {

    fun serialize(root: Chain): Serialized {
        val sb = StringBuilder()
        val locs = mutableListOf<SourceLocation>()
        writeChain(root, sb, locs)
        return Serialized(sb.toString(), locs)
    }

    /**
     * Code for previewing [targetId] in isolation: the target chain plus the
     * transforms of every ancestor, but *not* the ancestors' other children.
     * If [sourceOverride] is given, the target's mini-notation string is
     * replaced by it (used to audition a single note through the chain).
     */
    fun serializePreview(root: Chain, targetId: String, sourceOverride: String? = null): Serialized? {
        val path = TreeOps.pathTo(root, targetId) ?: return null
        var target = path.last()
        if (sourceOverride != null && target.source is MiniSource) {
            target = target.copy(source = (target.source as MiniSource).copy(pattern = sourceOverride))
        }
        val sb = StringBuilder()
        val locs = mutableListOf<SourceLocation>()
        writeChain(target, sb, locs)
        // Nearest ancestor first, so its transforms wrap the target before the
        // next one up — same order as the real tree.
        for (ancestor in path.dropLast(1).asReversed()) {
            writeTransforms(ancestor.transforms, sb)
        }
        return Serialized(sb.toString(), locs)
    }

    private fun writeChain(chain: Chain, sb: StringBuilder, locs: MutableList<SourceLocation>) {
        when (val src = chain.source) {
            is MiniSource -> {
                sb.append(src.fn).append('(')
                val start = sb.length
                sb.append('"').append(escape(src.pattern)).append('"')
                locs += SourceLocation(chain.id, start, sb.length - 1)
                sb.append(')')
            }
            is GroupSource -> {
                sb.append(src.fn).append('(')
                src.children.forEachIndexed { i, child ->
                    if (i > 0) sb.append(", ")
                    writeChain(child, sb, locs)
                }
                sb.append(')')
            }
        }
        writeTransforms(chain.transforms, sb)
    }

    private fun writeTransforms(transforms: List<Transform>, sb: StringBuilder) {
        for (t in transforms) {
            sb.append('.').append(t.fn).append('(')
            t.args.forEachIndexed { i, arg ->
                if (i > 0) sb.append(", ")
                sb.append(argCode(arg))
            }
            sb.append(')')
        }
    }

    fun argCode(arg: Arg): String = when (arg) {
        is Arg.Num -> formatNumber(arg.value)
        is Arg.Str -> "\"${escape(arg.value)}\""
    }

    fun formatNumber(v: Double): String =
        if (v == Math.rint(v) && !v.isInfinite()) v.toLong().toString()
        else String.format(Locale.ROOT, "%.4f", v).trimEnd('0').trimEnd('.')

    /**
     * Mini-notation strings are emitted as double-quoted JS strings. A real
     * newline would break the JS, and an escaped quote/backslash must survive
     * so the offsets in the location table line up with what Strudel sees.
     * We keep it simple by stripping the characters that can't be typed into
     * a pattern anyway.
     */
    private fun escape(s: String): String =
        s.replace("\\", "").replace("\"", "").replace("\n", " ").replace("\r", " ")
}
