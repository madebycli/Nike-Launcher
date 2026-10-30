// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.pages

import kotlin.math.roundToInt

internal data class GestureExclusionBand(val top: Int, val bottom: Int)

/** Rect's bottom is exclusive. Offscreen grips must never reserve a screen edge. */
internal fun resizeGripExclusionBand(centerY: Float, viewportHeight: Int, touchSize: Int): GestureExclusionBand? {
    if (!centerY.isFinite() || viewportHeight <= 0 || touchSize <= 0) return null
    val top = (centerY - touchSize / 2f).roundToInt().coerceIn(0, viewportHeight)
    val bottom = (centerY + touchSize / 2f).roundToInt().coerceIn(0, viewportHeight)
    return if (bottom > top) GestureExclusionBand(top, bottom) else null
}
