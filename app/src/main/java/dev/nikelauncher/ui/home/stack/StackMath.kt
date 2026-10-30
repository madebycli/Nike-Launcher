// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.stack

import dev.nikelauncher.data.BlockPin
import dev.nikelauncher.data.HomeBlock
import dev.nikelauncher.data.PinEdge
import kotlin.math.abs
import kotlin.math.roundToInt

/** One block as the stack sees it: how tall it is and where it is held, if anywhere. */
internal data class StackBlock(val id: HomeBlock, val heightPx: Int, val pin: BlockPin?)

internal data class StackPlacement(
    val tops: Map<HomeBlock, Int>,
    /** Where the last block ends: how far the stack can be scrolled. */
    val contentHeightPx: Int,
)

/**
 * Where a pin asks for a block's top edge to be, before the visible field is clamped to the viewport.
 *
 * Bottom pins count from the usable bottom, above the navigation bar, so a block held "a thumb
 * above the bottom" stays reachable whatever the device draws down there.
 */
internal fun pinnedTopPx(pin: BlockPin, heightPx: Int, viewportPx: Int, bottomInsetPx: Int): Int {
    val fraction = pin.fraction.coerceIn(BlockPin.MIN_FRACTION, BlockPin.MAX_FRACTION)
    return when (pin.edge) {
        PinEdge.TOP -> (fraction * viewportPx).roundToInt()
        PinEdge.CENTER -> (fraction * viewportPx - heightPx / 2f).roundToInt()
        PinEdge.BOTTOM -> (viewportPx - bottomInsetPx - fraction * viewportPx - heightPx).roundToInt()
    }
}

/** The pin that holds a block at [topPx] by [edge]: the inverse of [pinnedTopPx]. */
internal fun pinForTop(edge: PinEdge, topPx: Int, heightPx: Int, viewportPx: Int, bottomInsetPx: Int): BlockPin {
    if (viewportPx <= 0) return BlockPin(edge, 0f)
    val fraction = when (edge) {
        PinEdge.TOP -> topPx.toFloat() / viewportPx
        PinEdge.CENTER -> (topPx + heightPx / 2f) / viewportPx
        PinEdge.BOTTOM -> (viewportPx - bottomInsetPx - topPx - heightPx).toFloat() / viewportPx
    }
    return BlockPin(edge, fraction.coerceIn(BlockPin.MIN_FRACTION, BlockPin.MAX_FRACTION))
}

/** Keep a pinned field visible; only genuinely oversized content needs ordinary scrolling. */
internal fun fittedTop(top: Int, height: Int, viewport: Int, inset: Int): Int =
    top.coerceIn(0, (viewport - inset - height).coerceAtLeast(0))

/** Pins are authoritative. Unpinned frames flow after the preceding reserved frame. */
internal fun placeStack(blocks: List<StackBlock>, viewportPx: Int, bottomInsetPx: Int, gapPx: Int): StackPlacement {
    val tops = LinkedHashMap<HomeBlock, Int>()
    var previousEnd = 0
    var contentEnd = 0
    for ((index, block) in blocks.withIndex()) {
        val followingTop = if (index == 0) 0 else previousEnd + gapPx
        val top = block.pin?.let { fittedTop(pinnedTopPx(it, block.heightPx, viewportPx, bottomInsetPx), block.heightPx, viewportPx, bottomInsetPx) }
            ?: followingTop.coerceAtLeast(0)
        tops[block.id] = top
        previousEnd = top + block.heightPx
        contentEnd = maxOf(contentEnd, previousEnd)
    }
    return StackPlacement(tops, contentEnd + if (blocks.isEmpty()) 0 else bottomInsetPx)
}

/** How near a dragged block has to come to a guide before it lets go of the finger and sits on it. */
internal const val SNAP_DISTANCE_DP = 10

/** A place a dragged block settles on: this part of the block at this share of the screen. */
internal data class SnapGuide(val edge: PinEdge, val fraction: Float)

/** The guides offered while dragging. The middle is the one that matters; the rest give the eye a grid. */
internal val SnapGuides: List<SnapGuide> = listOf(
    SnapGuide(PinEdge.TOP, 0f),
    SnapGuide(PinEdge.TOP, 0.25f),
    SnapGuide(PinEdge.CENTER, 0.5f),
    SnapGuide(PinEdge.TOP, 0.5f),
    SnapGuide(PinEdge.TOP, 0.75f),
    SnapGuide(PinEdge.BOTTOM, 0.03f),
)

/**
 * Settles a dragged block onto a guide when one is within reach of it, otherwise holds it by its
 * top edge exactly where it was left.
 */
internal fun snapped(
    topPx: Int,
    heightPx: Int,
    viewportPx: Int,
    bottomInsetPx: Int,
    snapDistancePx: Int,
): Pair<BlockPin, SnapGuide?> {
    val best = SnapGuides
        .map { it to abs(pinnedTopPx(BlockPin(it.edge, it.fraction), heightPx, viewportPx, bottomInsetPx) - topPx) }
        .filter { (_, distance) -> distance <= snapDistancePx }
        .minByOrNull { (_, distance) -> distance }
        ?.first
    return if (best != null) BlockPin(best.edge, best.fraction) to best
    else pinForTop(PinEdge.TOP, topPx, heightPx, viewportPx, bottomInsetPx) to null
}
