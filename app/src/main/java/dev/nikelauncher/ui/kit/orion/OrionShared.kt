// SPDX-License-Identifier: GPL-3.0-or-later
/* Adapted from Orion Store, RookieEnough and contributors, GPL-3.0, by way of iris-android
 * (components/orion/OrionPrimitives.kt). Source: Orion e561e91, ui/Settings.kt (tile and page share bounds, PAGE_RESIZE).
 */
@file:OptIn(ExperimentalSharedTransitionApi::class)

package dev.nikelauncher.ui.kit.orion

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale

val LocalOrionShared = staticCompositionLocalOf<SharedTransitionScope?> { null }
val LocalOrionVisibility = compositionLocalOf<AnimatedVisibilityScope?> { null }

/** Makes the shared-transition scopes of one animated destination available to [orionSharedBounds] inside [content]. */
@Composable
fun OrionSharedScreen(shared: SharedTransitionScope, visibility: AnimatedVisibilityScope, content: @Composable () -> Unit) =
    CompositionLocalProvider(LocalOrionShared provides shared, LocalOrionVisibility provides visibility, content = content)

/** Pages are measured once and scaled through the container transform; remeasuring every frame was the jank. */
private val PageResize = SharedTransitionScope.ResizeMode.scaleToBounds(ContentScale.FillWidth, Alignment.TopCenter)

/** The tile with [key] and the page with the same key grow into each other. Outside a shared screen, or with motion off, nothing happens. */
@Composable
fun Modifier.orionSharedBounds(key: String?): Modifier {
    val shared = LocalOrionShared.current
    val visibility = LocalOrionVisibility.current
    if (shared == null || visibility == null || key == null || !orionMotion()) return this
    return with(shared) { this@orionSharedBounds.sharedBounds(rememberSharedContentState(key), visibility, resizeMode = PageResize) }
}
