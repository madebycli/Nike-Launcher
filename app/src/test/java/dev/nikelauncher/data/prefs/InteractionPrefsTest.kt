// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.data.prefs

import org.junit.Assert.*
import org.junit.Test

class InteractionPrefsTest {
    @Test fun `legacy search conflicts prefer the existing visible bar`() {
        assertEquals(SearchDisplayMode.BAR, searchDisplayMode(true, true))
        assertEquals(SearchDisplayMode.BAR, searchDisplayMode(true, false))
        assertEquals(SearchDisplayMode.BUTTON, searchDisplayMode(false, true))
        assertEquals(SearchDisplayMode.NONE, searchDisplayMode(false, false))
    }
    @Test fun `invalid restored setup steps stay inside the flow`() {
        assertEquals(0, setupStep(-1)); assertEquals(4, setupStep(Int.MAX_VALUE))
    }
}
