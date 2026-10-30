// SPDX-License-Identifier: GPL-3.0-or-later
// Adapted from Victoria Grid (GPL-3.0-or-later), ui/home/CornerButtonFace.kt.
package dev.nikelauncher.ui.home.corner

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.nikelauncher.data.AppInfo
import dev.nikelauncher.data.CornerAction
import dev.nikelauncher.data.CornerAppearance
import dev.nikelauncher.data.CornerColorMode
import dev.nikelauncher.data.CornerShape
import dev.nikelauncher.ui.common.AppIcon

/**
 * The corner button itself, drawn from its settings. Shared with the settings sheet, which shows
 * what the choices will look like.
 *
 * [overWallpaper] is the color that reads on whatever is behind it, for the shapes that have no
 * surface of their own.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun CornerButtonFace(
    shape: CornerShape,
    sizeDp: Int,
    action: CornerAction,
    app: AppInfo?,
    overWallpaper: Color,
    modifier: Modifier = Modifier,
    appearance: CornerAppearance = CornerAppearance(),
) {
    val scheme = MaterialTheme.colorScheme
    val outline = shape == CornerShape.OUTLINE
    val minimal = shape == CornerShape.MINIMAL
    val classicContainer = when (shape) {
        CornerShape.CIRCLE -> scheme.primaryContainer
        CornerShape.SQUIRCLE -> scheme.secondaryContainer
        CornerShape.OUTLINE -> scheme.surface.copy(alpha = .88f)
        CornerShape.MINIMAL -> Color.Transparent
    }
    val classicForeground = when (shape) {
        CornerShape.CIRCLE -> scheme.onPrimaryContainer
        CornerShape.SQUIRCLE -> scheme.onSecondaryContainer
        else -> overWallpaper
    }
    val baseContainer = when (appearance.colorMode) {
        CornerColorMode.CLASSIC -> classicContainer
        CornerColorMode.MATERIAL -> scheme.primaryContainer
        CornerColorMode.CUSTOM -> Color(appearance.background)
    }
    // A transparent button has nothing to contrast with but the wallpaper.
    val transparentAutomatic = appearance.backgroundOpacity < 40 &&
        (appearance.colorMode != CornerColorMode.CUSTOM || appearance.automaticForeground)
    val iconColor = if (transparentAutomatic) overWallpaper else when (appearance.colorMode) {
        CornerColorMode.CLASSIC -> classicForeground
        CornerColorMode.MATERIAL -> if (minimal) overWallpaper else scheme.onPrimaryContainer
        CornerColorMode.CUSTOM -> when {
            appearance.automaticForeground ->
                if (minimal) overWallpaper else if (baseContainer.luminance() > .179f) Color.Black else Color.White
            else -> Color(appearance.foreground)
        }
    }
    val container by animateColorAsState(
        if (minimal) Color.Transparent else baseContainer.copy(alpha = baseContainer.alpha * appearance.backgroundOpacity.coerceIn(0, 100) / 100f),
        label = "cornerContainer",
    )
    val foreground by animateColorAsState(iconColor, label = "cornerIcon")
    Surface(
        modifier = modifier.size(sizeDp.dp),
        shape = if (shape == CornerShape.SQUIRCLE) MaterialShapes.Square.toShape() else CircleShape,
        color = container,
        contentColor = foreground,
        border = if (outline) BorderStroke(1.dp, foreground.copy(alpha = .65f)) else null,
        // A transparent face must not leave an opaque elevation shadow behind.
        shadowElevation = if (minimal || outline || appearance.backgroundOpacity < 40) 0.dp else 6.dp,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (action == CornerAction.APP && app != null) {
                AppIcon(app, (sizeDp * .5f).toInt())
            } else {
                Icon(action.icon, stringResource(action.label), Modifier.size((sizeDp * .44f).coerceIn(22f, 30f).dp))
            }
        }
    }
}
