// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.edit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import dev.nikelauncher.data.PinEdge
import dev.nikelauncher.ui.home.stack.SnapGuide
import dev.nikelauncher.ui.home.stack.SnapGuides
import dev.nikelauncher.ui.kit.motion.AppMotion
import androidx.compose.ui.unit.dp

/** The guides a dragged block can settle on, drawn only while one is being dragged. */
@Composable
internal fun SnapGuideOverlay(
    visible: Boolean,
    active: SnapGuide?,
    viewportPx: () -> Int,
    bottomInsetPx: () -> Int,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val quiet = MaterialTheme.colorScheme.outline
    AnimatedVisibility(visible, modifier, enter = fadeIn(AppMotion.effects()), exit = fadeOut(AppMotion.effects())) {
        Canvas(Modifier.fillMaxSize()) {
            val viewport = viewportPx().toFloat()
            val inset = bottomInsetPx().toFloat()
            val dash = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 8.dp.toPx()))
            SnapGuides.forEach { guide ->
                val y = when (guide.edge) {
                    PinEdge.BOTTOM -> viewport - inset - guide.fraction * viewport
                    else -> guide.fraction * viewport
                }
                if (y < 1f || y > size.height - 1f) return@forEach
                val isActive = guide == active
                drawLine(
                    color = if (isActive) primary else quiet,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = if (isActive) 2.dp.toPx() else 1.dp.toPx(),
                    pathEffect = if (isActive) null else dash,
                )
            }
        }
    }
}

