package com.okan.strudelmobile.feedback

import android.content.Context
import android.os.Build
import android.util.Log
import com.okan.strudelmobile.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

enum class FeedbackKind(val label: String, val wire: String) {
    BUG("Bug", "bug"),
    IDEA("Idea", "idea"),
    OTHER("Other", "other"),
}

/**
 * One report, in the shape the shared feedback server expects:
 * `project` tells the server which app this came from, the rest is free-form.
 */
@Serializable
data class FeedbackReport(
    val project: String = PROJECT,
    val kind: String,
    val message: String,
    val contact: String? = null,
    val pattern: String? = null,
    val appVersion: String = BuildConfig.VERSION_NAME,
    val appVersionCode: Int = BuildConfig.VERSION_CODE,
    val device: String = "${Build.MANUFACTURER} ${Build.MODEL}",
    val androidVersion: String = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
    val createdAt: Long = System.currentTimeMillis(),
) {
    companion object {
        const val PROJECT = "strudel-mobile"
    }
}

sealed interface SendResult {
    /** Delivered to the server. */
    data object Sent : SendResult

    /** Kept on the device; retried on the next submit / app start once a server exists. */
    data object Queued : SendResult
}

/**
 * Feedback goes into a small on-device outbox first, then the outbox is
 * flushed to [BuildConfig.FEEDBACK_URL]. With an empty URL (the current state:
 * the server is not up yet) nothing leaves the device — reports just accumulate
 * in the outbox and are delivered by the first build that has the URL set.
 */
class FeedbackClient(context: Context) {

    private val prefs = context.getSharedPreferences("feedback", Context.MODE_PRIVATE)
    // encodeDefaults: project/version/device are defaults and must still reach the server.
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    val configured: Boolean get() = BuildConfig.FEEDBACK_URL.isNotBlank()

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
        val remaining = outbox().dropWhile { post(it) }
        saveOutbox(remaining)
        remaining.isEmpty()
    }

    private fun post(report: FeedbackReport): Boolean = runCatching {
        val conn = URL(BuildConfig.FEEDBACK_URL).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 8_000
            conn.readTimeout = 8_000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("User-Agent", "${FeedbackReport.PROJECT}/${BuildConfig.VERSION_NAME}")
            conn.outputStream.use { it.write(json.encodeToString(report).toByteArray()) }
            conn.responseCode in 200..299
        } finally {
            conn.disconnect()
        }
    }.onFailure { Log.w(TAG, "feedback post failed", it) }.getOrDefault(false)

    private fun saveOutbox(reports: List<FeedbackReport>) {
        // Cap so a never-configured build can't grow the prefs file forever.
        prefs.edit().putString(KEY_OUTBOX, json.encodeToString(reports.takeLast(MAX_OUTBOX))).apply()
    }

    private companion object {
        const val TAG = "Feedback"
        const val KEY_OUTBOX = "outbox"
        const val MAX_OUTBOX = 50
    }
}
