// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.edit

import dev.nikelauncher.data.*
import dev.nikelauncher.ui.home.stack.*
import org.junit.Assert.*
import org.junit.Test

class PinEditorTest {
    @Test fun editorKeepsPendingWritesAfterDoneAndAcknowledgesEncodedPrecision() {
        val metrics = StackMetrics().apply { viewportPx = 2000; tops[2] = 700; heights[2] = 600 }
        var stored = HomePins.Default
        var written = emptyMap<HomeBlock, BlockPin?>()
        val editor = PinEditor(metrics, 10, { stored }, { written = it }, {})
        editor.setAvailable(setOf(HomeBlock.FAVORITES)); editor.select(HomeBlock.FAVORITES)
        repeat(7) { editor.nudgeSelection(-1f) }
        val wanted = editor.pins.favorites!!
        assertEquals(693, pinnedTopPx(wanted, 600, 2000, 0))
        editor.clear()
        assertEquals(wanted, editor.pins.favorites)
        stored = stored.with(HomeBlock.FAVORITES, BlockPin.decode(written.getValue(HomeBlock.FAVORITES)!!.encode()))
        editor.onStoredChanged(); assertNull(editor.live)
        assertEquals(wanted.encode(), editor.pins.favorites!!.encode())
    }
    @Test fun cancellingAFrameDragRestoresTheDraftWithoutWriting() {
        val metrics = StackMetrics().apply { viewportPx = 2000; tops[2] = 700; heights[2] = 600 }
        var writes = 0
        val editor = PinEditor(metrics, 10, { HomePins.Default }, { writes++ }, {})
        editor.setAvailable(setOf(HomeBlock.FAVORITES)); editor.begin(HomeBlock.FAVORITES)
        editor.move(HomeBlock.FAVORITES, 190f); editor.cancel()
        assertEquals(HomePins.Default, editor.pins); assertEquals(0, writes)
    }
}
