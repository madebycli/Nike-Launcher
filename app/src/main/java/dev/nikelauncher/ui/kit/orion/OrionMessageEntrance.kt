// SPDX-License-Identifier: GPL-3.0-or-later
/* Adapted from Orion Store, RookieEnough and contributors, GPL-3.0, by way of iris-android
 * (components/orion/OrionPrimitives.kt OrionMessageEntrance). Source: Orion e561e91, ui/AiChat.kt (Entrance).
 */
package dev.nikelauncher.ui.kit.orion

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * A new message springs up into place from its own side ([fromSelf] grows out of the bottom right, the other side out of
 * the bottom left). With [animate] false, or motion off, it simply appears.
 */
@Composable
fun OrionMessageEntrance(fromSelf: Boolean, animate: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val motion = orionMotion()
    val k = remember { Animatable(if (animate && motion) 0f else 1f) }
    val spec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    LaunchedEffect(Unit) { if (animate && motion) k.animateTo(1f, spec) else k.snapTo(1f) }
    Box(modifier.graphicsLayer {
        alpha = k.value.coerceIn(0f, 1f)
        translationY = (1 - k.value) * 32.dp.toPx()
        scaleX = .92f + .08f * k.value
        scaleY = scaleX
        transformOrigin = TransformOrigin(if (fromSelf) 1f else 0f, 1f)
    }) { content() }
}
