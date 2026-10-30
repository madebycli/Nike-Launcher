// SPDX-License-Identifier: GPL-3.0-or-later
// Adapted from AniSync (GPL-3.0-or-later), presentation/util/MotionUtils.kt.
package dev.nikelauncher.ui.kit.motion

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ripple
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics

/**
 * A click target that shrinks slightly while pressed, on the theme's spatial spring. For content
 * that is not itself a Material component; Material buttons and list items already animate.
 */
fun Modifier.bouncyClickable(
    enabled: Boolean = true,
    pressedScale: Float = MotionConstants.PRESSED_SCALE,
    role: Role? = Role.Button,
    onClick: () -> Unit,
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = AppMotion.spatial(),
        label = "bouncyScale",
    )
    this
        .semantics { role?.let { this.role = it } }
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = ripple(),
            enabled = enabled,
            onClick = onClick,
        )
}
