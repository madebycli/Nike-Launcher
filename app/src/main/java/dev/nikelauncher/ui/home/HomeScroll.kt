// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import dev.nikelauncher.data.QuickLaunchSlot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/** How much of the bottom of the home screen always answers the quick-launch swipes. */
private const val QUICK_LAUNCH_BOTTOM_FRACTION = 0.2f

/**
 * The home screen's free vertical drag, with a spring at both ends, and the gestures that ride on
 * it: pulling down at the top peeks at or opens the shade, pulling up at the bottom opens the app
 * list, and a sideways swipe in the empty space launches a quick-launch app.
 */
@Stable
internal class HomeScroll(
    private val scope: CoroutineScope,
    private val actions: GestureActions,
    private val swipeUpOpensAppList: Boolean,
    private val peekPullPx: Float,
    private val deepPullPx: Float,
    private val quickLaunchPx: Float,
) {
    val offsetY = Animatable(0f)
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
        onDragStarted = {
            rawPullDown = 0f
            rawPullUp = 0f
            pullActionFired = false
            peekFired = false
        },
        state = rememberDraggableState { delta -> onDrag(delta) },
        onDragStopped = { velocity -> onDragStopped(velocity) },
    )

    private fun onDrag(delta: Float) {
        // Undamped downward travel once already at the top drives the status-bar gestures; the
        // damped offset would need more than a screen of dragging.
        if (delta > 0f && offsetY.value >= -2f) {
            rawPullDown += delta
            if (!pullActionFired && rawPullDown > deepPullPx) {
                pullActionFired = true
                actions.onExpandShade()
            } else if (!peekFired && rawPullDown > peekPullPx) {
                peekFired = true
                actions.onPeekStatusBar()
            }
        }
        // The mirror of the pull-down, measured from the bottom of the stack. Reported as it
        // happens rather than fired once, so the list can come in under the finger.
        if (swipeUpOpensAppList && delta < 0f && offsetY.value <= minOffset + 2f) {
            rawPullUp -= delta
            actions.onSwipeUpDrag(rawPullUp, -delta)
        }
        scope.launch {
            val next = offsetY.value + delta
            val outOfBounds = next > 0f || next < minOffset
            offsetY.snapTo(if (outOfBounds) offsetY.value + delta * 0.4f else next)
        }
    }

    private suspend fun onDragStopped(velocity: Float) {
        if (!pullActionFired && rawPullDown > peekPullPx && velocity > 2200f) actions.onExpandShade()
        if (swipeUpOpensAppList && rawPullUp > 0f) actions.onSwipeUpEnd(velocity)
        rawPullDown = 0f
        rawPullUp = 0f
        pullActionFired = false
        peekFired = false

        val target = exponentialDecay<Float>(frictionMultiplier = 1.6f)
            .calculateTargetValue(offsetY.value, velocity)
            .coerceIn(minOffset, 0f)
        offsetY.animateTo(
            targetValue = target,
            initialVelocity = velocity,
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        )
    }

    /** Sideways swipes in the empty space under the stack, when anything is bound to them. */
    fun quickLaunchModifier(enabled: Boolean, editMode: Boolean): Modifier =
        if (!enabled) Modifier else Modifier.pointerInput(editMode, contentHeight) {
            var eligible = false
            var travelled = 0f
            detectHorizontalDragGestures(
                onDragStart = { start ->
                    // The empty space under the whole stack, and the bottom of the screen even
                    // when favorites reach it, for a list too long to leave any space.
                    eligible = !editMode && (
                        start.y > offsetY.value + contentHeight ||
                            start.y > size.height * (1f - QUICK_LAUNCH_BOTTOM_FRACTION)
                        )
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
        .offset { IntOffset(0, offsetY.value.roundToInt()) }

    /** Back to the top, for when HOME is pressed on a home screen already showing. */
    suspend fun scrollToTop() {
        if (offsetY.value != 0f) {
            offsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium))
        }
    }

    /** Moves up by what is hidden below the screen, but never so far that [topInView] leaves the top. */
    suspend fun revealBelow(overflow: Float, topInView: Float) {
        if (overflow <= 0f) return
        val target = (offsetY.value - minOf(overflow, topInView)).coerceIn(minOf(0f, (viewportHeight - contentHeight).toFloat()), 0f)
        offsetY.animateTo(target, spring(stiffness = Spring.StiffnessMediumLow))
    }
}

@Composable
internal fun rememberHomeScroll(actions: GestureActions, swipeUpOpensAppList: Boolean, homeIntentTick: Int): HomeScroll {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val scroll = remember(actions, swipeUpOpensAppList) {
        HomeScroll(
            scope = scope,
            actions = actions,
            swipeUpOpensAppList = swipeUpOpensAppList,
            peekPullPx = with(density) { 30.dp.toPx() },
            deepPullPx = with(density) { 200.dp.toPx() },
            quickLaunchPx = with(density) { 80.dp.toPx() },
        )
    }
    LaunchedEffect(homeIntentTick) { if (homeIntentTick > 0) scroll.scrollToTop() }
    // Turning the phone changes how much there is to scroll through: a stack dragged up in
    // landscape kept that offset when the screen went tall again, which left it above the top.
    LaunchedEffect(scroll.minOffset) {
        val settled = scroll.offsetY.value.coerceIn(scroll.minOffset, 0f)
        if (settled != scroll.offsetY.value) scroll.offsetY.snapTo(settled)
    }
    return scroll
}
