// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** Choose among Material surfaces by actual contrast, independent of the theme's light/dark role. */
internal fun previewPlate(text: Color, surfaces: List<Color>): Color = surfaces.maxBy { surface ->
    val a = text.luminance()
    val b = surface.luminance()
    (maxOf(a, b) + .05f) / (minOf(a, b) + .05f)
}
