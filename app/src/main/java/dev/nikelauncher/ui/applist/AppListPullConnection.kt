// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.applist

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlin.math.abs
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Pull-to-collapse, edge elasticity and placement ratchets. */
internal fun appListConnection(session: AppListSession): NestedScrollConnection =
    with(session) {
        object : NestedScrollConnection {

            private var dragging = false

            private var topPullEligible = false
            private var bottomPullEligible = false

            private var pullArmed = false

            private var scrolledInGesture = 0f

            private var settleJob: Job? = null

            private fun stretch(delta: Float) {
                val resistance = 1f - (abs(stretchPx) / maxStretchPx).coerceIn(0f, 0.9f)
                stretchPx = (stretchPx + delta * resistance).coerceIn(-maxStretchPx, maxStretchPx)
            }

            private fun startSettle(velocity: Float) {
                settleJob?.cancel()
                settleJob = scope.launch { settleStretch(velocity) }
            }

            private fun takeOverSettle() {
                settleJob?.cancel()
                settleJob = null
                if (stretchSettling) {
                    stretchPx = stretchAnim.value.coerceIn(-maxStretchPx, maxStretchPx)
                    stretchSettling = false
                }
            }

            private suspend fun settleStretch(velocity: Float) {
                val headroom = (1f - abs(stretchPx) / maxStretchPx).coerceIn(0f, 1f)
                stretchSettling = true
                try {
                    stretchAnim.snapTo(stretchPx.coerceIn(-maxStretchPx, maxStretchPx))
                    stretchPx = 0f
                    stretchAnim.animateTo(
                        targetValue = 0f,
                        animationSpec =
                            spring(
                                dampingRatio = EDGE_STRETCH_DAMPING,
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                        initialVelocity =
                            (velocity * headroom).coerceIn(
                                -maxStretchPx * 12f,
                                maxStretchPx * 12f,
                            ),
                    )
                } finally {
                    stretchPx = stretchAnim.value.coerceIn(-maxStretchPx, maxStretchPx)
                    stretchSettling = false
                }
            }

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.Drag && !dragging) {
                    dragging = true
                    takeOverSettle()
                    userDragged = true
                    focusManager.clearFocus()
                    topPullEligible = !listState.canScrollBackward
                    bottomPullEligible = !listState.canScrollForward
                    pullArmed = false
                    scrolledInGesture = 0f
                }
                if (collapsing || stretchSettling) return Offset.Zero
                if (source == NestedScrollSource.Drag || source == NestedScrollSource.Fling) {
                    val info = listState.layoutInfo
                    val first = info.visibleItemsInfo.firstOrNull()
                    if (first != null && first.index == 0 && !highlightRange.isEmpty()) {
                        val gap = (first.offset - info.viewportStartOffset).toFloat()
                        if (gap < gapCeilingPx) gapCeilingPx = gap
                        if (available.y > 0f) {
                            val room = (gapCeilingPx - gap).coerceAtLeast(0f)
                            if (available.y > room) {
                                val excess = available.y - room
                                val resistance =
                                    1f - (abs(overPull) / maxPullPx).coerceIn(0f, 0.75f)
                                overPull =
                                    (overPull + excess * resistance).coerceIn(-maxPullPx, maxPullPx)
                                return available
                            }
                        }
                    }
                    val last = info.visibleItemsInfo.lastOrNull()
                    if (bottomRoomLive && last != null && last.index == info.totalItemsCount - 1) {
                        val blank = (info.viewportEndOffset - (last.offset + last.size)).toFloat()
                        if (blank < blankCeilingPx) blankCeilingPx = blank
                        if (available.y < 0f) {
                            val room = (blankCeilingPx - blank).coerceAtLeast(0f)
                            if (-available.y > room) {
                                val excess = available.y + room
                                val resistance =
                                    1f - (abs(overPull) / maxPullPx).coerceIn(0f, 0.75f)
                                overPull =
                                    (overPull + excess * resistance).coerceIn(-maxPullPx, maxPullPx)
                                return available
                            }
                        }
                    }
                }
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

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (collapsing || stretchSettling || source != NestedScrollSource.Drag) {
                    return Offset.Zero
                }
                scrolledInGesture += abs(consumed.y)
                if (available.y == 0f) return Offset.Zero
                if (scrolledInGesture <= nearEndPx || abs(available.y) <= fastDragPx)
                    pullArmed = true
                val startedAtThisEnd = if (available.y > 0f) topPullEligible else bottomPullEligible
                val pulling = startedAtThisEnd || pullArmed
                if (pulling) {
                    val resistance = 1f - (abs(overPull) / maxPullPx).coerceIn(0f, 0.75f)
                    overPull = (overPull + available.y * resistance).coerceIn(-maxPullPx, maxPullPx)
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
                    val flungAway = available.y * away > 800f
                    val dismissing =
                        abs(pulled) > dismissCommitPx ||
                            (flungAway && abs(pulled) > dismissCommitPx * 0.4f)
                    collapsing = true
                    try {
                        collapseAnim.snapTo(pulled)
                        overPull = 0f
                        if (dismissing) {
                            collapseAnim.animateTo(
                                targetValue = viewportHeightPx.toFloat() * away,
                                animationSpec = tween(240, easing = FastOutLinearInEasing),
                            )
                            currentDismiss()
                        } else {
                            collapseAnim.animateTo(
                                targetValue = 0f,
                                animationSpec =
                                    spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMedium,
                                    ),
                            )
                        }
                    } finally {
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
