package com.okan.beatblox.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.widget.Toast

/** Getting a pattern *out* of the app: clipboard, the system share sheet, or strudel.cc. */
object Share {

    /**
     * Stand-alone Strudel code: the tempo line first, so pasting it into the
     * strudel.cc REPL plays at the same speed as here.
     */
    fun exportCode(code: String, cpm: Double, name: String?): String = buildString {
        if (name != null) append("// ").append(name).append('\n')
        append("setcpm(").append(cpm.toInt()).append(")\n\n")
        append(code)
        append('\n')
    }

    /**
     * The REPL reads its code from the URL fragment as url-encoded base64 of
     * the UTF-8 text (`strudel.cc/#...`), so a link *is* the pattern.
     */
    fun strudelUrl(exported: String): String {
        val b64 = Base64.encodeToString(exported.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        return "https://strudel.cc/#" + Uri.encode(b64)
    }

    fun copy(context: Context, text: String, what: String = "Code") {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("Strudel pattern", text))
        // Android 13+ shows its own clipboard toast; older versions don't.
        if (android.os.Build.VERSION.SDK_INT < 33) {
            Toast.makeText(context, "$what copied", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareText(context: Context, text: String, subject: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(send, "Share pattern"))
    }

    fun open(context: Context, url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            .onFailure { Toast.makeText(context, "No app can open $url", Toast.LENGTH_SHORT).show() }
    }
}
