// SPDX-License-Identifier: GPL-3.0-or-later
// Adapted from AniSync (GPL-3.0-or-later), presentation/components/menu/Menu.kt.
package dev.nikelauncher.ui.kit.menu

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Rows that show at most [maxVisible] at a time and scroll the rest. A sliver of the next row peeks
 * out under them, an edge with rows behind it fades out and a thin bar marks the position, so a list
 * cut short reads as one that scrolls.
 */
@Composable
internal fun ScrollingRows(maxVisible: Int, rows: @Composable () -> Unit) {
    val scrollState = rememberScrollState()
    val barColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    val measurePolicy = remember(maxVisible) { firstRowsMeasurePolicy(maxVisible) }
    Layout(
        content = rows,
        modifier = Modifier
            .scrollHints(scrollState, barColor)
            // The viewport takes the intrinsic height, which only counts the first rows and the peek.
            .height(IntrinsicSize.Max)
            .verticalScroll(scrollState),
        measurePolicy = measurePolicy
    )
}

/**
 * Stacks every row, but reports the height of the first [count], and of a sliver of the next one, as
 * its intrinsic height.
 */
private fun firstRowsMeasurePolicy(count: Int) = object : MeasurePolicy {
    override fun MeasureScope.measure(measurables: List<Measurable>, constraints: Constraints): MeasureResult {
        val placeables = measurables.map { it.measure(constraints.copy(minHeight = 0)) }
        val width = (placeables.maxOfOrNull { it.width } ?: 0)
            .coerceIn(constraints.minWidth, constraints.maxWidth)
        return layout(width, placeables.sumOf { it.height }) {
            var y = 0
            placeables.forEach { placeable ->
                placeable.placeRelative(0, y)
                y += placeable.height
            }
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int) =
        firstRowsHeight(measurables, count) { it.minIntrinsicHeight(width) }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int) =
        firstRowsHeight(measurables, count) { it.maxIntrinsicHeight(width) }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int) =
        measurables.maxOfOrNull { it.minIntrinsicWidth(height) } ?: 0

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int) =
        measurables.maxOfOrNull { it.maxIntrinsicWidth(height) } ?: 0
}

private fun IntrinsicMeasureScope.firstRowsHeight(
    measurables: List<IntrinsicMeasurable>,
    count: Int,
    height: (IntrinsicMeasurable) -> Int,
): Int {
    val shown = measurables.take(count).sumOf(height)
    val next = measurables.getOrNull(count) ?: return shown
    return shown + minOf(NextRowPeek.roundToPx(), height(next))
}

/** Offscreen so the fade masks the rows alone, not the group surface behind them. */
private fun Modifier.scrollHints(state: ScrollState, barColor: Color): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        if (state.maxValue == 0) return@drawWithContent
        val fade = ScrollFadeLength.toPx()
        if (state.canScrollBackward) {
            drawRect(
                brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black), endY = fade),
                size = Size(size.width, fade),
                blendMode = BlendMode.DstIn
            )
        }
        if (state.canScrollForward) {
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color.Black, Color.Transparent),
                    startY = size.height - fade,
                    endY = size.height
                ),
                topLeft = Offset(0f, size.height - fade),
                size = Size(size.width, fade),
                blendMode = BlendMode.DstIn
            )
        }
        val inset = ScrollBarInset.toPx()
        val barWidth = ScrollBarWidth.toPx()
        val track = size.height - inset * 2
        val thumb = (track * size.height / (size.height + state.maxValue))
            .coerceIn(minOf(ScrollBarMinLength.toPx(), track), track)
        val x = if (layoutDirection == LayoutDirection.Ltr) size.width - inset - barWidth else inset
        drawRoundRect(
            color = barColor,
            topLeft = Offset(x, inset + (track - thumb) * state.value / state.maxValue),
            size = Size(barWidth, thumb),
            cornerRadius = CornerRadius(barWidth / 2)
        )
    }

private val ScrollFadeLength = 16.dp

// Taller than the fade, so the top of the next row's text shows before it fades out.
private val NextRowPeek = 28.dp

private val ScrollBarWidth = 4.dp
private val ScrollBarInset = 4.dp
private val ScrollBarMinLength = 24.dp
