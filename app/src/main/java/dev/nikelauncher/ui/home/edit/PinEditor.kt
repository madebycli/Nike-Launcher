// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.edit

import androidx.compose.runtime.*
import dev.nikelauncher.data.*
import dev.nikelauncher.ui.home.stack.*
import kotlin.math.roundToInt

/** A layout draft stays visible until the atomic preference write has been acknowledged. */
@Stable
internal class PinEditor(
    private val metrics: StackMetrics,
    private val snapDistancePx: Int,
    private val stored: () -> HomePins,
    private val commit: (Map<HomeBlock, BlockPin?>) -> Unit,
    private val tick: () -> Unit,
) {
    var live by mutableStateOf<HomePins?>(null)
        private set
    var selection by mutableStateOf(setOf<HomeBlock>())
        private set
    val selected: HomeBlock? get() = selection.singleOrNull()
    var dragging by mutableStateOf<HomeBlock?>(null)
        private set
    var guide by mutableStateOf<SnapGuide?>(null)
        private set
    val pins: HomePins get() = live ?: stored()
    private var available = setOf<HomeBlock>()
    private var origin = HomePins.Default
    private var originTops = emptyMap<HomeBlock, Int>()
    private var travelled = 0f
    private var nudgeRemainder = 0f
    private var beforeDrag: HomePins? = null

    fun setAvailable(blocks: Set<HomeBlock>) { available = blocks; selection = selection.intersect(blocks) }
    fun select(block: HomeBlock) {
        nudgeRemainder = 0f
        selection = if (block in selection) selection - block else selection + block
    }
    fun selectAll() { selection = if (selection == available) emptySet() else available }

    private fun freeze(): HomePins {
        var frozen = pins
        available.forEach { block ->
            if (frozen[block] == null) frozen = frozen.with(block, pinForTop(PinEdge.TOP,
                metrics.top(block), metrics.height(block), metrics.viewportPx, metrics.bottomInsetPx))
        }
        return frozen
    }

    fun begin(block: HomeBlock) {
        if (block !in selection) selection = setOf(block)
        dragging = block
        beforeDrag = live
        origin = freeze()
        originTops = available.associateWith { metrics.top(it) }
        travelled = 0f
        live = origin
        tick()
    }
    fun move(block: HomeBlock, deltaPx: Float) {
        if (dragging != block) return
        travelled += deltaPx
        var delta = travelled.roundToInt()
        guide = null
        if (selection.size == 1) {
            val (_, snap) = snapped(originTops.getValue(block) + delta, metrics.height(block),
                metrics.viewportPx, metrics.bottomInsetPx, snapDistancePx)
            if (snap != null) {
                delta = pinnedTopPx(BlockPin(snap.edge, snap.fraction), metrics.height(block), metrics.viewportPx, metrics.bottomInsetPx) - originTops.getValue(block)
                guide = snap
            }
        }
        val tops = shiftedTops(originTops, selection, delta, metrics.viewportPx)
        live = applyTops(origin, tops.filterKeys { it in selection })
    }
    fun end(block: HomeBlock) {
        if (dragging != block) return
        save()
        dragging = null
        guide = null
    }
    fun cancel() { if (dragging != null) live = beforeDrag; dragging = null; guide = null }

    fun nudgeSelection(deltaPx: Float) {
        val exact = deltaPx + nudgeRemainder
        val step = exact.roundToInt()
        nudgeRemainder = exact - step
        val current = freeze()
        val tops = available.associateWith { block -> current[block]?.let { fittedTop(pinnedTopPx(it, metrics.height(block), metrics.viewportPx, metrics.bottomInsetPx), metrics.height(block), metrics.viewportPx, metrics.bottomInsetPx) } ?: metrics.top(block) }
        live = applyTops(current, shiftedTops(tops, selection, step, metrics.viewportPx).filterKeys { it in selection })
        save()
    }
    fun setGap(gapPx: Int) {
        val targets = selection.ifEmpty { available }
        live = applyTops(freeze(), spacedTops(available.associateWith { metrics.top(it) },
            available.associateWith { metrics.height(it) }, targets, gapPx))
        save()
    }
    private fun applyTops(base: HomePins, tops: Map<HomeBlock, Int>): HomePins = tops.entries.fold(base) { pins, (block, top) ->
        pins.with(block, pinForTop(PinEdge.TOP, top, metrics.height(block), metrics.viewportPx, metrics.bottomInsetPx))
    }
    private fun save() { commit(available.associateWith { pins[it] }) }

    fun resetSelected() {
        val targets = selection.ifEmpty { available }
        live = targets.fold(pins) { result, block -> result.with(block, HomePins.Default[block]) }
        commit(targets.associateWith { HomePins.Default[it] })
    }
    fun onStoredChanged() {
        if (dragging == null && HomeBlock.entries.all { live?.get(it)?.encode() == stored()[it]?.encode() }) live = null
    }
    fun clear() { cancel(); selection = emptySet() }
}
