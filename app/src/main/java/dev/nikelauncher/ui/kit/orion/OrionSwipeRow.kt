// SPDX-License-Identifier: GPL-3.0-or-later
/* Adapted from Orion Store, RookieEnough and contributors, GPL-3.0, by way of iris-android
 * (components/orion/OrionSwipeRow.kt). Source: Orion e561e91, ui/Swipe.kt.
 * Nike changes: a removing action may refuse (the row then comes back); reduced motion follows the system.
 */
package dev.nikelauncher.ui.kit.orion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.sign
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * What a swipe reveals and does. [removes] rows slide out and fold away before [run]; when [run] answers false the
 * row unfolds and springs back. Actions that do not remove fire at once and spring back.
 */
class OrionSwipeAction(val icon: ImageVector, val label: String, val bg: Color, val fg: Color, val removes: Boolean, val run: () -> Boolean)

/**
 * Swipe a row right ([right]) or left ([left]). The row follows the finger 1:1, the action icon grows and turns in with the
 * travel (read in the draw layer, so dragging never recomposes), the colour fills with a tick once the swipe is armed, and
 * release springs on the expressive scheme. A removed row folds its height away, so whatever holds it shrinks smoothly.
 * Content must paint its own opaque container.
 */
@Composable
fun OrionSwipeRow(
    shape: Shape,
    modifier: Modifier = Modifier,
    right: OrionSwipeAction? = null,
    left: OrionSwipeAction? = null,
    content: @Composable () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val cs = MaterialTheme.colorScheme
    val trip = with(LocalDensity.current) { 88.dp.toPx() }
    val currentRight by rememberUpdatedState(right)
    val currentLeft by rememberUpdatedState(left)
    // Plain state while dragging, a cancellable animate() only to settle: per-delta snapTo coroutines used to land after
    // release and cancel the spring-back, leaving rows stuck mid-swipe.
    var x by remember { mutableFloatStateOf(0f) }
    var settle by remember { mutableStateOf<Job?>(null) }
    fun to(target: Float, spec: AnimationSpec<Float>, then: () -> Unit = {}) {
        settle?.cancel()
        settle = scope.launch { animate(x, target, animationSpec = spec) { v, _ -> x = v }; then() }
    }
    var width by remember { mutableFloatStateOf(Float.MAX_VALUE) }
    val shown = remember { MutableTransitionState(true) }
    var fired by remember { mutableStateOf<OrionSwipeAction?>(null) }
    val motion = orionMotion()
    val spring = if (motion) MaterialTheme.motionScheme.defaultSpatialSpec<Float>() else snap()
    val fx = MaterialTheme.motionScheme.fastEffectsSpec<Color>()
    val dir by remember { derivedStateOf { sign(x) } }
    val armed by remember { derivedStateOf { abs(x) >= trip } }
    LaunchedEffect(armed) { if (armed) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    LaunchedEffect(shown.isIdle, shown.currentState) {
        val action = fired
        if (shown.isIdle && !shown.currentState && action != null) {
            fired = null
            if (!action.run()) { x = 0f; shown.targetState = true }
        }
    }
    val drag = rememberDraggableState { d ->
        // Non-removing actions get rubber-band resistance past the trip point, so the row never looks like it is leaving.
        val a = if (x + d >= 0f) currentRight else currentLeft
        val k = if (a?.removes == false && abs(x) > trip) .35f else 1f
        x = (x + d * k).coerceIn(if (currentLeft != null) -width else 0f, if (currentRight != null) width else 0f)
    }
    AnimatedVisibility(
        shown,
        modifier,
        enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()),
        exit = shrinkVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) + fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()),
    ) {
        val a = if (dir > 0f) right else if (dir < 0f) left else null
        val bg by animateColorAsState(if (armed && a != null) a.bg else cs.surfaceContainerHighest, fx, label = "swipe-bg")
        val fg by animateColorAsState(if (armed && a != null) a.fg else cs.onSurfaceVariant, fx, label = "swipe-fg")
        Box(
            Modifier.clip(shape)
                .semantics {
                    customActions = listOfNotNull(left, right).map { action -> CustomAccessibilityAction(action.label) { action.run() } }
                }
                .onSizeChanged { width = it.width.toFloat() }
                .draggable(drag, Orientation.Horizontal, onDragStarted = { settle?.cancel() }, onDragStopped = { v ->
                    val act = if (x > 0f) right else if (x < 0f) left else null
                    val flung = abs(v) > 1600f && sign(v) == sign(x) && abs(x) > trip * .4f
                    when {
                        act == null || !(abs(x) >= trip || flung) -> to(0f, spring)
                        act.removes -> {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            to(sign(x) * width, if (motion) tween(160, easing = FastOutLinearInEasing) else snap()) {
                                fired = act
                                shown.targetState = false
                            }
                        }
                        else -> {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            act.run()
                            to(0f, spring)
                        }
                    }
                }),
        ) {
            Row(
                Modifier.matchParentSize().background(bg).padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (dir < 0f) Arrangement.End else Arrangement.Start,
            ) {
                if (a != null) Icon(a.icon, a.label, Modifier.graphicsLayer {
                    val k = (abs(x) / trip).coerceIn(0f, 1f)
                    scaleX = .4f + .6f * k; scaleY = scaleX; alpha = k; rotationZ = (1f - k) * -45f * dir
                }, tint = fg)
            }
            Box(Modifier.graphicsLayer { translationX = x }) { content() }
        }
    }
}
