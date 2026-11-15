// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.blocks

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Reordering of the favorites rows by a grab handle.
 *
 * Runs against a local copy of the order and is committed once on release; going through storage
 * on every swap would lag behind the finger. Rows are identified by [tokens], never by position:
 * a position is what a swap changes, so anything remembered against one would be read back for
 * whichever row had since moved into it.
 */
@Stable
internal class FavoritesReorder {
    /** The order while a row is held, or null when nothing is being dragged. */
    var order by mutableStateOf<List<String>?>(null)
        private set
    private var held by mutableStateOf<String?>(null)

    /** How far the held row has travelled from its slot. */
    var dragOffset by mutableFloatStateOf(0f)
        private set

    /**
     * Row heights and tops, written by every row on every layout pass. Plain maps on purpose: as
     * snapshot state, read back during composition, each write would ask for a recomposition that
     * laid the rows out and wrote again, which never settles.
     */
    val heights = HashMap<String, Int>()
    val tops = HashMap<String, Float>()

    fun isHeld(token: String) = held == token

    fun begin(token: String, current: List<String>) {
        if (token !in current) return
        order = current
        held = token
        dragOffset = 0f
    }

    /** Swaps the held row with a neighbour once it has travelled past that neighbour's midpoint. */
    fun dragBy(amount: Float) {
        val list = order ?: return
        val token = held ?: return
        val from = list.indexOf(token)
        if (from < 0) return
        dragOffset += amount

        val below = list.getOrNull(from + 1)?.let { heights[it] }
        if (below != null && dragOffset > below / 2f) {
            order = list.toMutableList().apply { add(from + 1, removeAt(from)) }
            dragOffset -= below
            return
        }
        val above = list.getOrNull(from - 1)?.let { heights[it] }
        if (above != null && dragOffset < -above / 2f) {
            order = list.toMutableList().apply { add(from - 1, removeAt(from)) }
            dragOffset += above
        }
    }

    /** Hands the final order to [commit] and clears the drag. */
    fun finish(commit: (List<String>) -> Unit) {
        order?.let(commit)
        cancel()
    }

    /** Drops anything a drag left standing, back to the order that was saved. */
    fun cancel() {
        order = null
        held = null
        dragOffset = 0f
    }

    /** Forgets measurements of rows that are gone. */
    fun retain(present: Set<String>) {
        heights.keys.retainAll(present)
        tops.keys.retainAll(present)
    }
}
