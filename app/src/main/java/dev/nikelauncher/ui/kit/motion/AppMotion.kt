// SPDX-License-Identifier: GPL-3.0-or-later
// Adapted from AniSync (GPL-3.0-or-later), presentation/util/AppMotion.kt.
package dev.nikelauncher.ui.kit.motion

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
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
 * - **Spatial** specs move or resize things and may overshoot; [size] is the one that never does.
 * - **Effects** specs fade or tint things and never overshoot.
 * - Each comes as default, fast (small, immediate feedback) and slow (hero moments).
 */
object AppMotion {

    @Composable
    fun <T> spatial(): FiniteAnimationSpec<T> = scheme { defaultSpatialSpec<T>() }

    @Composable
    fun <T> fastSpatial(): FiniteAnimationSpec<T> = scheme { fastSpatialSpec<T>() }

    @Composable
    fun <T> slowSpatial(): FiniteAnimationSpec<T> = scheme { slowSpatialSpec<T>() }

    @Composable
    fun offset(): FiniteAnimationSpec<IntOffset> = scheme { defaultSpatialSpec() }

    @Composable
    fun fastOffset(): FiniteAnimationSpec<IntOffset> = scheme { fastSpatialSpec() }
    /**
     * Resizing a container whose edge stands over other content, like a sheet growing with its page. Uses
     * Material's standard spatial spring, which does not overshoot: an edge that dips past its content and
     * springs back reads as a wobble, not as motion.
     */
    /** A value settling back after a gesture let go, like a page returning to full size: fast, no overshoot. */
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    fun <T> settle(): FiniteAnimationSpec<T> = remember { MotionScheme.standard().fastSpatialSpec<T>() }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    fun size(): FiniteAnimationSpec<IntSize> = remember { MotionScheme.standard().defaultSpatialSpec() }

    @Composable
    fun <T> effects(): FiniteAnimationSpec<T> = scheme { defaultEffectsSpec<T>() }

    @Composable
    fun <T> fastEffects(): FiniteAnimationSpec<T> = scheme { fastEffectsSpec<T>() }

    @Composable
    fun <T> slowEffects(): FiniteAnimationSpec<T> = scheme { slowEffectsSpec<T>() }

    /** For a hero moment that settles slowly and may overshoot. */
    @Composable
    fun <T> emphasized(): FiniteAnimationSpec<T> = scheme { slowSpatialSpec<T>() }

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
