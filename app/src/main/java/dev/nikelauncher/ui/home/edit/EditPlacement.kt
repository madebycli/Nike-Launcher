// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.edit

import dev.nikelauncher.data.HomeBlock

/** Translate selected frames together, keeping every selected top on screen. */
internal fun shiftedTops(tops: Map<HomeBlock, Int>, selected: Set<HomeBlock>, delta: Int, viewport: Int): Map<HomeBlock, Int> {
    val positions = tops.filterKeys { it in selected }.values
    if (positions.isEmpty()) return tops
    val bounded = delta.coerceIn(-positions.min(), maxOf(-positions.min(), viewport - 1 - positions.max()))
    return tops.mapValues { (block, top) -> if (block in selected) top + bounded else top }
}

/** Equal gaps in visual order, anchored to the first selected frame; other frames stay put. */
internal fun spacedTops(tops: Map<HomeBlock, Int>, heights: Map<HomeBlock, Int>, selected: Set<HomeBlock>, gap: Int): Map<HomeBlock, Int> {
    var end: Int? = null
    return tops.toMutableMap().apply {
        selected.filter { it in tops }.sortedBy { tops.getValue(it) }.forEach { block ->
            val top = end?.plus(gap.coerceAtLeast(0)) ?: tops.getValue(block)
            this[block] = top
            end = top + heights.getValue(block)
        }
    }
}
