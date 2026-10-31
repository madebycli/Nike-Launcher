// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.route

import org.junit.Assert.*
import org.junit.Test

class SearchDismissalTest {
    @Test fun `only a previously visible keyboard closing on an empty query dismisses`() {
        assertFalse(dismissEmptySearch(false, false, ""))
        assertFalse(dismissEmptySearch(true, true, ""))
        assertFalse(dismissEmptySearch(true, false, "calendar"))
        assertTrue(dismissEmptySearch(true, false, "  "))
    }
}
