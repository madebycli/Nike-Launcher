// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.route

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.nikelauncher.ui.applist.ScrubBand

/**
 * Which part of the screen the A-Z strip occupies, and the editing of it.
 *
 * While the band is being edited the live value wins; otherwise a range the user set by hand wins
 * over the measured favorites, which is what makes it stop following them.
 */
@Stable
internal class BandEditing(
    private val stored: () -> Pair<Float, Float>?,
    private val viewportPx: () -> Int,
    private val save: (top: Float, height: Float) -> Unit,
) {
    var editing by mutableStateOf(false)
        private set
    var live by mutableStateOf<ScrubBand?>(null)
    var favoritesBand by mutableStateOf<ScrubBand?>(null)

    /**
     * Clamped on the way back in as well as on the way out: a range stored from a bad measurement
     * would otherwise put the strip off screen for good, with no gesture left to fix it.
     */
    private fun storedBand(): ScrubBand? {
        val viewport = viewportPx()
        return stored()
            ?.takeIf { (top, height) -> top.isFinite() && height.isFinite() && height > 0f }
            ?.let { (top, height) ->
                val bandHeight = (viewport * height).coerceIn(0f, viewport.toFloat())
                val bandTop = (viewport * top).coerceIn(0f, (viewport - bandHeight).coerceAtLeast(0f))
                ScrubBand(topPx = bandTop, heightPx = bandHeight)
            }
    }

    val band: ScrubBand
        get() = live ?: storedBand() ?: favoritesBand ?: ScrubBand.fallbackFor(viewportPx())

    fun start() {
        live = band
        editing = true
    }

    fun commit() {
        live?.let { edited ->
            val viewport = viewportPx()
            if (viewport > 0) save(edited.topPx / viewport, edited.heightPx / viewport)
        }
        live = null
        editing = false
    }
}
