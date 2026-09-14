package com.okan.beatblox.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.IconButton
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.okan.beatblox.engine.StrudelWebViewHost
import kotlinx.coroutines.delay

@Composable
fun BeatBloxApp(vm: EditorViewModel) {
    val root by vm.root.collectAsStateWithLifecycle()
    val selection by vm.selection.collectAsStateWithLifecycle()
    val panel by vm.panel.collectAsStateWithLifecycle()
    val active by vm.engine.active.collectAsStateWithLifecycle()
    val ready by vm.engine.ready.collectAsStateWithLifecycle()
    val playing by vm.engine.playing.collectAsStateWithLifecycle()
    val loading by vm.engine.loading.collectAsStateWithLifecycle()
    val error by vm.engine.lastError.collectAsStateWithLifecycle()
    val cpm by vm.cpm.collectAsStateWithLifecycle()
    val canUndo by vm.canUndo.collectAsStateWithLifecycle()
    val canRedo by vm.canRedo.collectAsStateWithLifecycle()
    val patternName by vm.currentName.collectAsStateWithLifecycle()
    var showLibrary by rememberSaveable { mutableStateOf(false) }
    var showHelp by rememberSaveable { mutableStateOf(false) }
    var showShare by rememberSaveable { mutableStateOf(false) }
    var showFeedback by rememberSaveable { mutableStateOf(false) }
    var showAbout by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(error) {
        if (error != null) {
            delay(4000)
            vm.engine.clearError()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        // The engine's WebView: attached, invisible, 1dp. Must stay in the tree.
        StrudelWebViewHost(vm.engine, Modifier.align(Alignment.TopStart))

        Column(Modifier.fillMaxSize()) {
            TransportBar(
                ready = ready,
                playing = playing,
                loading = loading,
                cpm = cpm,
                patternName = patternName,
                onLibrary = { showLibrary = true },
                onHelp = { showHelp = true },
                onShare = { showShare = true },
                onFeedback = { showFeedback = true },
                onAbout = { showAbout = true },
                canUndo = canUndo,
                canRedo = canRedo,
                onUndo = vm::undo,
                onRedo = vm::redo,
                onTogglePlay = vm::togglePlay,
                onHush = vm::hush,
                onCpm = vm::setCpm,
                onReset = vm::reset,
            )
            HorizontalDivider()

            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                ChainEditor(
                    chain = root,
                    parentGroupId = null,
                    siblingIndex = 0,
                    siblingCount = 1,
                    selection = selection,
                    active = active,
                    vm = vm,
                )
            }

            HorizontalDivider()
            BottomPanel(panel, vm)
        }

        if (showLibrary) {
            LibrarySheet(vm, onDismiss = { showLibrary = false })
        }
        if (showHelp) {
            HelpSheet(onDismiss = { showHelp = false })
        }
        if (showShare) {
            ShareSheet(vm, onDismiss = { showShare = false })
        }
        if (showFeedback) {
            FeedbackSheet(vm, onDismiss = { showFeedback = false })
        }
        if (showAbout) {
            AboutSheet(onDismiss = { showAbout = false }, onFeedback = { showAbout = false; showFeedback = true })
        }

        if (error != null) {
            Snackbar(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) { Text(error ?: "", maxLines = 3) }
        }
    }
}

private const val PanelCollapsed = 0f
// Fractions of the screen height, so "full" still leaves the editor visible.
private const val PanelHalfFraction = 0.32f
private const val PanelFullFraction = 0.62f

/**
 * The tool panel. Its height is user-controlled: drag the tab row to resize,
 * tap the chevron (or the active tab) to open/close.
 */
@Composable
private fun BottomPanel(panel: Panel, vm: EditorViewModel, modifier: Modifier = Modifier) {
    val screenH = LocalConfiguration.current.screenHeightDp.toFloat()
    val half = screenH * PanelHalfFraction
    val full = screenH * PanelFullFraction
    var target by rememberSaveable { mutableFloatStateOf(half) }
    val density = LocalDensity.current
    val height by animateDpAsState(target.dp, label = "panelHeight")
    val collapsed = target <= PanelCollapsed + 1f

    // Chevron / active-tab tap: open ↔ closed. Any height up to `full` is
    // reachable by dragging the tab row.
    fun cycle() {
        target = if (collapsed) half else PanelCollapsed
    }

    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        val dp = with(density) { delta.toDp().value }
                        target = (target - dp).coerceIn(PanelCollapsed, full)
                    },
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TabRow(selectedTabIndex = panel.ordinal, modifier = Modifier.weight(1f)) {
                PanelTab(Panel.NOTES, panel, collapsed, vm, ::cycle) { Icon(Icons.Default.Piano, null) }
                PanelTab(Panel.SOUNDS, panel, collapsed, vm, ::cycle) { Icon(Icons.Default.GraphicEq, null) }
                PanelTab(Panel.PARAMS, panel, collapsed, vm, ::cycle) { Icon(Icons.Default.Tune, null) }
                PanelTab(Panel.CODE, panel, collapsed, vm, ::cycle) { Icon(Icons.Default.Code, null) }
            }
            IconButton(onClick = ::cycle) {
                Icon(
                    if (collapsed) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    "Resize panel",
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(height)) {
            when (panel) {
                Panel.NOTES -> NotesPanel(vm)
                Panel.SOUNDS -> SoundBrowser(vm)
                Panel.PARAMS -> ParamsPanel(vm)
                Panel.CODE -> CodePanel(vm)
            }
        }
    }
}

@Composable
private fun PanelTab(
    target: Panel,
    current: Panel,
    collapsed: Boolean,
    vm: EditorViewModel,
    cycle: () -> Unit,
    icon: @Composable () -> Unit,
) {
    Tab(
        selected = current == target,
        onClick = {
            // Tapping the active tab toggles the panel; a collapsed panel opens on any tab.
            if (current == target || collapsed) cycle()
            vm.setPanel(target)
        },
        icon = icon,
        text = { Text(target.label, style = MaterialTheme.typography.labelSmall, maxLines = 1) },
    )
}
