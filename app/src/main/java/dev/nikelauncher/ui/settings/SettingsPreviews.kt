// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.settings

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

/**
 * What adjusting a setting briefly paints over the settings screen, because a number means little
 * until it is seen against the screen it is measured on: the edge zones, the dim scrim and the
 * line a scrubbed letter comes to rest on.
 *
 * Each preview is driven by the act of adjusting, not by the value, so opening the screen and the
 * stored values arriving never flash one nobody asked for.
 */
@Stable
class SettingsPreviews {
    var edgeTick by mutableIntStateOf(0)
        private set
    var dimTick by mutableIntStateOf(0)
        private set
    var sectionTick by mutableIntStateOf(0)
        private set

    /** The dim being adjusted, since the home and list dims are separate and each is only true for its own screen. */
    var dimAlpha by mutableFloatStateOf(0f)
        private set

    fun showEdge() {
        edgeTick++
    }

    fun showDim(alpha: Float) {
        dimAlpha = alpha
        dimTick++
    }

    fun showSection() {
        sectionTick++
    }
}
