// SPDX-License-Identifier: GPL-3.0-or-later
@file:OptIn(ExperimentalSharedTransitionApi::class)

package dev.nikelauncher.ui.settings

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import dev.nikelauncher.ui.kit.motion.AppMotion
import dev.nikelauncher.ui.kit.orion.OrionSharedScreen
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch

/**
 * The hub and one section's page, moved between as iris' settings navigation does.
 *
 * - The hub tile grows into its page and the page shrinks back into the tile (shared bounds); the rest cross-fades.
 * - Back from a page is the system's predictive gesture: held, the page shrinks into its tile step by step; let go, it
 *   finishes; drawn back, the page returns.
 * - Each page keeps its state while the other is shown, like a back stack: the hub comes back scrolled where it was,
 *   with the tile in view to shrink into.
 */
@Composable
internal fun SettingsNavigation(
    open: SettingsSection?,
    onClose: () -> Unit,
    hub: @Composable () -> Unit,
    page: @Composable (SettingsSection) -> Unit,
) {
    val seek = remember { SeekableTransitionState(open) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(open) { if (seek.currentState != open || seek.targetState != open) seek.animateTo(open) }
    PredictiveBackHandler(enabled = open != null) { gesture ->
        val from = open
        try {
            gesture.collect { event -> seek.seekTo(event.progress, targetState = null) }
            onClose()
        } catch (cancelled: CancellationException) {
            scope.launch { seek.animateTo(from) }
            throw cancelled
        }
    }
    val saved = rememberSaveableStateHolder()
    val effects = AppMotion.effects<Float>()
    SharedTransitionLayout {
        rememberTransition(seek, label = "settingsSection").AnimatedContent(
            transitionSpec = { fadeIn(effects) togetherWith fadeOut(effects) },
            contentKey = { it?.name },
        ) { target ->
            saved.SaveableStateProvider(target?.name ?: HUB_KEY) {
                OrionSharedScreen(this@SharedTransitionLayout, this@AnimatedContent) {
                    if (target == null) hub() else page(target)
                }
            }
        }
    }
}

private const val HUB_KEY = "hub"
