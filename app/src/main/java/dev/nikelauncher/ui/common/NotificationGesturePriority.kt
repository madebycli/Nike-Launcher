// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.common

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.input.pointer.PointerId

/** Per-home pointer ownership; never stored globally or used as observable UI state. */
internal class NotificationGesturePriority {
    private var pointer: PointerId? = null
    fun begin(id: PointerId) { pointer = id }
    fun end(id: PointerId) { if (pointer == id) pointer = null }
    fun owns(id: PointerId): Boolean = pointer == id
}

internal val LocalNotificationGesturePriority = staticCompositionLocalOf<NotificationGesturePriority?> { null }
