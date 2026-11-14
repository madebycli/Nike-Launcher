// SPDX-License-Identifier: GPL-3.0-or-later
/* Adapted from Orion Store, RookieEnough and contributors, GPL-3.0, by way of iris-android
 * (components/orion/OrionPrimitives.kt). Source: Orion e561e91, ui/Common.kt (tonal, TonalIcon), ui/Settings.kt (Spinner, accents).
 * Nike changes: reduced motion follows the system animator setting.
 */
package dev.nikelauncher.ui.kit.orion

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.graphics.shapes.RoundedPolygon

/** Accent hues of the hub tiles. Orion ui/Settings.kt and ui/Xtra.kt. */
object OrionAccent {
    val Blue = Color(0xFF3B82F6)
    val Purple = Color(0xFFA855F7)
    val Sky = Color(0xFF38BDF8)
    val Orange = Color(0xFFF97316)
    val Green = Color(0xFF22C55E)
    val Pink = Color(0xFFEC4899)
    val Teal = Color(0xFF14B8A6)
    val Yellow = Color(0xFFEAB308)
    val Indigo = Color(0xFF6366F1)
    val Cyan = Color(0xFF06B6D4)
}

/** Pastel container and legible icon tone of any accent, so fixed hues still read as tonal pairs in light and dark. */
@Composable
fun orionTonal(c: Color): Pair<Color, Color> {
    val light = MaterialTheme.colorScheme.surface.luminance() > .5f
    return remember(c, light) {
        val hsl = FloatArray(3).also { ColorUtils.colorToHSL(c.toArgb(), it) }
        Color.hsl(hsl[0], (hsl[1] * .7f).coerceAtMost(.75f), if (light) .88f else .26f) to
            Color.hsl(hsl[0], hsl[1].coerceAtMost(.8f), if (light) .3f else .82f)
    }
}

/** Decorative MaterialShape that slowly turns; rests when motion is off. The turn is applied in the draw phase only. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OrionSpinner(shape: RoundedPolygon, color: Color, size: Dp, modifier: Modifier = Modifier, periodMs: Int = 24_000) {
    val motion = orionMotion()
    val turn by rememberInfiniteTransition(label = "spin")
        .animateFloat(0f, if (motion) 360f else 0f, infiniteRepeatable(tween(periodMs, easing = LinearEasing)), label = "a")
    val outline = shape.toShape()
    Box(modifier.size(size).graphicsLayer { rotationZ = turn }.background(color, outline))
}

/** The 40dp tonal circle that leads a row. */
@Composable
fun OrionTonalIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.secondaryContainer,
    content: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    shape: Shape = CircleShape,
) {
    Box(modifier.size(40.dp).background(container, shape), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(22.dp), tint = content)
    }
}
