// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.wallpaper

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import kotlin.random.Random

/** Fixed noise is generated only when the viewport changes, not on each frame. */
@Composable
internal fun WallpaperGrain(strength: Int) {
    if (strength <= 0) return
    Canvas(Modifier.fillMaxSize().drawWithCache {
        val random = Random(0x5647)
        val count = (size.width * size.height / 350f).toInt().coerceIn(400, 6000)
        val light = List(count) { Offset(random.nextFloat() * size.width, random.nextFloat() * size.height) }
        val dark = List(count) { Offset(random.nextFloat() * size.width, random.nextFloat() * size.height) }
        val opacity = strength.coerceIn(0, 100) / 100f * .24f
        onDrawBehind {
            drawPoints(light, PointMode.Points, Color.White.copy(alpha = opacity), strokeWidth = 1.5f)
            drawPoints(dark, PointMode.Points, Color.Black.copy(alpha = opacity), strokeWidth = 1.5f)
        }
    }) { }
}
