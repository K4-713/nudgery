// SPDX-License-Identifier: CC0-1.0

package com.nudgery.android.ui.screen

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * Returns a copy of this list with the element at [from] moved to index [to], shifting the rest.
 * Pure and total — out-of-range or no-op moves return the list unchanged. Covered by tests; this is
 * the in-memory reorder the drag applies optimistically before it is persisted (ED-19).
 */
internal fun <T> List<T>.moveItem(from: Int, to: Int): List<T> {
    if (from == to || from !in indices || to !in indices) return this
    return toMutableList().apply { add(to, removeAt(from)) }
}

/**
 * Drives long-press drag-to-reorder for a [LazyListState] (ED-19, Phase 2). It tracks which item is
 * lifted and how far it has been dragged, reorders the backing list optimistically via [onMove] as
 * the lifted item's midpoint crosses its neighbors, and signals edge auto-scroll through a channel.
 * The visual lift (accent wash, shadow, landing-gap outline) is applied by the caller using
 * [draggingItemIndex] and [draggingItemOffset]; persistence happens on drop in the caller.
 *
 * Adapted from the canonical Compose reorderable-list pattern so we avoid a third-party dependency.
 */
internal class NudgeDragDropState(
    private val state: LazyListState,
    private val scope: CoroutineScope,
    private val onMove: (from: Int, to: Int) -> Unit,
) {
    var draggingItemIndex by mutableStateOf<Int?>(null)
        private set

    internal val scrollChannel = Channel<Float>()

    private var draggingItemDraggedDelta by mutableFloatStateOf(0f)
    private var draggingItemInitialOffset by mutableIntStateOf(0)

    /** Visual translation for the lifted item that keeps it pinned under the finger across reorders. */
    internal val draggingItemOffset: Float
        get() = draggingItemLayoutInfo?.let { item ->
            draggingItemInitialOffset + draggingItemDraggedDelta - item.offset
        } ?: 0f

    private val draggingItemLayoutInfo: LazyListItemInfo?
        get() = state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == draggingItemIndex }

    internal fun onDragStart(offset: Offset) {
        state.layoutInfo.visibleItemsInfo
            .firstOrNull { item -> offset.y.toInt() in item.offset..(item.offset + item.size) }
            ?.also {
                draggingItemIndex = it.index
                draggingItemInitialOffset = it.offset
            }
    }

    internal fun onDragInterrupted() {
        draggingItemIndex = null
        draggingItemDraggedDelta = 0f
        draggingItemInitialOffset = 0
    }

    /** @return true when a reorder happened this frame, so the caller can fire a haptic tick. */
    internal fun onDrag(offset: Offset): Boolean {
        draggingItemDraggedDelta += offset.y
        val draggingItem = draggingItemLayoutInfo ?: return false
        val startOffset = draggingItem.offset + draggingItemOffset
        val endOffset = startOffset + draggingItem.size
        val middleOffset = startOffset + draggingItem.size / 2f

        val targetItem = state.layoutInfo.visibleItemsInfo.find { item ->
            middleOffset.toInt() in item.offset..(item.offset + item.size) &&
                draggingItem.index != item.index
        }

        if (targetItem != null) {
            onMove(draggingItem.index, targetItem.index)
            draggingItemIndex = targetItem.index
            return true
        }

        // No reorder target: if the lifted item is pushed past a viewport edge, request auto-scroll.
        val overscroll = when {
            draggingItemDraggedDelta > 0 ->
                (endOffset - state.layoutInfo.viewportEndOffset).coerceAtLeast(0f)
            draggingItemDraggedDelta < 0 ->
                (startOffset - state.layoutInfo.viewportStartOffset).coerceAtMost(0f)
            else -> 0f
        }
        if (overscroll != 0f) scrollChannel.trySend(overscroll)
        return false
    }
}

/** Remembers a [NudgeDragDropState] and pumps its auto-scroll requests into the list. */
@Composable
internal fun rememberNudgeDragDropState(
    lazyListState: LazyListState,
    onMove: (from: Int, to: Int) -> Unit,
): NudgeDragDropState {
    val scope = rememberCoroutineScope()
    val state = remember(lazyListState) { NudgeDragDropState(lazyListState, scope, onMove) }
    LaunchedEffect(state) {
        while (true) {
            val diff = state.scrollChannel.receive()
            lazyListState.scrollBy(diff)
        }
    }
    return state
}
