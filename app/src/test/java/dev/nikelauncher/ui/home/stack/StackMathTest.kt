// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.stack

import dev.nikelauncher.data.BlockPin
import dev.nikelauncher.data.HomeBlock
import dev.nikelauncher.data.PinEdge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class StackMathTest {
    private val viewport = 2000
    private val inset = 100
    private val gap = 32

    private fun widget(pin: BlockPin? = null, height: Int = 360) = StackBlock(HomeBlock.WIDGET, height, pin)
    private fun nowPlaying(pin: BlockPin? = null, height: Int = 128) = StackBlock(HomeBlock.NOW_PLAYING, height, pin)
    private fun favorites(pin: BlockPin? = null, height: Int = 600) = StackBlock(HomeBlock.FAVORITES, height, pin)

    private fun place(vararg blocks: StackBlock) = placeStack(blocks.toList(), viewport, inset, gap)

    @Test fun unpinnedBlocksFollowEachOtherWithTheGap() {
        val placed = place(widget(), nowPlaying(), favorites())
        assertEquals(0, placed.tops.getValue(HomeBlock.WIDGET))
        assertEquals(360 + gap, placed.tops.getValue(HomeBlock.NOW_PLAYING))
        assertEquals(360 + gap + 128 + gap, placed.tops.getValue(HomeBlock.FAVORITES))
        assertEquals(360 + gap + 128 + gap + 600, placed.contentHeightPx)
    }

    @Test fun absentBlockTakesNoSpace() {
        val placed = place(widget(), favorites())
        assertNull(placed.tops[HomeBlock.NOW_PLAYING])
        assertEquals(360 + gap, placed.tops.getValue(HomeBlock.FAVORITES))
    }

    @Test fun pinnedFavoritesDoNotMoveWhenTheBlockAboveGrowsIntoFreeSpace() {
        val pin = BlockPin(PinEdge.CENTER, 0.5f)
        val before = place(widget(), favorites(pin))
        val after = place(widget(height = 400), nowPlaying(), favorites(pin))
        assertEquals(700, before.tops.getValue(HomeBlock.FAVORITES))
        assertEquals(700, after.tops.getValue(HomeBlock.FAVORITES))
    }

    @Test fun nowPlayingAppearingDoesNotMovePinnedFavorites() {
        val pin = BlockPin(PinEdge.TOP, 0.5f)
        assertEquals(1000, place(widget(), favorites(pin)).tops.getValue(HomeBlock.FAVORITES))
        assertEquals(1000, place(widget(), nowPlaying(), favorites(pin)).tops.getValue(HomeBlock.FAVORITES))
    }

    @Test fun blockIsPushedDownRatherThanOverlappingTheOneAbove() {
        val placed = place(widget(height = 700), favorites(BlockPin(PinEdge.TOP, 0.2f)))
        assertEquals(700 + gap, placed.tops.getValue(HomeBlock.FAVORITES))
    }

    @Test fun pushesChainDownTheStack() {
        val pin = BlockPin(PinEdge.TOP, 0.5f)
        val placed = place(widget(pin), nowPlaying(pin), favorites(pin))
        assertEquals(1000, placed.tops.getValue(HomeBlock.WIDGET))
        assertEquals(1000 + 360 + gap, placed.tops.getValue(HomeBlock.NOW_PLAYING))
        assertEquals(1392 + 128 + gap, placed.tops.getValue(HomeBlock.FAVORITES))
    }

    @Test fun bottomPinCountsFromTheUsableBottom() {
        val placed = place(favorites(BlockPin(PinEdge.BOTTOM, 0.05f), height = 400))
        assertEquals(2000 - 100 - 100 - 400, placed.tops.getValue(HomeBlock.FAVORITES))
    }

    @Test fun tallCenteredBlockNeverStartsAboveTheScreen() {
        val placed = place(favorites(BlockPin(PinEdge.CENTER, 0.5f), height = 2600))
        assertEquals(0, placed.tops.getValue(HomeBlock.FAVORITES))
    }

    @Test fun pinFractionOutsideTheScreenIsClamped() {
        assertEquals(2000, place(favorites(BlockPin(PinEdge.TOP, 1f), height = 100)).tops.getValue(HomeBlock.FAVORITES))
    }

    @Test fun pinForTopInvertsPinnedTop() {
        for (edge in PinEdge.entries) {
            val pin = pinForTop(edge, 640, 300, viewport, inset)
            assertEquals(640, pinnedTopPx(pin, 300, viewport, inset))
        }
    }

    @Test fun draggedBlockSnapsToTheCentreGuide() {
        val centreTop = pinnedTopPx(BlockPin(PinEdge.CENTER, 0.5f), 400, viewport, inset)
        val (pin, guide) = snapped(centreTop + 6, 400, viewport, inset, snapDistancePx = 20)
        assertNotNull(guide)
        assertEquals(PinEdge.CENTER, pin.edge)
        assertEquals(0.5f, pin.fraction, 0.0001f)
    }

    @Test fun draggedBlockFarFromGuidesKeepsItsTopWhereItWasLeft() {
        val (pin, guide) = snapped(333, 400, viewport, inset, snapDistancePx = 10)
        assertNull(guide)
        assertEquals(333, pinnedTopPx(pin, 400, viewport, inset))
    }

    @Test fun pinsRoundTripThroughTheirStoredForm() {
        for (pin in listOf(BlockPin(PinEdge.TOP, 0.1234f), BlockPin(PinEdge.CENTER, 0.5f), BlockPin(PinEdge.BOTTOM, 0.03f))) {
            val decoded = BlockPin.decode(pin.encode())
            assertNotNull(decoded)
            assertEquals(pin.edge, decoded!!.edge)
            assertEquals(pin.fraction, decoded.fraction, 0.0001f)
        }
    }

    @Test fun damagedStoredPinsMeanNoPin() {
        for (raw in listOf(null, "", "X:0.5", "T0.5", "T:abc", "T:NaN", "T:Infinity", "C:" + "9".repeat(40))) {
            assertNull(raw, BlockPin.decode(raw))
        }
        assertEquals(1f, BlockPin.decode("T:7.5")!!.fraction, 0f)
    }
}
