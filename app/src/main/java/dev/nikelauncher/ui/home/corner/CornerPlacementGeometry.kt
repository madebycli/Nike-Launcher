// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.corner

import dev.nikelauncher.data.CornerPosition
import kotlin.math.roundToInt

internal data class CornerTravel(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width get() = (right - left).coerceAtLeast(0)
    val height get() = (bottom - top).coerceAtLeast(0)
    fun location(position: CornerPosition) = (left + width * position.x).roundToInt() to (top + height * position.y).roundToInt()
    fun position(x: Float, y: Float) = CornerPosition(
        if (width == 0) 0f else ((x - left) / width).coerceIn(0f, 1f),
        if (height == 0) 0f else ((y - top) / height).coerceIn(0f, 1f))
}

internal fun cornerTravel(width: Int, height: Int, button: Int, side: Int, top: Int, bottom: Int) =
    CornerTravel(side, top, (width - button - side).coerceAtLeast(side), (height - button - bottom).coerceAtLeast(top))
