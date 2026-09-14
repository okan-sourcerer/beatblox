package com.okan.beatblox.engine

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Keeps the engine's WebView attached to the window. Android's WebView is not
 * dependable as a detached/headless engine (timers throttle, some OEMs stall
 * rendering), so it lives here at 1dp and invisible rather than off-tree.
 */
@Composable
fun StrudelWebViewHost(engine: StrudelEngine, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { engine.webView },
        modifier = modifier.size(1.dp).alpha(0f),
    )
}
