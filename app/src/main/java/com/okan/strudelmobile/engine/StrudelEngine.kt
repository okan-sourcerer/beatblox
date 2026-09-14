package com.okan.strudelmobile.engine

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader
import com.okan.strudelmobile.model.ActiveToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.json.JSONObject

@Serializable
data class SoundInfo(val name: String, val type: String = "sample", val count: Int = 1, val tag: String = "")

/**
 * Owns the WebView that hosts Strudel and is the only thing that talks to it.
 *
 * The WebView must stay attached to the view hierarchy for its timers and
 * audio to be reliable, so [webView] is handed to a tiny `AndroidView` in the
 * Compose tree (see [StrudelWebViewHost]). All calls into JS go through
 * [evaluateJavascript] on the main thread; callbacks from JS arrive on a
 * WebView thread and are published through StateFlows.
 */
@SuppressLint("SetJavaScriptEnabled")
class StrudelEngine(context: Context) {

    private val main = Handler(Looper.getMainLooper())
    private val json = Json { ignoreUnknownKeys = true }

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val _playing = MutableStateFlow(false)
    val playing: StateFlow<Boolean> = _playing.asStateFlow()

    private val _active = MutableStateFlow<Set<ActiveToken>>(emptySet())
    val active: StateFlow<Set<ActiveToken>> = _active.asStateFlow()

    private val _sounds = MutableStateFlow<List<SoundInfo>>(emptyList())
    val sounds: StateFlow<List<SoundInfo>> = _sounds.asStateFlow()

    private val _log = MutableStateFlow<List<String>>(emptyList())
    val log: StateFlow<List<String>> = _log.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val assetLoader = WebViewAssetLoader.Builder()
        .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
        .build()

    val webView: WebView = WebView(context).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        // Web Audio may start without a click: transport buttons are native.
        settings.mediaPlaybackRequiresUserGesture = false
        settings.allowFileAccess = false
        addJavascriptInterface(Bridge(), "Android")
        webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                assetLoader.shouldInterceptRequest(request.url)
        }
        webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(msg: ConsoleMessage): Boolean {
                Log.d(TAG, "console: ${msg.message()} (${msg.sourceId()}:${msg.lineNumber()})")
                if (msg.messageLevel() == ConsoleMessage.MessageLevel.ERROR) appendLog("js: ${msg.message()}")
                return true
            }
        }
        // Served over https://appassets.androidplatform.net so the page is a
        // secure context (AudioWorklet needs one) and can fetch sample banks.
        loadUrl("https://appassets.androidplatform.net/assets/strudel/index.html")
    }

    // --- Kotlin -> JS ---------------------------------------------------------

    fun setPattern(code: String, locationsJson: String, autostart: Boolean) {
        js("StrudelBridge.setPattern(${JSONObject.quote(code)}, ${JSONObject.quote(locationsJson)}, $autostart)")
    }

    fun start() = js("StrudelBridge.start()")
    fun stop() = js("StrudelBridge.stop()")
    fun hush() = js("StrudelBridge.hush()")
    fun setCpm(cpm: Double) = js("StrudelBridge.setCpm($cpm)")

    /** One-shot playback of [code] for [cycles] cycles, independent of the transport. */
    fun preview(code: String, cycles: Int = 1) {
        js("StrudelBridge.preview(${JSONObject.quote(code)}, $cycles)")
    }

    fun clearError() = _lastError.update { null }

    private fun js(script: String) {
        if (!_ready.value) {
            appendLog("engine not ready, dropped: ${script.take(60)}")
            return
        }
        main.post {
            webView.evaluateJavascript(script) { result ->
                if (result != null && result != "null" && result != "undefined" && result.startsWith("\"")) {
                    Log.d(TAG, "js result: $result")
                }
            }
        }
    }

    fun destroy() {
        main.post { webView.destroy() }
    }

    private fun appendLog(line: String) {
        _log.update { (it + line).takeLast(200) }
    }

    // --- JS -> Kotlin ---------------------------------------------------------

    private inner class Bridge {
        @JavascriptInterface
        fun onReady() {
            _ready.value = true
            appendLog("strudel ready")
        }

        @JavascriptInterface
        fun onToggle(started: Boolean) {
            _playing.value = started
        }

        @JavascriptInterface
        fun onPatternSet(code: String) {
            Log.d(TAG, "pattern set: $code")
        }

        @JavascriptInterface
        fun onLog(message: String, type: String) {
            appendLog(message)
        }

        @JavascriptInterface
        fun onError(message: String) {
            Log.w(TAG, "strudel error: $message")
            appendLog("error: $message")
            _lastError.value = message
        }

        /** Comma-separated `chainId:from:to` entries, empty when nothing sounds. */
        @JavascriptInterface
        fun onActive(key: String) {
            if (key.isEmpty()) {
                _active.value = emptySet()
                return
            }
            val set = HashSet<ActiveToken>()
            for (item in key.split(',')) {
                val parts = item.split(':')
                if (parts.size == 3) {
                    val from = parts[1].toIntOrNull() ?: continue
                    val to = parts[2].toIntOrNull() ?: continue
                    set += ActiveToken(parts[0], from, to)
                }
            }
            _active.value = set
        }

        @JavascriptInterface
        fun onSounds(payload: String) {
            runCatching { json.decodeFromString<List<SoundInfo>>(payload) }
                .onSuccess { _sounds.value = it }
                .onFailure { appendLog("bad sound list: ${it.message}") }
        }
    }

    private companion object {
        const val TAG = "StrudelEngine"
    }
}
