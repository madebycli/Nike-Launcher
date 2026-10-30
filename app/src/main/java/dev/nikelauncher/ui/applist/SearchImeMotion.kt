// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.applist

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationSource
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import dev.nikelauncher.ui.kit.motion.AppMotion
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Follow native IME insets directly when they are animated. Some keyboards / Android versions
 * instead deliver an entire IME closing inset change in one frame. Preserve the previous
 * visible field position in that case and settle the correction, rather than teleporting down.
 * Opening always follows the IME directly, so the field never stays behind the keyboard.
 * The modifier observes layout only; individual inset frames never recompose the list.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun Modifier.smoothSearchImeJumps(
    enabled: Boolean,
    onFrame: ((Long, Float, Int) -> Unit)? = null,
): Modifier {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val threshold = with(density) { 96.dp.toPx() }
    val ime = WindowInsets.ime
    val imeSource = WindowInsets.imeAnimationSource
    val imeTarget = WindowInsets.imeAnimationTarget
    val motion = remember { SearchImeJumpMotion() }
    val spring = AppMotion.spatial<Float>()
    DisposableEffect(enabled) {
        if (!enabled) motion.reset()
        onDispose { motion.reset() }
    }
    // Optional debug-fixture probe: sample visual position and actual IME insets at frame rate.
    // Production passes null and does not start a frame loop.
    LaunchedEffect(enabled, onFrame) {
        if (enabled && onFrame != null) while (true) {
            withFrameNanos { time ->
                motion.previousY?.let { onFrame(time, it + motion.correction, ime.getBottom(density)) }
            }
        }
    }
    return onGloballyPositioned { coordinates ->
        if (enabled) {
            val y = coordinates.positionInWindow().y
            val imeBottom = ime.getBottom(density)
            val jump = motion.previousY?.let { it - y } ?: 0f
            val imeChange = motion.previousIme?.let { imeBottom - it } ?: 0
            motion.previousY = y
            motion.previousIme = imeBottom
            // Only compensate an actual simultaneous IME *closing* jump. Rotation, window
            // resize and list rearrangements do not qualify, nor does keyboard opening.
            if (imeChange < -threshold && jump < -threshold &&
                imeSource.getBottom(density) == imeTarget.getBottom(density)) {
                motion.job?.cancel()
                // Assign synchronously before drawing, so even the first changed frame stays
                // at the old position. Keep continuity if an opposite jump interrupts it.
                motion.correction += jump
                motion.job = scope.launch {
                    Animatable(motion.correction).animateTo(0f, spring) {
                        motion.correction = value
                    }
                }
            }
        }
    }.graphicsLayer { translationY = motion.correction }
}

private class SearchImeJumpMotion {
    var previousY: Float? = null
    var previousIme: Int? = null
    var correction by mutableFloatStateOf(0f)
    var job: Job? = null

    fun reset() {
        job?.cancel()
        job = null
        previousY = null
        previousIme = null
        correction = 0f
    }
}
