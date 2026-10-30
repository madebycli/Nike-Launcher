// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.setup

import org.junit.Assert.*
import org.junit.Test

class PreviewAppsTest {
    @Test fun `preview uses two distinct installed apps and fills incomplete favorites`() {
        assertEquals(listOf("b", "a"), previewAppKeys(listOf("missing", "b", "b"), listOf("a", "b", "c")))
        assertEquals(listOf("a", "b"), previewAppKeys(emptyList(), listOf("a", "b", "c")))
        assertEquals(listOf("a"), previewAppKeys(emptyList(), listOf("a")))
    }
}
