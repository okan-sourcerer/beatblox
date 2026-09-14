package com.okan.beatblox.feedback

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The JSON must match the hub's `POST /api/feedback` contract (docs/integration.md). */
class FeedbackReportTest {

    private val json = Json { encodeDefaults = true; explicitNulls = false }

    @Test
    fun `uses the hub's snake_case field names and omits nulls`() {
        val report = FeedbackReport(
            type = "bug",
            message = "Save does nothing",
            userId = "install-1",
            metadata = buildJsonObject { put("pattern", "s(\"bd\")") },
        )
        val obj = json.parseToJsonElement(json.encodeToString(report)).jsonObject

        assertEquals("bug", obj["type"]!!.jsonPrimitive.content)
        assertEquals("install-1", obj["user_id"]!!.jsonPrimitive.content)
        assertEquals("android", obj["platform"]!!.jsonPrimitive.content)
        assertTrue(obj.containsKey("app_version"))
        assertTrue(obj.containsKey("idempotency_key"))
        assertTrue(obj.containsKey("environment"))
        assertEquals("s(\"bd\")", obj["metadata"]!!.jsonObject["pattern"]!!.jsonPrimitive.content)
        // Optional fields left null must not be sent as `null`.
        assertFalse(obj.containsKey("user_email"))
        assertFalse(obj.containsKey("title"))
        // The hub derives app_id from the key; sending it is a contract violation.
        assertFalse(obj.containsKey("app_id"))
    }

    @Test
    fun `idempotency key survives a round trip through the outbox`() {
        val report = FeedbackReport(type = "other", message = "hi", userId = "u")
        val back = json.decodeFromString<FeedbackReport>(json.encodeToString(report))
        assertEquals(report.idempotencyKey, back.idempotencyKey)
    }

    @Test
    fun `kinds map onto the hub's type enum`() {
        val allowed = setOf("bug", "feature", "question", "praise", "other")
        FeedbackKind.entries.forEach { assertTrue(it.wire, it.wire in allowed) }
    }
}
