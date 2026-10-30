// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.route

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.nikelauncher.ui.home.HomeSettings
import dev.nikelauncher.ui.home.wallpaper.AppListWallpaperEffect
import dev.nikelauncher.ui.home.wallpaper.WallpaperGrain
import dev.nikelauncher.ui.kit.motion.AppMotion

private const val HOME_FADE_MS = 220

/**
 * Everything drawn over the wallpaper and under both screens: the blur or picture the list brings
 * in, the dim, and the grain. Belongs to neither screen, so moving between them has nothing to
 * hand over.
 */
@Composable
internal fun WallpaperLayers(
    settings: HomeSettings,
    listVisible: Boolean,
    editing: Boolean,
    homeDim: Float,
    snapNow: Boolean,
    /** How much a widget page dims the wallpaper, or null while the home screen is showing. */
    sidePageDim: Float? = null,
) {
    val wallpaperDim by animateFloatAsState(
        targetValue = when {
            editing -> 0f
            listVisible -> settings.dimWallpaperAlpha
            sidePageDim != null -> sidePageDim
            else -> homeDim
        },
        animationSpec = if (snapNow) snap() else tween(HOME_FADE_MS, easing = LinearOutSlowInEasing),
        label = "wallpaperDim",
    )
    // The compositor's blur is a window setting that arrives all at once, so it is walked up and
    // down: the wallpaper softens as the list comes in and clears as it goes.
    val blurLevel by animateFloatAsState(
        targetValue = if (listVisible && !editing) 1f else 0f,
        animationSpec = AppMotion.effects(),
        label = "systemBlurLevel",
    )
    AppListWallpaperEffect(
        effect = settings.appListBackgroundEffect,
        strength = settings.appListBackgroundStrength,
        image = settings.appListBackgroundImage,
        edgesOnly = settings.appListBackgroundEdges,
        visible = listVisible && !editing,
        blurLevel = blurLevel,
    )
    if (wallpaperDim > 0f) Box(Modifier.fillMaxSize().background(Color(if (listVisible) settings.appListTint else settings.dimColor).copy(alpha = wallpaperDim)))
    if (listVisible && !editing) WallpaperGrain(settings.appListGrain)
}
