// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.edit

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.nikelauncher.data.BlockPin
import dev.nikelauncher.data.HomeBlock
import dev.nikelauncher.data.HomePins
import dev.nikelauncher.ui.home.stack.SnapGuide
import dev.nikelauncher.ui.home.stack.StackMetrics
import dev.nikelauncher.ui.home.stack.snapped
import kotlin.math.roundToInt

/**
 * Moving the home blocks: which one is selected, which one a finger is holding, and the pins as
 * the finger has made them so far.
 *
 * What is stored only changes on release; until the stored pins catch up the live ones are kept,
 * so letting go never flashes back to the old place.
 */
@Stable
internal class PinEditor(
    private val metrics: StackMetrics,
    private val snapDistancePx: Int,
    private val stored: () -> HomePins,
    private val commit: (HomeBlock, BlockPin?) -> Unit,
    private val tick: () -> Unit,
) {
    var live by mutableStateOf<HomePins?>(null)
        private set
    var selected by mutableStateOf<HomeBlock?>(null)
        private set
    var dragging by mutableStateOf<HomeBlock?>(null)
        private set
    var guide by mutableStateOf<SnapGuide?>(null)
        private set

    /** [0] where the held block's top was when the finger came down, [1] how far it has travelled. */
    private val origin = floatArrayOf(0f, 0f)

    val pins: HomePins get() = live ?: stored()

    fun select(block: HomeBlock) {
        selected = block
    }

    fun begin(block: HomeBlock) {
        dragging = block
        selected = block
        origin[0] = metrics.top(block).toFloat()
        origin[1] = 0f
        live = live ?: stored()
        tick()
    }

    fun move(block: HomeBlock, deltaPx: Float) {
        origin[1] += deltaPx
        val (pin, snappedTo) = snapped(
            topPx = (origin[0] + origin[1]).roundToInt(),
            heightPx = metrics.height(block),
            viewportPx = metrics.viewportPx,
            bottomInsetPx = metrics.bottomInsetPx,
            snapDistancePx = snapDistancePx,
        )
        live = (live ?: stored()).with(block, pin)
        if (snappedTo != guide) {
            guide = snappedTo
            if (snappedTo != null) tick()
        }
    }

    fun end(block: HomeBlock) {
        live?.get(block)?.let { commit(block, it) }
        dragging = null
        guide = null
    }

    /** Puts the selected block back to its default place. */
    fun resetSelected() {
        selected?.let { commit(it, null) }
        live = null
    }

    /** The stored pins moved on, so the live ones are no longer needed. */
    fun onStoredChanged() {
        if (dragging == null) live = null
    }

    fun clear() {
        selected = null
        dragging = null
        guide = null
        live = null
    }
}
