// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.applist

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

/** Damping for the edge elastic. Under 1 so a fling into an end overshoots and comes back once. */
private const val EDGE_STRETCH_DAMPING = 0.55f

/** The distances the pulls are measured in, resolved to pixels once. */
internal class PullDistances(
    /** The travel a collapse is drawn over. */
    val dismissPx: Float,
    val maxPullPx: Float,
    val maxStretchPx: Float,
    /** Per-frame drag big enough to call a flick rather than a deliberate push. */
    val fastDragPx: Float,
    /** How far a gesture may have scrolled the list and still count as starting at the end. */
    val nearEndPx: Float,
    /** How much of the collapse travel commits it. */
    val commitPx: Float,
)

/**
 * Pull-to-collapse from either end of the list, and the elastic at both ends.
 *
 * Done the way pull-to-refresh is done: one nested-scroll connection that actually consumes the
 * drag. Held signed throughout, positive pulled down off the top and negative pulled up off the
 * bottom, so one gesture serves both ends. Consuming means the list cannot scroll while a pull is
 * outstanding and winding back spends the pull before the list moves again, so the gesture is
 * always in exactly one state.
 *
 * A drag past an end stretches it, a fling into an end seeds the spring with the leftover velocity,
 * and it always springs back to rest.
 */
@Stable
internal class ListPull(
    private val scope: CoroutineScope,
    private val listState: LazyListState,
    private val placement: ScrubPlacement,
    private val focusManager: FocusManager,
    private val distances: PullDistances,
    private val viewportHeightPx: () -> Int,
    private val onDismiss: () -> Unit,
) {
    /** Signed: positive pulled down off the top of the list, negative up off the bottom. */
    var overPull by mutableFloatStateOf(0f)
    val collapseAnim = Animatable(0f)
    var collapsing by mutableStateOf(false)

    var stretchPx by mutableFloatStateOf(0f)
    val stretchAnim = Animatable(0f)
    var stretchSettling by mutableStateOf(false)

    /** The collapse transform input. Entering and collapsing are the same transform in opposite directions. */
    fun collapseProvider(enterPullPx: Float): Float = when {
        collapsing -> collapseAnim.value
        enterPullPx > 0f -> enterPullPx
        else -> overPull
    }

    fun stretchProvider(): Float {
        val raw = if (stretchSettling) stretchAnim.value else stretchPx
        return raw.coerceIn(-distances.maxStretchPx, distances.maxStretchPx)
    }

    /** Everything the overlay left behind has to be cleared explicitly, because it stays composed while hidden. */
    fun reset() {
        overPull = 0f
        stretchPx = 0f
        collapsing = false
    }

    val connection = object : NestedScrollConnection {
        /** True between the first drag of a gesture and the fling that ends it. */
        private var dragging = false

        /** Whether the drag in progress began with the list already parked against that end. */
        private var topPullEligible = false
        private var bottomPullEligible = false

        /**
         * Whether this gesture may collapse the overlay: it arms if it barely scrolled the list at
         * all, or if it is travelling slowly by the time it gets there. A long fast flick satisfies
         * neither, which is what stops a flick slamming into the end and shrinking the list mid-scroll.
         */
        private var pullArmed = false

        /** How far this gesture has scrolled the list, to tell a pull from a flick. */
        private var scrolledInGesture = 0f

        /** The edge spring, held so a finger arriving mid-settle can take it over. */
        private var settleJob: Job? = null

        private fun stretch(delta: Float) {
            // Rubber band: the further it goes, the less each pixel counts.
            val resistance = 1f - (abs(stretchPx) / distances.maxStretchPx).coerceIn(0f, 0.9f)
            stretchPx = (stretchPx + delta * resistance).coerceIn(-distances.maxStretchPx, distances.maxStretchPx)
        }

        /**
         * Runs the edge spring without making the fling wait for it: awaited inside onPostFling it
         * held the connection in its settling state, and every guard below turns a drag away while
         * that is true, so the next pull was swallowed.
         */
        private fun startSettle(velocity: Float) {
            settleJob?.cancel()
            settleJob = scope.launch { settleStretch(velocity) }
        }

        /** Ends a settle in progress and hands its position back to the finger. */
        private fun takeOverSettle() {
            settleJob?.cancel()
            settleJob = null
            if (stretchSettling) {
                stretchPx = stretchAnim.value.coerceIn(-distances.maxStretchPx, distances.maxStretchPx)
                stretchSettling = false
            }
        }

        private suspend fun settleStretch(velocity: Float) {
            val headroom = (1f - abs(stretchPx) / distances.maxStretchPx).coerceIn(0f, 1f)
            stretchSettling = true
            try {
                stretchAnim.snapTo(stretchPx.coerceIn(-distances.maxStretchPx, distances.maxStretchPx))
                stretchPx = 0f
                stretchAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(dampingRatio = EDGE_STRETCH_DAMPING, stiffness = Spring.StiffnessMediumLow),
                    // Scaled by what headroom is left before the clamp: seeding a full fling on top
                    // of an already-stretched edge drives the spring past the maximum, and the draw
                    // clamps it there for a few frames, which reads as a hitch.
                    initialVelocity = (velocity * headroom).coerceIn(-distances.maxStretchPx * 12f, distances.maxStretchPx * 12f),
                )
            } finally {
                // Handing the live value back means an interrupted settle continues from where it
                // was instead of snapping flat.
                stretchPx = stretchAnim.value.coerceIn(-distances.maxStretchPx, distances.maxStretchPx)
                stretchSettling = false
            }
        }

        /** Refuses [available] against a ceiling, handing the excess to the pull rather than swallowing it. */
        private fun refuseToPull(excess: Float) {
            val resistance = 1f - (abs(overPull) / distances.maxPullPx).coerceIn(0f, 0.75f)
            overPull = (overPull + excess * resistance).coerceIn(-distances.maxPullPx, distances.maxPullPx)
        }

        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (source == NestedScrollSource.Drag && !dragging) {
                dragging = true
                // Cancellation is not instant, so the state is handed over here and now.
                takeOverSettle()
                // A finger on the list, as opposed to a programmatic scrub scroll.
                placement.userDragged = true
                // Scrolling means looking rather than typing, and the keyboard is covering half of
                // what is being looked at. It stays gone until the field is tapped again.
                focusManager.clearFocus()
                // Collapsing has to be a deliberate pull from rest: a scroll that merely arrives at
                // an end must not turn into one.
                topPullEligible = !listState.canScrollBackward
                bottomPullEligible = !listState.canScrollForward
                pullArmed = false
                scrolledInGesture = 0f
            }
            if (collapsing || stretchSettling) return Offset.Zero

            // The gap a placement opens above the first row closes and stays closed, and so does the
            // blank under the last row. A fling counts as much as a finger.
            if (source == NestedScrollSource.Drag || source == NestedScrollSource.Fling) {
                val info = listState.layoutInfo
                val first = info.visibleItemsInfo.firstOrNull()
                if (first != null && first.index == 0 && !placement.highlightRange.isEmpty()) {
                    val gap = (first.offset - info.viewportStartOffset).toFloat()
                    if (gap < placement.gapCeilingPx) placement.gapCeilingPx = gap
                    if (available.y > 0f) {
                        val room = (placement.gapCeilingPx - gap).coerceAtLeast(0f)
                        // Handed to the elastic: pulling on past a gap that will not open is the
                        // list being pulled off the top of itself.
                        if (available.y > room) {
                            refuseToPull(available.y - room)
                            return available
                        }
                    }
                }
                val last = info.visibleItemsInfo.lastOrNull()
                if (placement.bottomRoomLive && last != null && last.index == info.totalItemsCount - 1) {
                    val blank = (info.viewportEndOffset - (last.offset + last.size)).toFloat()
                    if (blank < placement.blankCeilingPx) placement.blankCeilingPx = blank
                    if (available.y < 0f) {
                        val room = (placement.blankCeilingPx - blank).coerceAtLeast(0f)
                        // Refused, but not swallowed: consuming it outright left nothing for the
                        // elastic to pick up, so the list could no longer be pulled off its end.
                        if (-available.y > room) {
                            refuseToPull(available.y + room)
                            return available
                        }
                    }
                }
            }
            // Spend whatever is outstanding before the list is allowed to move again, so winding a
            // gesture back never has the two running at once.
            if (overPull > 0f && available.y < 0f) {
                val used = maxOf(available.y, -overPull)
                overPull = (overPull + used).coerceAtLeast(0f)
                return Offset(0f, used)
            }
            if (overPull < 0f && available.y > 0f) {
                val used = minOf(available.y, -overPull)
                overPull = (overPull + used).coerceAtMost(0f)
                return Offset(0f, used)
            }
            if (stretchPx > 0f && available.y < 0f) {
                val used = maxOf(available.y, -stretchPx)
                stretchPx += used
                return Offset(0f, used)
            }
            if (stretchPx < 0f && available.y > 0f) {
                val used = minOf(available.y, -stretchPx)
                stretchPx += used
                return Offset(0f, used)
            }
            return Offset.Zero
        }

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            // Only a finger stretches an end; a fling that runs out of content is dealt with in
            // onPostFling, where its velocity is still known.
            if (collapsing || stretchSettling || source != NestedScrollSource.Drag) return Offset.Zero
            scrolledInGesture += abs(consumed.y)
            if (available.y == 0f) return Offset.Zero
            if (scrolledInGesture <= distances.nearEndPx || abs(available.y) <= distances.fastDragPx) pullArmed = true
            val startedAtThisEnd = if (available.y > 0f) topPullEligible else bottomPullEligible
            if (startedAtThisEnd || pullArmed) {
                // Signed: pulled down off the top is positive, pulled up off the bottom is negative.
                val resistance = 1f - (abs(overPull) / distances.maxPullPx).coerceIn(0f, 0.75f)
                overPull = (overPull + available.y * resistance).coerceIn(-distances.maxPullPx, distances.maxPullPx)
                return available
            }
            stretch(available.y)
            return available
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            dragging = false
            pullArmed = false
            scrolledInGesture = 0f
            if (collapsing || stretchSettling) return Velocity.Zero
            if (overPull != 0f) {
                val pulled = overPull
                val away = if (pulled > 0f) 1f else -1f
                // Flung on past the threshold counts even when the pull itself is short.
                val flungAway = available.y * away > 800f
                val dismissing = abs(pulled) > distances.commitPx || (flungAway && abs(pulled) > distances.commitPx * 0.4f)
                collapsing = true
                try {
                    collapseAnim.snapTo(pulled)
                    overPull = 0f
                    if (dismissing) {
                        collapseAnim.animateTo(
                            // Off whichever edge it was heading for.
                            targetValue = viewportHeightPx().toFloat() * away,
                            animationSpec = tween(240, easing = FastOutLinearInEasing),
                        )
                        onDismiss()
                    } else {
                        collapseAnim.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
                        )
                    }
                } finally {
                    // Also on the way out of a canceled fling, or the overlay stays parked halfway
                    // down the screen for good.
                    collapsing = false
                }
                return available
            }
            if (stretchPx != 0f) {
                startSettle(available.y)
                return available
            }
            return Velocity.Zero
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            if (collapsing || stretchSettling || available.y == 0f) return Velocity.Zero
            startSettle(available.y)
            return available
        }
    }
}
