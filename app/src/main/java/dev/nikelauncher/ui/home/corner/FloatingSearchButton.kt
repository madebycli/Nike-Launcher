// SPDX-License-Identifier: GPL-3.0-or-later
// Adapted from Victoria Grid (GPL-3.0-or-later), ui/home/FloatingSearchButton.kt.
package dev.nikelauncher.ui.home.corner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.nikelauncher.data.CornerAction
import dev.nikelauncher.data.CornerAppearance
import dev.nikelauncher.data.CornerShape
import dev.nikelauncher.ui.kit.motion.AppMotion

/**
 * The search button in the list's corner. It fades and scales while the list is idle and gives
 * way during a scrub, a scroll or a search, without taking any of those gestures.
 */
@Composable
internal fun FloatingSearchButton(
    visible: Boolean,
    appearance: CornerAppearance,
    overWallpaper: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = remember { MutableTransitionState(false) }
    state.targetState = visible
    val effects = AppMotion.effects<Float>()
    AnimatedVisibility(
        visibleState = state,
        modifier = modifier.zIndex(3f).navigationBarsPadding(),
        enter = fadeIn(effects) + scaleIn(effects, initialScale = 0f),
        exit = fadeOut(effects) + scaleOut(effects, targetScale = 0f),
    ) {
        CornerButtonFace(
            CornerShape.CIRCLE, 56, CornerAction.SEARCH, null, overWallpaper,
            Modifier.padding(20.dp).clip(CircleShape).clickable(enabled = visible, onClick = onClick),
            appearance = appearance,
        )
    }
}
