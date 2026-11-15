// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.edit

import dev.nikelauncher.data.HomeBlock
import org.junit.Assert.*
import org.junit.Test

class EditPlacementTest {
    @Test fun selectedFramesTranslateTogetherWithoutMovingTheirNeighbor() {
        val tops = mapOf(HomeBlock.WIDGET to 100, HomeBlock.NOW_PLAYING to 400, HomeBlock.FAVORITES to 800)
        val selected = setOf(HomeBlock.WIDGET, HomeBlock.NOW_PLAYING)
        val moved = shiftedTops(tops, selected, -200, 2000)
        assertEquals(0, moved[HomeBlock.WIDGET]); assertEquals(300, moved[HomeBlock.NOW_PLAYING])
        assertEquals(800, moved[HomeBlock.FAVORITES])
        val spaced = spacedTops(tops, mapOf(HomeBlock.WIDGET to 200, HomeBlock.NOW_PLAYING to 80, HomeBlock.FAVORITES to 300), selected, 40)
        assertEquals(340, spaced[HomeBlock.NOW_PLAYING]); assertEquals(800, spaced[HomeBlock.FAVORITES])
    }
}
