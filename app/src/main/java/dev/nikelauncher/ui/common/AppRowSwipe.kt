// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.common

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import dev.nikelauncher.data.ShortcutSwipe

internal enum class AppSwipeAction { NONE, NOTIFICATIONS, SHORTCUTS }

internal fun appSwipeAction(distance: Float, threshold: Float, mode: ShortcutSwipe, notifications: Boolean): AppSwipeAction = when {
    notifications && distance > threshold -> AppSwipeAction.NOTIFICATIONS
    mode == ShortcutSwipe.RIGHT && distance > threshold -> AppSwipeAction.SHORTCUTS
    mode == ShortcutSwipe.LEFT && distance < -threshold -> AppSwipeAction.SHORTCUTS
    else -> AppSwipeAction.NONE
}

/** Owns only horizontal drags; notification overview has priority over optional shortcuts. */
@Composable
fun Modifier.swipeForShortcuts(mode: ShortcutSwipe, enabled: Boolean,
    onNotifications: (() -> Unit)? = null, onSwipe: () -> Unit): Modifier {
    if (!enabled || (mode == ShortcutSwipe.OFF && onNotifications == null)) return this
    val threshold = with(LocalDensity.current) { 36.dp.toPx() }
    val latestNotifications = rememberUpdatedState(onNotifications)
    val latestShortcut = rememberUpdatedState(onSwipe)
    return pointerInput(mode, onNotifications != null, threshold) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var distance = 0f
            var fired = false
            fun advance(amount: Float) {
                distance += amount
                if (fired) return
                when (appSwipeAction(distance, threshold, mode, latestNotifications.value != null)) {
                    AppSwipeAction.NOTIFICATIONS -> { fired = true; latestNotifications.value?.invoke() }
                    AppSwipeAction.SHORTCUTS -> { fired = true; latestShortcut.value() }
                    AppSwipeAction.NONE -> Unit
                }
            }
            val start = awaitHorizontalTouchSlopOrCancellation(down.id) { change, over ->
                change.consume(); advance(over)
            }
            if (start != null) horizontalDrag(start.id) { change ->
                advance(change.position.x - change.previousPosition.x); change.consume()
            }
        }
    }
}
