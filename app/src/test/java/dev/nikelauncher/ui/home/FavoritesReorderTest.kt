// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home

import dev.nikelauncher.ui.home.blocks.FavoritesReorder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FavoritesReorderTest {
    private fun reorder() = FavoritesReorder().apply {
        listOf("a", "b", "c").forEach { heights[it] = 100 }
    }

    @Test fun passingTheMidpointSwapsWithTheNeighbour() {
        val r = reorder().apply { begin("a", listOf("a", "b", "c")) }
        r.dragBy(40f)
        assertEquals(listOf("a", "b", "c"), r.order)
        r.dragBy(20f)
        assertEquals(listOf("b", "a", "c"), r.order)
        assertEquals(-40f, r.dragOffset, 0.001f)
    }

    @Test fun draggingUpSwapsWithTheRowAbove() {
        val r = reorder().apply { begin("c", listOf("a", "b", "c")) }
        r.dragBy(-60f)
        assertEquals(listOf("a", "c", "b"), r.order)
    }

    @Test fun finishingCommitsTheOrderOnceAndClears() {
        val r = reorder().apply { begin("a", listOf("a", "b", "c")) }
        r.dragBy(120f)
        var committed: List<String>? = null
        r.finish { committed = it }
        assertEquals(listOf("b", "a", "c"), committed)
        assertNull(r.order)
        assertEquals(0f, r.dragOffset, 0f)
    }

    @Test fun cancellingCommitsNothing() {
        val r = reorder().apply { begin("a", listOf("a", "b", "c")) }
        r.dragBy(120f)
        r.cancel()
        assertNull(r.order)
    }

    @Test fun unknownRowsCannotBeHeld() {
        val r = reorder().apply { begin("z", listOf("a", "b")) }
        assertNull(r.order)
    }
}
