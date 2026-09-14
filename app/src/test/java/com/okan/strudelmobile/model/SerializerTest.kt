package com.okan.strudelmobile.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SerializerTest {

    private val drums = Chain(
        id = "A",
        source = MiniSource("s", "bd [sd hh] hh"),
        transforms = listOf(Transform(id = "t1", fn = "fast", args = listOf(Arg.Num(2.0)))),
    )
    private val keys = Chain(
        id = "B",
        source = MiniSource("note", "c3 e3"),
        transforms = listOf(Transform(id = "t2", fn = "s", args = listOf(Arg.Str("piano")))),
    )
    private val root = Chain(
        id = "R",
        source = GroupSource("stack", listOf(drums, keys)),
        transforms = listOf(Transform(id = "t3", fn = "room", args = listOf(Arg.Num(0.5)))),
    )

    @Test
    fun `serializes a nested stack with transforms`() {
        val out = Serializer.serialize(root)
        assertEquals("""stack(s("bd [sd hh] hh").fast(2), note("c3 e3").s("piano")).room(0.5)""", out.code)
    }

    @Test
    fun `location table points at the quotes of each mini source`() {
        val out = Serializer.serialize(root)
        val byId = out.locations.associateBy { it.chainId }
        val a = byId.getValue("A")
        val b = byId.getValue("B")
        assertEquals('"', out.code[a.start])
        assertEquals('"', out.code[a.end])
        assertEquals("bd [sd hh] hh", out.code.substring(a.start + 1, a.end))
        assertEquals("c3 e3", out.code.substring(b.start + 1, b.end))
        // Transform string args must not appear in the table.
        assertEquals(2, out.locations.size)
    }

    @Test
    fun `preview excludes sibling layers but keeps ancestor transforms`() {
        val out = Serializer.serializePreview(root, "B")!!
        assertEquals("""note("c3 e3").s("piano").room(0.5)""", out.code)
    }

    @Test
    fun `preview can override the source string`() {
        val out = Serializer.serializePreview(root, "B", sourceOverride = "g4")!!
        assertEquals("""note("g4").s("piano").room(0.5)""", out.code)
    }

    @Test
    fun `preview of unknown id is null`() {
        assertNull(Serializer.serializePreview(root, "nope"))
    }

    @Test
    fun `numbers format without trailing noise`() {
        assertEquals("2", Serializer.formatNumber(2.0))
        assertEquals("0.5", Serializer.formatNumber(0.5))
        assertEquals("0.125", Serializer.formatNumber(0.125))
        assertEquals("-1", Serializer.formatNumber(-1.0))
    }

    @Test
    fun `no-arg transforms serialize as empty calls`() {
        val c = Chain(id = "X", source = MiniSource("s", "bd"), transforms = listOf(Transform(fn = "rev")))
        assertEquals("""s("bd").rev()""", Serializer.serialize(c).code)
    }

    @Test
    fun `tree ops keep the tree consistent`() {
        var r = TreeOps.setPattern(root, "A", "bd sd")
        assertEquals("bd sd", (TreeOps.find(r, "A")!!.source as MiniSource).pattern)

        r = TreeOps.moveTransform(r, "t1", 1) // out of range: no-op
        assertEquals(listOf("t1"), TreeOps.find(r, "A")!!.transforms.map { it.id })

        r = TreeOps.removeChild(r, "R", "A")
        // Group of one collapses: B's transforms come first, then R's.
        assertEquals("""note("c3 e3").s("piano").room(0.5)""", Serializer.serialize(r).code)
        assertEquals("R", r.id)
    }

    @Test
    fun `function args serialize as arrow functions and nested ops work`() {
        val inner = Transform(id = "in1", fn = "fast", args = listOf(Arg.Num(2.0)))
        val every = Transform(id = "ev", fn = "every", args = listOf(Arg.Num(4.0), Arg.Fn(listOf(inner))))
        val jux = Transform(id = "jx", fn = "jux", args = listOf(Arg.Fn()))
        var c = Chain(id = "X", source = MiniSource("s", "bd sd"), transforms = listOf(every, jux))
        assertEquals("""s("bd sd").every(4, x => x.fast(2)).jux(x => x)""", Serializer.serialize(c).code)

        // nested lookup / edit / remove go through the same ops as top-level blocks
        assertEquals("X", TreeOps.chainOfTransform(c, "in1")!!.id)
        c = TreeOps.setArg(c, "in1", 0, Arg.Num(3.0))
        assertEquals("""s("bd sd").every(4, x => x.fast(3)).jux(x => x)""", Serializer.serialize(c).code)
        c = TreeOps.addNestedTransform(c, "jx", 0, Transform(id = "in2", fn = "rev"))
        assertEquals("""s("bd sd").every(4, x => x.fast(3)).jux(x => x.rev())""", Serializer.serialize(c).code)
        c = TreeOps.removeTransform(c, "in1")
        assertEquals("""s("bd sd").every(4, x => x).jux(x => x.rev())""", Serializer.serialize(c).code)
    }
}
