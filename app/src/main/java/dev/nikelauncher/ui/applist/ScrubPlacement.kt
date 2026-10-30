// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.applist

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first

/**
 * Where a scrubbed letter is parked, and the room the list holds open to park it.
 *
 * The letter's section is placed on a line down the screen, so there has to be blank space above
 * the first row and below the last for every letter to reach it. That room is ordinary scrollable
 * space, so it is given back by ratchets: once the letter has been carried away from the line the
 * room it came from is not offered again until a letter is placed again.
 *
 * Inputs are read through lambdas because the numbers behind them change with every composition.
 */
@Stable
internal class ScrubPlacement(
    private val listState: LazyListState,
    private val rowCount: () -> Int,
    private val sectionTopPx: () -> Int,
    private val idleTopPaddingPx: () -> Int,
    private val idleBottomPaddingPx: () -> Int,
    private val viewportHeightPx: () -> Int,
) {
    /** Row indices of the highlighted section. Applied after the scroll lands, or the new letter lights up a frame early. */
    var highlightRange by mutableStateOf(IntRange.EMPTY)

    /**
     * The most of the placement gap above the first row that may still be shown, which only ever
     * falls. Enforced by refusing the scroll rather than by shrinking the padding: shrinking it
     * moves the content by the same amount the finger just did, and the two add up.
     */
    var gapCeilingPx by mutableFloatStateOf(Float.MAX_VALUE)

    /**
     * The most blank that may still be shown under the last row, which only ever falls. The blank
     * grows when you scroll forward and shrinks when you scroll back toward the apps, so refusing
     * it to grow stops the last letter travelling up past the line and leaves scrolling back free.
     */
    var blankCeilingPx by mutableFloatStateOf(Float.MAX_VALUE)

    /** Whether the room under the last app is still being held for a placement. */
    var bottomRoomLive by mutableStateOf(false)

    /** A finger has moved the list since the letter was placed. */
    var userDragged by mutableStateOf(false)

    /**
     * How much of the scrub placement padding is still sitting on screen at the top, over and above
     * the gap the list idles with. Positive means that end has further to travel.
     */
    fun topGapRemaining(): Float {
        val info = listState.layoutInfo
        val first = info.visibleItemsInfo.firstOrNull() ?: return 0f
        // Not the first row at the top means the gap is long gone above us.
        if (first.index > 0) return 0f
        return (first.offset - info.viewportStartOffset).toFloat() - idleTopPaddingPx()
    }

    /** How much further the list could still scroll forward: large when the end is not on screen, zero when parked against it. */
    fun forwardRoom(): Float {
        val info = listState.layoutInfo
        val last = info.visibleItemsInfo.lastOrNull() ?: return Float.MAX_VALUE
        if (last.index < rowCount()) return Float.MAX_VALUE
        val bottom = (last.offset - info.viewportStartOffset + last.size).toFloat()
        return bottom + idleBottomPaddingPx() - viewportHeightPx()
    }

    /**
     * Whether the placement padding can be retired without anything appearing to move. A list short
     * enough to show both ends at once can never clear either gap by scrolling, so it is let through.
     */
    fun placementSettled(): Boolean {
        val items = listState.layoutInfo.visibleItemsInfo
        val first = items.firstOrNull() ?: return true
        if (first.index == 0 && items.last().index >= rowCount()) return true
        return topGapRemaining() <= 0f
    }

    /** The settings shortcut etc. below the last letter: how much blank there is under the last row, if it is the last. */
    fun blankUnderLastRow(): Float? {
        val info = listState.layoutInfo
        val last = info.visibleItemsInfo.lastOrNull() ?: return null
        if (last.index != info.totalItemsCount - 1) return null
        return (info.viewportEndOffset - (last.offset + last.size)).toFloat()
    }
}

