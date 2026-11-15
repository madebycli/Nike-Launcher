// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.widget

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

internal class WidgetScrollTarget {
    var view: LongPressFrameLayout? = null
}

/** Resolve the axis before AndroidView's interop filter and the page's Main-pass drag. */
internal fun Modifier.protectWidgetScroll(target: WidgetScrollTarget, enabled: Boolean = true) =
    pointerInput(target, enabled) {
        if (!enabled) return@pointerInput
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val native = target.view ?: return@awaitEachGesture
            if (!native.hasScrollableContentAt(down.position.x, down.position.y))
                return@awaitEachGesture
            val slop = viewConfiguration.touchSlop / 2f
            var resolved = false
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!resolved && change.pressed) {
                    val delta = change.position - down.position
                    val dx = abs(delta.x)
                    val dy = abs(delta.y)
                    if (dy > slop && dy > dx * 1.15f) {
                        // This switches AndroidView to Initial-pass dispatch for this same move.
                        native.claimVerticalTouch()
                        resolved = true
                    } else if (dx > viewConfiguration.touchSlop && dx > dy) {
                        resolved = true
                    }
                }
            } while (event.changes.any { it.pressed })
        }
    }
