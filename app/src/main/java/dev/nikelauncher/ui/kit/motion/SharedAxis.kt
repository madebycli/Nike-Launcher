// SPDX-License-Identifier: GPL-3.0-or-later
// Adapted from AniSync (GPL-3.0-or-later), presentation/settings/SettingsListDetail.kt.
package dev.nikelauncher.ui.kit.motion

import androidx.compose.animation.*
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.ui.unit.IntOffset

/** The reference's full-container forward/back slide and fade, using the active Material specs. */
fun <T> AnimatedContentTransitionScope<T>.sharedAxisEnter(forward: Boolean,
    spatial: FiniteAnimationSpec<IntOffset>, effects: FiniteAnimationSpec<Float>): EnterTransition =
    slideIntoContainer(if (forward) AnimatedContentTransitionScope.SlideDirection.Start else AnimatedContentTransitionScope.SlideDirection.End,
        animationSpec = spatial) + fadeIn(effects)

fun <T> AnimatedContentTransitionScope<T>.sharedAxisExit(forward: Boolean,
    spatial: FiniteAnimationSpec<IntOffset>, effects: FiniteAnimationSpec<Float>): ExitTransition =
    slideOutOfContainer(if (forward) AnimatedContentTransitionScope.SlideDirection.Start else AnimatedContentTransitionScope.SlideDirection.End,
        animationSpec = spatial) + fadeOut(effects)
