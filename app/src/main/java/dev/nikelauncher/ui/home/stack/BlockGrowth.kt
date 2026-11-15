// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.stack

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * How much a block has grown below its own content, by things that come and go, such as the
 * notification lines under an app.
 *
 * The stack holds a block by its height without these, so a block pinned by its middle or bottom
 * does not move up when a notification arrives: the lines only ever add room underneath.
 */
@Stable
internal class BlockGrowth {
    private val parts = mutableStateMapOf<Any, Int>()

    val totalPx: Int get() = parts.values.sum()

    fun report(owner: Any, heightPx: Int) {
        if (parts[owner] != heightPx) parts[owner] = heightPx
    }

    fun forget(owner: Any) {
        parts.remove(owner)
    }
}

/** The growth of the block being composed, if it keeps one. */
internal val LocalBlockGrowth = staticCompositionLocalOf<BlockGrowth?> { null }
