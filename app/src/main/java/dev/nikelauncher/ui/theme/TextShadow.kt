// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.luminance

/**
 * The shadow to put behind the launcher's own text, or null for none.
 *
 * Only behind light text: a shadow under dark text on a pale wallpaper muddies the letters
 * instead of separating them. The setting asks for contrast, and this is where it is decided
 * whether a shadow would actually gain any.
 */
@Composable
internal fun launcherTextShadow(enabled: Boolean, textColor: Color, strength: Int = DEFAULT_SHADOW_STRENGTH): Shadow? {
    if (!enabled) return null
    if (textColor.luminance() < 0.5f) return null
    // One number moving darkness and spread together. Five is what it always was, so the
    // default reads exactly as before; either side of it is darker and wider, or lighter and
    // tighter, which is the one thing anyone adjusting it is after.
    val k = strength.coerceIn(SHADOW_STRENGTH_RANGE) / DEFAULT_SHADOW_STRENGTH.toFloat()
    return Shadow(
        color = Color.Black.copy(alpha = (0.55f * k).coerceIn(0.1f, 0.95f)),
        offset = Offset(0f, 1f),
        blurRadius = 4f * k,
    )
}

const val DEFAULT_SHADOW_STRENGTH = 5
val SHADOW_STRENGTH_RANGE = 1..10
