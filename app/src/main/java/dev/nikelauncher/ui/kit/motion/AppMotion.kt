// SPDX-License-Identifier: GPL-3.0-or-later
// Adapted from AniSync (GPL-3.0-or-later), presentation/util/AppMotion.kt.
package dev.nikelauncher.ui.kit.motion

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * Memoised access to the Material 3 Expressive motion specs of the current theme.
 *
 * Every animation in the app takes its spec from here (or straight from
 * [MaterialTheme.motionScheme]); there are no private durations or curves.
 *
 * - **Spatial** specs move or resize things and may overshoot.
 * - **Effects** specs fade or tint things and never overshoot.
 * - Each comes as default, fast (small, immediate feedback) and slow (hero moments).
 */
object AppMotion {

    @Composable
    fun spatial(): FiniteAnimationSpec<Float> = scheme { defaultSpatialSpec() }

    @Composable
    fun fastSpatial(): FiniteAnimationSpec<Float> = scheme { fastSpatialSpec() }

    @Composable
    fun slowSpatial(): FiniteAnimationSpec<Float> = scheme { slowSpatialSpec() }

    @Composable
    fun offset(): FiniteAnimationSpec<IntOffset> = scheme { defaultSpatialSpec() }

    @Composable
    fun fastOffset(): FiniteAnimationSpec<IntOffset> = scheme { fastSpatialSpec() }

    @Composable
    fun size(): FiniteAnimationSpec<IntSize> = scheme { defaultSpatialSpec() }

    @Composable
    fun effects(): FiniteAnimationSpec<Float> = scheme { defaultEffectsSpec() }

    @Composable
    fun fastEffects(): FiniteAnimationSpec<Float> = scheme { fastEffectsSpec() }

    @Composable
    fun slowEffects(): FiniteAnimationSpec<Float> = scheme { slowEffectsSpec() }

    @Composable
    fun color(): FiniteAnimationSpec<Color> = scheme { defaultEffectsSpec() }

    @Composable
    fun fastColor(): FiniteAnimationSpec<Color> = scheme { fastEffectsSpec() }

    @Composable
    private inline fun <T> scheme(crossinline pick: androidx.compose.material3.MotionScheme.() -> T): T {
        val motionScheme = MaterialTheme.motionScheme
        return remember(motionScheme) { motionScheme.pick() }
    }
}
