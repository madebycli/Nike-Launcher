// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.data

/** The band of the screen where a sideways swipe launches an app, as percentages of its height. */
data class QuickLaunchRegion(val bottomPercent: Int = 0, val heightPercent: Int = 18) {
    val bottom get() = bottomPercent.coerceIn(BOTTOM_RANGE)

    val height get() = heightPercent.coerceIn(HEIGHT_RANGE).coerceAtMost(95 - bottom)

    fun contains(y: Float, viewport: Float): Boolean =
        viewport > 0f && y.isFinite() &&
            y >= viewport * (100 - bottom - height) / 100f &&
            y < viewport * (100 - bottom) / 100f

    companion object {
        val BOTTOM_RANGE = 0..85
        val HEIGHT_RANGE = 5..60
    }
}
