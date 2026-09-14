package com.okan.strudelmobile.model

import java.util.UUID

/**
 * The pattern tree — the single source of truth for what the app plays.
 *
 * A [Chain] is one Strudel expression: a source followed by chained method
 * calls, e.g. `s("bd sd").fast(2).gain(0.8)`. The source is either a
 * mini-notation call ([MiniSource], `s("bd sd")`) or a group of child chains
 * ([GroupSource], `stack(a, b)`), which is how `stack()` / sequences nest.
 *
 * Everything is immutable; edits produce a new tree via the helpers in
 * [TreeOps].
 */
data class Chain(
    val id: String = newId(),
    val source: Source,
    val transforms: List<Transform> = emptyList(),
)

sealed interface Source

/** A function call taking one mini-notation string: `s("bd sd")`, `note("c e g")`, `n("0 2 4")`. */
data class MiniSource(
    val fn: String,
    val pattern: String,
) : Source

/** A combinator over child chains: `stack(...)`, `seq(...)`, `cat(...)`. */
data class GroupSource(
    val fn: String,
    val children: List<Chain>,
) : Source

/** One chained method call: `.fast(2)`, `.s("piano")`, `.rev()`. */
data class Transform(
    val id: String = newId(),
    val fn: String,
    val args: List<Arg> = emptyList(),
)

sealed interface Arg {
    data class Num(val value: Double) : Arg
    data class Str(val value: String) : Arg
}

fun newId(): String = UUID.randomUUID().toString().substring(0, 8)

/** A sounding token: which source node, and which char range inside its pattern string. */
data class ActiveToken(val chainId: String, val from: Int, val to: Int)
