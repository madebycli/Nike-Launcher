// SPDX-License-Identifier: GPL-3.0-or-later
// Adapted from Victoria Grid (GPL-3.0-or-later), ui/applist/AppListScreen.kt.
package dev.nikelauncher.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** How strong the halo behind list text is, 0 to 100. Provided by whoever draws the list. */
val LocalTextCloudStrength = compositionLocalOf { 0 }

/** A faint local contrast halo, limited to the text's bounds rather than dimming the wallpaper. */
@Composable
fun Modifier.textContrastCloud(textColor: Color): Modifier {
    val strength = LocalTextCloudStrength.current
    if (strength <= 0) return this
    val cloud = (if (textColor.luminance() > 0.5f) Color.Black else Color.White)
        .copy(alpha = strength.coerceIn(0, 100) / 100f * 0.7f)
    return drawBehind {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(cloud, Color.Transparent),
                center = Offset(size.width / 2, size.height / 2),
                radius = maxOf(size.width * 0.65f, size.height),
            ),
        )
    }
}
