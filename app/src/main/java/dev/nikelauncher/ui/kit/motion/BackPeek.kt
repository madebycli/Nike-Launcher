// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.kit.motion

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch

// Searched Material 3 and ui/kit: M3 sheets follow the back gesture only to close themselves; nothing
// lets a page inside one follow it, so this is the small piece that does.

/** The progress of a back gesture held on a page inside a sheet. */
@Stable
class BackPeek internal constructor() {
    internal val progress = Animatable(0f)
    internal var fromLeft by mutableStateOf(true)
}

/**
 * Lets a page inside a sheet follow the back gesture. While the gesture is held the page shrinks a little
 * and leans away from the edge it started at. Letting go runs [onBack] and the page settles back to its
 * full size as the previous one takes its place; a cancelled gesture springs back the same way.
 */
@Composable
fun rememberBackPeek(enabled: Boolean, onBack: () -> Unit): BackPeek {
    val peek = remember { BackPeek() }
    val scope = rememberCoroutineScope()
    val settle = AppMotion.settle<Float>()
    val latest by rememberUpdatedState(onBack)
    PredictiveBackHandler(enabled) { gesture ->
        try {
            gesture.collect { event ->
                peek.fromLeft = event.swipeEdge == BackEventCompat.EDGE_LEFT
                peek.progress.snapTo(event.progress)
            }
            latest()
            scope.launch { peek.progress.animateTo(0f, settle) }
        } catch (cancelled: CancellationException) {
            scope.launch { peek.progress.animateTo(0f, settle) }
            throw cancelled
        }
    }
    return peek
}

/**
 * Draws [peek] on this page: at most a tenth smaller and 8 dp toward the far edge, clipped to [shape]. While
 * the gesture is held the page is a card in [color] with a soft shadow, so its edge reads against the sheet.
 */
fun Modifier.backPeek(peek: BackPeek, shape: Shape, color: Color): Modifier = graphicsLayer {
    val p = peek.progress.value
    val scale = 1f - 0.1f * p
    scaleX = scale
    scaleY = scale
    translationX = (if (peek.fromLeft) 1f else -1f) * 8.dp.toPx() * p
    shadowElevation = 6.dp.toPx() * p
    this.shape = shape
    clip = p > 0f
}.drawBehind { if (peek.progress.value > 0f) drawRect(color) }