/** Builds the [ScrubPlacement] and runs everything that keeps it in step with the scrub. */
@Composable
internal fun rememberScrubPlacement(
    listState: LazyListState,
    scrub: ScrubState,
    model: AppListModel,
    lastLetterToLine: Boolean,
    holdScrollSpeed: Int,
    sectionTopPx: Int,
    idleTopPaddingPx: Int,
    idleBottomPaddingPx: Int,
    viewportHeightPx: Int,
): ScrubPlacement {
    val rowCount by rememberUpdatedState(model.rows.size)
    val sectionTop by rememberUpdatedState(sectionTopPx)
    val idleTop by rememberUpdatedState(idleTopPaddingPx)
    val idleBottom by rememberUpdatedState(idleBottomPaddingPx)
    val viewport by rememberUpdatedState(viewportHeightPx)
    val placement = remember(listState) {
        ScrubPlacement(listState, { rowCount }, { sectionTop }, { idleTop }, { idleBottom }, { viewport })
    }

    val scrubLetter = scrub.letter
    val scrubPlacement = scrub.placement
    // From the placement and not from the live letter: the letter is gone by the time a quick tap
    // has composed anything, and the list is placed by what was asked for, not by what is still
    // under a finger.
    val scrubRowIndex = remember(model, scrubPlacement) {
        val letter = scrubPlacement?.letter ?: return@remember -1
        model.letterIndex.firstOrNull { it.first == letter }?.second ?: -1
    }

    // Taken away the moment the last row leaves the screen, which is the whole of the ratchet at
    // this end. Invisible when it happens: what is removed is below the viewport.
    LaunchedEffect(placement.bottomRoomLive) {
        if (!placement.bottomRoomLive) return@LaunchedEffect
        // Once the last row's bottom has reached the bottom of the screen, every pixel of the room
        // is below the fold and taking it away moves nothing. Asking where the row's bottom edge is
        // asks the right question: "the last row has left the viewport" never came true, because the
        // room keeps that row inside the measured viewport for as long as it is held.
        snapshotFlow {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()
            // Far enough past the fold to cover the gap above the first row as well, since that one
            // is let go at the same moment and the list has to be able to absorb it.
            val slack = (sectionTop - idleTop).coerceAtLeast(0)
            last != null && (
                // Scrolled back far enough that the final row is not even on screen.
                last.index < info.totalItemsCount - 1 ||
                    // Or it is, and there is room under it for both to go at once.
                    last.offset + last.size >= info.viewportEndOffset + slack
                )
        }
            // The layout has not caught up at the moment this starts: the scroll to the letter is
            // still in flight, so the first reading says the room is already spent. Waiting for it
            // to be false once means waiting for the letter to actually arrive.
            .dropWhile { it }
            .first { it }
        placement.bottomRoomLive = false
    }

    LaunchedEffect(placement.userDragged, scrubLetter, placement.bottomRoomLive) {
        // Not while the room under the last row is still held. Retiring the gap above the first
        // row shortens the list by its height, and the compensation for that is capped by how much
        // room is left below, which the held room has already spoken for. Both are let go together
        // instead, once the list is back among the apps and there is room to absorb it.
        if (!placement.userDragged || scrubLetter != null || placement.highlightRange.isEmpty() || placement.bottomRoomLive) {
            return@LaunchedEffect
        }
        snapshotFlow { placement.placementSettled() }.first { it }
        if (placement.highlightRange.isEmpty()) return@LaunchedEffect

        // The top padding falls from the scrub line back to the idle gap, so that is exactly how
        // far the content would rise. Except against the bottom, where shrinking the padding
        // shortens the scroll range by the same amount and the clamp slides us back by whatever no
        // longer fits, so only what the clamp cannot absorb is asked for.
        val shrinkBy = (sectionTop - idleTop).toFloat()
        val compensate = minOf(shrinkBy, placement.forwardRoom()).coerceAtLeast(0f)
        placement.highlightRange = IntRange.EMPTY
        // dispatchRawDelta rather than scrollBy: the drag that got us here holds the scroll mutex at
        // UserInput priority, and a scrollBy would just be canceled by it.
        if (compensate > 0f) listState.dispatchRawDelta(-compensate)
    }

    LaunchedEffect(scrubRowIndex, scrubPlacement, model) {
        if (scrubRowIndex < 0) return@LaunchedEffect
        // The next letter's header ends this section. Walking the rows to find it copied the whole
        // tail of the list on every one of the ~26 letter changes in a gesture.
        val end = model.letterIndex.firstOrNull { it.second > scrubRowIndex }?.second ?: model.rows.size

        // Room before the scroll, and a frame for it to land in: the scroll clamps against the range
        // as it stands when it runs. Offered to every placement rather than only the last section;
        // whether a letter needs any is whether the apps below it are enough to push it up to the
        // line, and the last several letters of an alphabet are never enough between them.
        if (lastLetterToLine && !placement.bottomRoomLive) {
            placement.bottomRoomLive = true
            withFrameNanos { }
        }

        listState.scrollToItem(scrubRowIndex)
        placement.highlightRange = scrubRowIndex until end
        // A fresh placement opens the room again; from here the ratchet only closes it.
        placement.gapCeilingPx = Float.MAX_VALUE
        placement.blankCeilingPx = Float.MAX_VALUE
        if (placement.bottomRoomLive) {
            // Now ask what the room was actually worth, once the scroll has landed. Blank left under
            // the last row means this letter leant on it and the ratchet has something to hold; none
            // means it reached the line on its own apps, and the room goes back before it can add a
            // screen of empty scrolling to the end of the list. Measured here and not left to the
            // first drag, whose reading arrives before the list has settled.
            withFrameNanos { }
            val blank = placement.blankUnderLastRow() ?: 0f
            if (blank > 0f) placement.blankCeilingPx = blank else placement.bottomRoomLive = false
        }
        // This placement is fresh, so the next drag is the one that retires it.
        placement.userDragged = false
    }

    // Walking on through a letter while the finger rests on it. A row at a time rather than a
    // fling, because the point is to read what goes past.
    val holdScrolling = scrub.holdScrolling
    LaunchedEffect(holdScrolling, holdScrollSpeed) {
        if (!holdScrolling) return@LaunchedEffect
        val rowPx = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index > 0 }?.size?.toFloat() ?: return@LaunchedEffect
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val seconds = (now - last) / 1_000_000_000f
            last = now
            val delta = rowPx * holdScrollSpeed * seconds
            // dispatchRawDelta because a scrollBy takes the scroll mutex, and anything else that
            // wants it (the placement scroll, an overscroll settling) would cancel this one and the
            // cancellation would stop the walk for good, a letter or two in.
            if (delta > 0f) listState.dispatchRawDelta(delta)
        }
    }
    return placement
}
