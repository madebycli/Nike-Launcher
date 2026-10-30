// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher

import dev.nikelauncher.data.QuickLaunchRegion
import dev.nikelauncher.ui.home.edit.*
import org.junit.Assert.*
import org.junit.Test

class GestureZoneGeometryTest {
    @Test fun `edge follows the configured side inset and corner keep out`() {
        val corner = GestureZoneBounds(350f, 800f, 411f, 900f)
        assertEquals(GestureZoneBounds(381f, 0f, 405f, 800f), edgeGestureBounds(411f, 923f, false, 24f, 6f, false, 200f, 300f, corner))
        assertEquals(GestureZoneBounds(6f, 0f, 30f, 923f), edgeGestureBounds(411f, 923f, true, 24f, 6f, false, 200f, 300f, corner))
    }
    @Test fun `confined edge matches the letter band`() {
        assertEquals(GestureZoneBounds(0f, 200f, 24f, 500f), edgeGestureBounds(411f, 923f, true, 24f, 0f, true, 200f, 300f, null))
    }
    @Test fun `quick launch overlay matches the gesture containment`() {
        val region = QuickLaunchRegion(bottomPercent = 10, heightPercent = 25)
        val bounds = quickLaunchBounds(411f, 923f, region)
        assertTrue(region.contains(bounds.top, 923f))
        assertTrue(region.contains(bounds.bottom - 1f, 923f))
        assertFalse(region.contains(bounds.bottom, 923f))
        assertFalse(region.contains(bounds.top - 1f, 923f))
    }
}
