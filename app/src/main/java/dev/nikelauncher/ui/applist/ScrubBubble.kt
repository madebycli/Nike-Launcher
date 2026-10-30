// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.applist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nikelauncher.data.EdgeSide
import dev.nikelauncher.data.ScrubBubbleShape
import kotlin.math.roundToInt

/** Bubble for the current letter, dragged out from the strip and springing back. */
@Composable
internal fun BoxScope.ScrubBubble(
    letter: Char,
    shape: ScrubBubbleShape,
    side: EdgeSide,
    scrubY: () -> Float?,
    pullPx: () -> Float,
) {
    val bubble = 72.dp
    val density = LocalDensity.current
    val halfPx = with(density) { (bubble / 2).toPx() }
    val insetPx = with(density) { SCRUB_BUBBLE_INSET_DP.dp.toPx() }
    Surface(
        color = Color.Black.copy(alpha = 0.6f),
        shape = when (shape) {
            ScrubBubbleShape.CIRCLE -> CircleShape
            ScrubBubbleShape.SQUARE -> RectangleShape
            else -> MaterialTheme.shapes.extraLarge
        },
        modifier = Modifier
            .align(if (side == EdgeSide.LEFT) Alignment.TopStart else Alignment.TopEnd)
            .offset {
                val x = insetPx + pullPx()
                IntOffset(
                    x = if (side == EdgeSide.LEFT) x.roundToInt() else -x.roundToInt(),
                    y = ((scrubY() ?: 0f) - halfPx).roundToInt(),
                )
            }
            .size(bubble),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(letter.toString(), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        }
    }
}
