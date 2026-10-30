// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import dev.nikelauncher.data.AppFont

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7FD1E0),
    onPrimary = Color(0xFF00363F),
    surface = Color(0xFF1B2733),
    onSurface = Color(0xFFE3E8EC),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF10707F),
    surface = Color(0xFFF3F6F8),
    onSurface = Color(0xFF1B2733),
)

/**
 * The app theme: Material 3 Expressive with its stock shapes, motion scheme and type scale.
 *
 * Nothing here restyles a component. Colours follow the wallpaper on Android 12+; the background
 * stays transparent because the home screen lets the real wallpaper show through.
 */
@Composable
fun NikeTheme(
    font: AppFont = AppFont.SYSTEM,
    fontFile: String? = null,
    content: @Composable () -> Unit,
) {
    val darkTheme = isSystemInDarkTheme()
    val context = LocalContext.current
    val colors = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (darkTheme) DarkColors else LightColors
    }.copy(background = Color.Transparent)
    val typography = remember(font, fontFile) { nikeTypography(fontFamilyOf(font, fontFile)) }
    MaterialTheme(
        colorScheme = colors,
        typography = typography,
        shapes = Shapes(),
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}
