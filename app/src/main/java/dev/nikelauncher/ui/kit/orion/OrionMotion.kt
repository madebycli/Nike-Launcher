// SPDX-License-Identifier: GPL-3.0-or-later
/* Adapted from Orion Store, RookieEnough and contributors, GPL-3.0, by way of iris-android
 * (components/orion/OrionPrimitives.kt). Source: Orion e561e91, ui/Common.kt (springClick, focusRing, reveal).
 * Nike changes: reduced motion follows the system animator setting; shapes come from the theme.
 */
package dev.nikelauncher.ui.kit.orion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import dev.nikelauncher.ui.kit.motion.motionEnabled
import kotlinx.coroutines.delay

/** Whether this composition should move; read once per composition, it is the system's animator switch. */
@Composable
internal fun orionMotion(): Boolean = remember { motionEnabled() }

/** Squash-on-press with an expressive spatial spring, plus ripple and a tick, clipped to [shape]. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.orionSpringClick(
    shape: Shape = MaterialTheme.shapes.large,
    onLongClick: (() -> Unit)? = null,
    src: MutableInteractionSource = remember { MutableInteractionSource() },
    onClick: () -> Unit,
): Modifier {
    val pressed by src.collectIsPressedAsState()
    val focused by src.collectIsFocusedAsState()
    val haptic = LocalHapticFeedback.current
    val spec = if (orionMotion()) MaterialTheme.motionScheme.fastSpatialSpec<Float>() else snap()
    val k by animateFloatAsState(if (pressed) .94f else if (focused) 1.04f else 1f, spec, label = "press")
    return graphicsLayer { scaleX = k; scaleY = k }
        .orionFocusRing(src, shape)
        .clip(shape)
        .combinedClickable(src, ripple(), onLongClick = onLongClick, onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        })
}

/** A primary ring around whatever has D-pad or keyboard focus. Touch never focuses, so phones never see it. */
@Composable
fun Modifier.orionFocusRing(src: InteractionSource, shape: Shape): Modifier {
    val focused by src.collectIsFocusedAsState()
    val w by animateDpAsState(if (focused) 3.dp else 0.dp, MaterialTheme.motionScheme.fastSpatialSpec(), label = "ring")
    return if (w > 0.dp) border(w, MaterialTheme.colorScheme.primary, shape) else this
}

/**
 * Rise-and-fade entrance for the first screenful, once per item (survives lazy recycling), staggered by [index].
 * Items further down appear as they are, so flinging never shows rows sliding in.
 */
@Composable
fun Modifier.orionReveal(index: Int = 0, enabled: Boolean = true): Modifier {
    if (index > 8 || !enabled || !orionMotion()) return this
    var done by rememberSaveable { mutableStateOf(false) }
    val p = remember { Animatable(if (done) 1f else 0f) }
    val spec = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
    LaunchedEffect(Unit) {
        if (!done) {
            delay(index.coerceIn(0, 8) * 45L)
            p.animateTo(1f, spec)
            done = true
        }
    }
    return graphicsLayer {
        alpha = p.value.coerceIn(0f, 1f)
        translationY = (1f - p.value) * 28.dp.toPx()
    }
}
