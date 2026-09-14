package com.okan.beatblox.feedback

import android.content.Context
import android.os.Build
import android.util.Log
import com.okan.beatblox.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

/** The hub's `type` enum, with the label the chip shows. */
enum class FeedbackKind(val label: String, val wire: String) {
    BUG("Bug", "bug"),
    IDEA("Idea", "feature"),
    QUESTION("Question", "question"),
    PRAISE("Praise", "praise"),
    OTHER("Other", "other"),
}

@Serializable
data class LogLine(val level: String, val msg: String)

/**
 * One report in the Coreworkbench hub's `POST /api/feedback` shape (see
 * docs/integration.md). Only `message` is required there; the rest is context
 * that makes the admin view useful. `app_id` is deliberately absent — the hub
 * derives it from the API key.
 */
@Serializable
data class FeedbackReport(
    val type: String,
    val message: String,
    val title: String? = null,
    @SerialName("user_email") val userEmail: String? = null,
    @SerialName("user_id") val userId: String,
    val logs: List<LogLine>? = null,
    val metadata: JsonObject? = null,
    @SerialName("idempotency_key") val idempotencyKey: String = UUID.randomUUID().toString(),
    @SerialName("app_version") val appVersion: String = BuildConfig.VERSION_NAME,
    val environment: String = BuildConfig.ENVIRONMENT,
    val platform: String = "android",
    val os: String = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
    val device: String = "${Build.MANUFACTURER} ${Build.MODEL}",
    val locale: String = Locale.getDefault().toLanguageTag(),
    val timezone: String = TimeZone.getDefault().id,
    @SerialName("screen_size") val screenSize: String? = null,
)

sealed interface SendResult {
    /** Accepted by the hub. */
    data object Sent : SendResult

    /** Kept on the device; retried on the next submit / app start. */
    data object Queued : SendResult
}

/**
 * Feedback goes into a small on-device outbox first, then the outbox is
 * flushed to the hub. With no API key baked in (`beatblox.hubKey` empty)
 * nothing leaves the device — reports accumulate and are delivered by the
 * first build that has the key. Each report carries its own idempotency key,
 * so a retry after a lost response can't create a duplicate.
 */
class FeedbackClient(context: Context) {

    private val prefs = context.getSharedPreferences("feedback", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = false }

    val configured: Boolean get() = BuildConfig.HUB_KEY.isNotBlank()

    /** Random, per-install. Only ever leaves the app as the hub's opaque `user_id`. */
    val installId: String
        get() = prefs.getString(KEY_INSTALL, null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString(KEY_INSTALL, it).apply()
        }

    fun outbox(): List<FeedbackReport> {
        val raw = prefs.getString(KEY_OUTBOX, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<FeedbackReport>>(raw) }
            .onFailure { Log.w(TAG, "could not read outbox", it) }
            .getOrDefault(emptyList())
    }

    suspend fun submit(report: FeedbackReport): SendResult {
        saveOutbox(outbox() + report)
        return if (flush()) SendResult.Sent else SendResult.Queued
    }

    /** Tries to deliver everything queued. Returns true when the outbox is empty afterwards. */
    suspend fun flush(): Boolean = withContext(Dispatchers.IO) {
        if (!configured) return@withContext false
        val remaining = mutableListOf<FeedbackReport>()
        var stop = false
        for (r in outbox()) {
            if (stop) { remaining += r; continue }
            when (post(r)) {
                Outcome.ACCEPTED, Outcome.REJECTED -> Unit // rejected would fail again: drop it
                Outcome.RETRY -> { remaining += r; stop = true }
            }
        }
        saveOutbox(remaining)
        remaining.isEmpty()
    }

    private enum class Outcome { ACCEPTED, REJECTED, RETRY }

    private fun post(report: FeedbackReport): Outcome = runCatching {
        val conn = URL("${BuildConfig.HUB_URL}/api/feedback").openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 8_000
            conn.readTimeout = 8_000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("Authorization", "Bearer ${BuildConfig.HUB_KEY}")
            conn.setRequestProperty("User-Agent", "beatblox/${BuildConfig.VERSION_NAME}")
            conn.outputStream.use { it.write(json.encodeToString(report).toByteArray()) }
            val code = conn.responseCode
            when {
                code in 200..299 -> Outcome.ACCEPTED // 201 created; 200 = duplicate of a retried key
                code == 401 || code == 422 -> {
                    val body = runCatching { conn.errorStream?.bufferedReader()?.readText() }.getOrNull()
                    Log.w(TAG, "hub rejected feedback ($code): $body")
                    Outcome.REJECTED
                }
                else -> Outcome.RETRY // 429 rate limit, 5xx
            }
        } finally {
            conn.disconnect()
        }
    }.onFailure { Log.w(TAG, "feedback post failed", it) }.getOrDefault(Outcome.RETRY)

    private fun saveOutbox(reports: List<FeedbackReport>) {
        // Cap so a never-configured build can't grow the prefs file forever.
        prefs.edit().putString(KEY_OUTBOX, json.encodeToString(reports.takeLast(MAX_OUTBOX))).apply()
    }

    private companion object {
        const val TAG = "Feedback"
        const val KEY_OUTBOX = "outbox"
        const val KEY_INSTALL = "install_id"
        const val MAX_OUTBOX = 50
    }
}
