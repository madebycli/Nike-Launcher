// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.data

import java.util.Locale

/** The three things that sit on the home screen, top to bottom. */
enum class HomeBlock { WIDGET, NOW_PLAYING, FAVORITES }

/** Which part of a block is held at the pinned position. */
enum class PinEdge { TOP, CENTER, BOTTOM }

/**
 * Where a block is held on the screen, independent of everything around it.
 *
 * [fraction] is a share of the screen height. For [PinEdge.TOP] it is how far down the block's
 * top edge sits, for [PinEdge.CENTER] where its middle sits, and for [PinEdge.BOTTOM] how far
 * above the usable bottom of the screen the block's bottom sits. A block without a pin follows
 * the block above it.
 */
data class BlockPin(val edge: PinEdge, val fraction: Float) {
    init {
        require(fraction.isFinite())
    }

    fun encode(): String = "${edge.name.first()}:${String.format(Locale.ROOT, "%.4f", fraction)}"

    companion object {
        const val MIN_FRACTION = 0f
        const val MAX_FRACTION = 1f

        /** Anything that does not parse is no pin at all, so a damaged value falls back to the default. */
        fun decode(raw: String?): BlockPin? {
            if (raw.isNullOrBlank() || raw.length > 16) return null
            val edge = when (raw.firstOrNull()) {
                'T' -> PinEdge.TOP
                'C' -> PinEdge.CENTER
                'B' -> PinEdge.BOTTOM
                else -> return null
            }
            if (raw.getOrNull(1) != ':') return null
            val value = raw.substring(2).toFloatOrNull()?.takeIf { it.isFinite() } ?: return null
            return BlockPin(edge, value.coerceIn(MIN_FRACTION, MAX_FRACTION))
        }
    }
}

/** How far each block is moved sideways from where its alignment puts it, in dp: left negative, right positive. */
data class BlockOffsets(
    val widget: Int = 0,
    val nowPlaying: Int = 0,
    val favorites: Int = 0,
) {
    operator fun get(block: HomeBlock): Int = when (block) {
        HomeBlock.WIDGET -> widget
        HomeBlock.NOW_PLAYING -> nowPlaying
        HomeBlock.FAVORITES -> favorites
    }

    companion object {
        const val LIMIT_DP = 300
    }
}

/** The pin of every block; null means the block follows the one above it. */
data class HomePins(
    val widget: BlockPin? = null,
    val nowPlaying: BlockPin? = null,
    val favorites: BlockPin? = null,
) {
    operator fun get(block: HomeBlock): BlockPin? = when (block) {
        HomeBlock.WIDGET -> widget
        HomeBlock.NOW_PLAYING -> nowPlaying
        HomeBlock.FAVORITES -> favorites
    }

    fun with(block: HomeBlock, pin: BlockPin?): HomePins = when (block) {
        HomeBlock.WIDGET -> copy(widget = pin)
        HomeBlock.NOW_PLAYING -> copy(nowPlaying = pin)
        HomeBlock.FAVORITES -> copy(favorites = pin)
    }

    companion object {
        /** The favorites held in the middle of the screen, the widget and Now Playing above them. */
        val Default = HomePins(favorites = BlockPin(PinEdge.CENTER, 0.5f))
    }
}
