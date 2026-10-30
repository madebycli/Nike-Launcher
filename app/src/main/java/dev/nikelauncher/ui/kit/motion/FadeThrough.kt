// SPDX-License-Identifier: GPL-3.0-or-later
// Adapted from AniSync (GPL-3.0-or-later), presentation/util/FadeThrough.kt.
package dev.nikelauncher.ui.kit.motion

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable

/**
 * Material fade-through: the outgoing content fades away, the incoming content fades in while it
 * scales up from 92 %. For swapping content that has no spatial relationship to what it replaces;
 * use a shared axis when there is a direction.
 */
object FadeThrough {
    private const val INITIAL_SCALE = 0.92f

    @Composable
    fun enter(): EnterTransition =
        fadeIn(AppMotion.effects()) + scaleIn(AppMotion.spatial(), initialScale = INITIAL_SCALE)

    /** The exit only fades, so it does not compete with the entering content. */
    @Composable
    fun exit(): ExitTransition = fadeOut(AppMotion.effects())

    @Composable
    fun contentTransform(): ContentTransform = enter() togetherWith exit()

    @Composable
    fun fastContentTransform(): ContentTransform =
        (fadeIn(AppMotion.fastEffects()) + scaleIn(AppMotion.fastSpatial(), initialScale = INITIAL_SCALE)) togetherWith
            fadeOut(AppMotion.fastEffects())
}
