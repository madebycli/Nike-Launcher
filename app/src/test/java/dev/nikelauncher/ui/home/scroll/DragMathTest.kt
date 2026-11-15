// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.scroll

import org.junit.Assert.*
import org.junit.Test

class DragMathTest {
    @Test fun overscrollReboundsToTheSameBoundaryDespiteReleaseVelocity() {
        assertEquals(0f, settleTarget(50f, -500f, -350f))
        assertEquals(-500f, settleTarget(-550f, -500f, -100f))
        assertEquals(-220f, settleTarget(-100f, -500f, -220f))
        assertEquals(0f, settleTarget(-100f, 0f, -220f))
    }
    @Test fun elasticTravelIsBoundedAndScrollingInsideTheContentIsUndamped() {
        assertEquals(-40f, draggedOffset(0f, -100f, 0f, 150f))
        assertEquals(150f, draggedOffset(100f, 10000f, 0f, 150f))
        assertEquals(-200f, draggedOffset(-100f, -100f, -500f, 150f))
        assertEquals(-540f, draggedOffset(-500f, -100f, -500f, 150f))
    }
}
