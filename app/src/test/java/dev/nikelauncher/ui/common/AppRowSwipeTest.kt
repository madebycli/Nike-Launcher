// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.common

import dev.nikelauncher.data.ShortcutSwipe
import org.junit.Assert.*
import org.junit.Test

class AppRowSwipeTest {
    @Test fun `notification overview owns right swipe even with shortcuts disabled or assigned right`() {
        ShortcutSwipe.entries.forEach { mode ->
            assertEquals(AppSwipeAction.NOTIFICATIONS, appSwipeAction(100f, 90f, mode, true))
            assertEquals(AppSwipeAction.NONE, appSwipeAction(80f, 90f, mode, true))
        }
    }
    @Test fun `opposite direction retains only explicit shortcut choice`() {
        assertEquals(AppSwipeAction.SHORTCUTS, appSwipeAction(-100f, 90f, ShortcutSwipe.LEFT, true))
        assertEquals(AppSwipeAction.NONE, appSwipeAction(-100f, 90f, ShortcutSwipe.RIGHT, true))
        assertEquals(AppSwipeAction.NONE, appSwipeAction(100f, 90f, ShortcutSwipe.OFF, false))
    }
}
