// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import kotlinx.coroutines.Job
import dev.nikelauncher.ui.kit.motion.AppMotion
import dev.nikelauncher.ui.home.scroll.draggedOffset
import dev.nikelauncher.ui.home.scroll.settleTarget
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.nikelauncher.data.QuickLaunchRegion
import dev.nikelauncher.data.QuickLaunchSlot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The home screen's free vertical drag, with a spring at both ends, and the gestures that ride on
 * it: pulling down at the top peeks at or opens the shade, pulling up at the bottom opens the app
 * list, and a sideways swipe in the empty space launches a quick-launch app.
 */
@Stable
internal class HomeScroll(
    private val scope: CoroutineScope,
    actions: GestureActions,
    swipeUpOpensAppList: Boolean,
    private val peekPullPx: Float,
    private val deepPullPx: Float,
    private val quickLaunchPx: Float,
    var motion: FiniteAnimationSpec<Float>,
) {
    /** Replaced on every composition, so the scroll itself outlives changes to what the gestures do. */
    var actions: GestureActions = actions
    var swipeUpOpensAppList: Boolean = swipeUpOpensAppList
    var offsetY by mutableFloatStateOf(0f)
        private set
    private var settling: Job? = null
    private var dragging = false
    var viewportHeight by mutableIntStateOf(0)
    var contentHeight by mutableIntStateOf(0)

    private var rawPullDown by mutableFloatStateOf(0f)
    private var rawPullUp by mutableFloatStateOf(0f)
    private var pullActionFired = false
    private var peekFired = false

    val minOffset: Float get() = minOf(0f, (viewportHeight - contentHeight).toFloat())

    /** Drag on the whole screen; off while editing, where blocks and rows take the finger. */
    @Composable
    fun dragModifier(enabled: Boolean): Modifier = Modifier.draggable(
        orientation = Orientation.Vertical,
        enabled = enabled,
        onDragStarted = { beginDrag() },
        state = rememberDraggableState { delta -> onDrag(delta) },
        onDragStopped = { velocity -> finishDrag(velocity) },
    )

    internal fun beginDrag() {
        settling?.cancel()
        dragging = true
        rawPullDown = 0f
        rawPullUp = 0f
        pullActionFired = false
        peekFired = false
    }

    internal fun onDrag(delta: Float) {
        val before = offsetY
        if (rawPullDown > 0f || before >= 0f) {
            rawPullDown = (rawPullDown + delta).coerceAtLeast(0f)
            if (!pullActionFired && rawPullDown >= deepPullPx) {
                pullActionFired = true
                actions.onExpandShade()
            } else if (!pullActionFired && !peekFired && rawPullDown >= peekPullPx) {
                peekFired = true
                actions.onPeekStatusBar()
            }
        }
        if (swipeUpOpensAppList && (rawPullUp > 0f || before <= minOffset)) {
            rawPullUp = (rawPullUp - delta).coerceAtLeast(0f)
            if (rawPullUp > 0f) actions.onSwipeUpDrag(rawPullUp, -delta)
        }
        // Apply the delta synchronously: one launched snapTo per event could cancel the rebound.
        offsetY = draggedOffset(before, delta, minOffset, maxOf(peekPullPx, viewportHeight * .18f))
    }

    internal fun finishDrag(velocity: Float) {
        dragging = false
        if (!pullActionFired && rawPullDown >= peekPullPx && velocity > 2200f) actions.onExpandShade()
        if (swipeUpOpensAppList && rawPullUp > 0f) actions.onSwipeUpEnd(velocity)
        rawPullDown = 0f
        rawPullUp = 0f
        val target = settleTarget(offsetY, minOffset,
            exponentialDecay<Float>(frictionMultiplier = 1.6f).calculateTargetValue(offsetY, velocity))
        settleAt(target, velocity)
    }

    private fun settleAt(target: Float, velocity: Float = 0f) {
        settling?.cancel()
        settling = scope.launch {
            animate(offsetY, target, initialVelocity = velocity, animationSpec = motion) { value, _ -> offsetY = value }
            offsetY = target.coerceIn(minOffset, 0f)
        }
    }

    fun boundsChanged() {
        if (!dragging && (offsetY < minOffset || offsetY > 0f)) settleAt(offsetY.coerceIn(minOffset, 0f))
    }

    fun reset() {
        dragging = false
        rawPullUp = 0f
        rawPullDown = 0f
        settleAt(0f)
    }

    /** Sideways swipes in the empty space under the stack, when anything is bound to them. */
    fun quickLaunchModifier(enabled: Boolean, editMode: Boolean, region: QuickLaunchRegion): Modifier =
        if (!enabled) Modifier else Modifier.pointerInput(editMode, region) {
            var eligible = false
            var travelled = 0f
            detectHorizontalDragGestures(
                onDragStart = { start ->
                    // A band of the screen, whatever the favorites are doing: where the swipe
                    // works should not depend on how many there are.
                    eligible = !editMode && region.contains(start.y, size.height.toFloat())
                    travelled = 0f
                },
                onDragEnd = {
                    if (eligible && abs(travelled) > quickLaunchPx) {
                        actions.onQuickLaunch(if (travelled < 0f) QuickLaunchSlot.LEFT else QuickLaunchSlot.RIGHT)
                    }
                    eligible = false
                },
                onDragCancel = { eligible = false },
            ) { change, dx ->
                if (eligible) {
                    change.consume()
                    travelled += dx
                }
            }
        }

    /** The stack, unbounded in height so it can be taller than the screen, and shifted by the drag. */
    fun contentModifier(): Modifier = Modifier
        .wrapContentHeight(align = Alignment.Top, unbounded = true)
        .offset { IntOffset(0, offsetY.roundToInt()) }

    /** Back to the top, for when HOME is pressed on a home screen already showing. */
    fun scrollToTop() = reset()

    /** Reveal real overflow; short stacks always have a single resting position. */
    fun revealBelow(overflow: Float, topInView: Float) {
        if (overflow <= 0f) return
        settleAt((offsetY - minOf(overflow, topInView)).coerceIn(minOffset, 0f))
    }

}

@Composable
internal fun rememberHomeScroll(actions: GestureActions, swipeUpOpensAppList: Boolean, homeIntentTick: Int): HomeScroll {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val motion = AppMotion.spatial<Float>()
    // Never keyed. A scroll made anew keeps the root's size only if the root is resized afterwards,
    // so one recreated on a settings change or on entering edit mode had a viewport of zero and put
    // every block at the top until something resized the screen. Whatever can change is copied in.
    val scroll = remember {
        HomeScroll(
            scope = scope,
            actions = actions,
            swipeUpOpensAppList = swipeUpOpensAppList,
            peekPullPx = with(density) { 30.dp.toPx() },
            deepPullPx = with(density) { 120.dp.toPx() },
            quickLaunchPx = with(density) { 80.dp.toPx() },
            motion = motion,
        )
    }
    SideEffect {
        scroll.motion = motion
        scroll.actions = actions
        scroll.swipeUpOpensAppList = swipeUpOpensAppList
    }
    LaunchedEffect(homeIntentTick) { if (homeIntentTick > 0) scroll.scrollToTop() }
    // Turning the phone changes how much there is to scroll through: a stack dragged up in
    // landscape kept that offset when the screen went tall again, which left it above the top.
    LaunchedEffect(scroll.minOffset) { scroll.boundsChanged() }
    return scroll
}
