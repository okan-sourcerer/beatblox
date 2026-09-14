package com.okan.strudelmobile.ui

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

/**
 * A plain Column whose items can be dragged by a handle to reorder them.
 *
 * Deliberately simple: while an item is dragged it follows the finger via a
 * translation, and once it has moved past half of its neighbour we ask the
 * owner to swap the two ([onMove]) and subtract the neighbour's height from
 * the translation so the item stays under the finger after the list
 * recomposes in its new order. Works with variable-height items and inside a
 * scrolling parent, because the handle consumes the drag.
 *
 * [itemContent] receives the modifier to put on the drag handle.
 */
@Composable
fun <T> ReorderableColumn(
    items: List<T>,
    key: (T) -> String,
    onMove: (id: String, delta: Int) -> Unit,
    modifier: Modifier = Modifier,
    spacing: Dp = 0.dp,
    itemContent: @Composable (item: T, handle: Modifier, dragging: Boolean) -> Unit,
) {
    val state = remember { ReorderState() }
    val ids = items.map(key)
    // The drag lambda below is keyed on the item id and so survives reorders;
    // it must read the *current* order, not the one it was created with.
    state.ids = ids
    state.onMove = onMove

    Column(modifier.fillMaxWidth()) {
        items.forEachIndexed { index, item ->
            val id = ids[index]
            // key() moves the node with the item when the order changes, so an
            // in-flight drag stays attached to the item that started it.
            key(id) {
            val dragging = state.draggingId == id
            val handle = Modifier.pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { state.draggingId = id; state.offsetY = 0f },
                    onDragEnd = { state.draggingId = null; state.offsetY = 0f },
                    onDragCancel = { state.draggingId = null; state.offsetY = 0f },
                    onDrag = { change, drag ->
                        change.consume()
                        state.offsetY += drag.y
                        val order = state.ids
                        val i = order.indexOf(id)
                        val below = order.getOrNull(i + 1)?.let { state.heights[it] } ?: 0
                        val above = order.getOrNull(i - 1)?.let { state.heights[it] } ?: 0
                        if (below > 0 && state.offsetY > below / 2f) {
                            state.offsetY -= below
                            state.onMove(id, +1)
                        } else if (above > 0 && state.offsetY < -above / 2f) {
                            state.offsetY += above
                            state.onMove(id, -1)
                        }
                    },
                )
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    // Spacing is padding *inside* the measured box so neighbour
                    // heights include the gap and the swap math stays exact.
                    .padding(top = if (index > 0) spacing else 0.dp)
                    .onSizeChanged { state.heights[id] = it.height }
                    .zIndex(if (dragging) 1f else 0f)
                    .graphicsLayer {
                        translationY = if (dragging) state.offsetY else 0f
                        scaleX = if (dragging) 1.02f else 1f
                        scaleY = if (dragging) 1.02f else 1f
                    },
            ) {
                itemContent(item, handle, dragging)
            }
            }
        }
    }
}

private class ReorderState {
    var draggingId by mutableStateOf<String?>(null)
    var offsetY by mutableFloatStateOf(0f)
    val heights = HashMap<String, Int>()
    var ids: List<String> = emptyList()
    var onMove: (String, Int) -> Unit = { _, _ -> }
}
