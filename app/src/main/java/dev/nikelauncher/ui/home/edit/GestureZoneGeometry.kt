// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.edit

import dev.nikelauncher.data.QuickLaunchRegion

/** Screen-space bounds shared by edit-mode annotations and their geometry tests. */
internal data class GestureZoneBounds(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width get() = (right - left).coerceAtLeast(0f)
    val height get() = (bottom - top).coerceAtLeast(0f)
}

internal fun edgeGestureBounds(
    width: Float, height: Float, fromLeft: Boolean, reach: Float, inset: Float,
    bandOnly: Boolean, bandTop: Float, bandHeight: Float, corner: GestureZoneBounds?,
): GestureZoneBounds {
    val left = if (fromLeft) inset else width - inset - reach
    val right = left + reach
    val overlapsCorner = corner != null && if (fromLeft) corner.left < inset + reach else corner.right > width - inset - reach
    val top = if (bandOnly) bandTop.coerceIn(0f, height) else 0f
    val bottom = if (bandOnly) (bandTop + bandHeight).coerceIn(top, height)
        else if (overlapsCorner) corner!!.top.coerceIn(0f, height) else height
    return GestureZoneBounds(left.coerceIn(0f, width), top, right.coerceIn(0f, width), bottom)
}

internal fun quickLaunchBounds(width: Float, height: Float, region: QuickLaunchRegion) = GestureZoneBounds(
    0f, height * (100 - region.bottom - region.height) / 100f,
    width, height * (100 - region.bottom) / 100f,
)
