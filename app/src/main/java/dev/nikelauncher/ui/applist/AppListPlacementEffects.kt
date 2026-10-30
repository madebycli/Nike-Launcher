// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.applist

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first

/** Scrub placement, padding retirement and held-letter walking. */
@Composable
internal fun AppListSession.ObservePlacement() {
    LaunchedEffect(bottomRoomLive) {
        if (!bottomRoomLive) return@LaunchedEffect
        snapshotFlow {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()
            val slack = (sectionTopPx - idleTopPaddingPx).coerceAtLeast(0)
            last != null &&
                (last.index < info.totalItemsCount - 1 ||
                    last.offset + last.size >= info.viewportEndOffset + slack)
        }
            .dropWhile { it }
            .first { it }
        bottomRoomLive = false
    }

    LaunchedEffect(userDragged, scrubLetter, bottomRoomLive) {
        if (!userDragged || scrubLetter != null || highlightRange.isEmpty() || bottomRoomLive) {
            return@LaunchedEffect
        }
        snapshotFlow { placementSettled() }.first { it }
        if (highlightRange.isEmpty()) return@LaunchedEffect

        val shrinkBy = (sectionTopPx - idleTopPaddingPx).toFloat()
        val compensate = minOf(shrinkBy, forwardRoom()).coerceAtLeast(0f)
        highlightRange = IntRange.EMPTY
        if (compensate > 0f) listState.dispatchRawDelta(-compensate)
    }

    LaunchedEffect(scrubRowIndex, scrubPlacement, displayModel) {
        if (scrubRowIndex < 0) return@LaunchedEffect
        val end =
            displayModel.letterIndex.firstOrNull { it.second > scrubRowIndex }?.second
                ?: displayModel.rows.size

        if (lastLetterToLine && !bottomRoomLive) {
            bottomRoomLive = true
            withFrameNanos {}
        }

        listState.scrollToItem(scrubRowIndex)
        highlightRange = scrubRowIndex until end
        gapCeilingPx = Float.MAX_VALUE
        blankCeilingPx = Float.MAX_VALUE
        if (bottomRoomLive) {
            withFrameNanos {}
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()
            val blank =
                if (last != null && last.index == info.totalItemsCount - 1) {
                    (info.viewportEndOffset - (last.offset + last.size)).toFloat()
                } else {
                    0f
                }
            if (blank > 0f) blankCeilingPx = blank else bottomRoomLive = false
        }
        userDragged = false
    }

    val holdScrolling = scrub.holdScrolling
    LaunchedEffect(holdScrolling, holdScrollSpeed) {
        if (!holdScrolling) return@LaunchedEffect
        val rowPx =
            listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index > 0 }?.size?.toFloat()
                ?: return@LaunchedEffect
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val seconds = (now - last) / 1_000_000_000f
            last = now
            val delta = rowPx * holdScrollSpeed * seconds
            if (delta > 0f) listState.dispatchRawDelta(delta)
        }
    }
}
