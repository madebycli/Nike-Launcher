// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.data

/** Fractions of the usable travel area, so the complete button stays inside the viewport. */
data class CornerPosition(val x: Float, val y: Float) : java.io.Serializable

internal fun cornerPosition(x: Float?, y: Float?): CornerPosition? =
    if (x == null || y == null || !x.isFinite() || !y.isFinite()) null
    else CornerPosition(x.coerceIn(0f, 1f), y.coerceIn(0f, 1f))
