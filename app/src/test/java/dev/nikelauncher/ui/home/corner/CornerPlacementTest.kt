// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.corner

import dev.nikelauncher.data.*
import org.junit.Assert.*
import org.junit.Test

class CornerPlacementTest {
    @Test fun `button stays inside insets and normalized coordinates round trip after resize`() {
        val travel = cornerTravel(1080, 2424, 168, 52, 72, 100)
        assertEquals(CornerPosition(0f, 0f), travel.position(-200f, -200f))
        assertEquals(CornerPosition(1f, 1f), travel.position(4000f, 4000f))
        val saved = travel.position(500f, 1200f)
        val resized = cornerTravel(800, 1600, 168, 52, 72, 100)
        val location = resized.location(saved)
        assertEquals(saved.x, resized.position(location.first.toFloat(), location.second.toFloat()).x, .002f)
        assertEquals(saved.y, resized.position(location.first.toFloat(), location.second.toFloat()).y, .002f)
    }
    @Test fun `partial or nonfinite backup position is absent`() {
        assertNull(cornerPosition(null, .5f)); assertNull(cornerPosition(Float.NaN, 1f))
        assertNull(cornerPosition(1f, Float.POSITIVE_INFINITY))
        assertEquals(CornerPosition(0f, 1f), cornerPosition(-1f, 2f))
    }
}
