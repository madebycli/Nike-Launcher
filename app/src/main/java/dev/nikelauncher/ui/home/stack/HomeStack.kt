// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.stack

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import dev.nikelauncher.data.HomeBlock
import dev.nikelauncher.data.HomePins

/**
 * Where the stack last put each block, written while it lays out and read by whoever drags one.
 *
 * Drag handlers read the plain measurement arrays directly. A separate geometry snapshot updates
 * the small editor controls when frame positions change; the home route does not observe it.
 */
@Stable
internal class StackMetrics {
    val tops = IntArray(HomeBlock.entries.size)
    val heights = IntArray(HomeBlock.entries.size)
    var viewportPx = 0
    var bottomInsetPx = 0
    var geometry by mutableStateOf<Map<HomeBlock, Pair<Int, Int>>>(emptyMap())

    fun top(block: HomeBlock) = tops[block.ordinal]
    fun height(block: HomeBlock) = heights[block.ordinal]
}

/** Marks a child as one of the blocks. Anything else in the stack is ignored. */
internal fun Modifier.homeBlock(block: HomeBlock): Modifier = layoutId(block)

/**
 * The home screen's blocks, laid out by where each is held rather than by what is above it.
 *
 * Each child is marked with [homeBlock]. [placeStack] decides where it goes and is the only place
 * that does. [pins] is read while measuring, not while composing, so dragging a block in edit mode
 * moves it every frame without recomposing the screen.
 */
@Composable
internal fun HomeStack(
    modifier: Modifier,
    viewportHeightPx: () -> Int,
    bottomInsetPx: () -> Int,
    pins: () -> HomePins,
    metrics: StackMetrics,
    gapDp: Int = 12,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val gapPx = with(density) { gapDp.dp.roundToPx() }
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val loose = Constraints(maxWidth = constraints.maxWidth, maxHeight = Constraints.Infinity)
        val placeables = HashMap<HomeBlock, Placeable>()
        measurables.forEach { child ->
            (child.layoutId as? HomeBlock)?.let { placeables[it] = child.measure(loose) }
        }
        val current = pins()
        val blocks = HomeBlock.entries.mapNotNull { id ->
            placeables[id]?.takeIf { it.height > 0 }?.let { StackBlock(id, it.height, current[id]) }
        }
        val viewport = viewportHeightPx().takeIf { it > 0 }
            ?: constraints.maxHeight.takeIf { it != Constraints.Infinity } ?: 0
        val inset = bottomInsetPx()
        val placement = placeStack(blocks, viewport, inset, gapPx)

        metrics.viewportPx = viewport
        metrics.bottomInsetPx = inset
        HomeBlock.entries.forEach { id ->
            metrics.tops[id.ordinal] = placement.tops[id] ?: 0
            metrics.heights[id.ordinal] = placeables[id]?.height ?: 0
        }

        metrics.geometry = HomeBlock.entries.associateWith { metrics.top(it) to metrics.height(it) }
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth
        else placeables.values.maxOfOrNull { it.width } ?: 0
        layout(width, placement.contentHeightPx.coerceAtLeast(0)) {
            placeables.forEach { (id, placeable) -> placement.tops[id]?.let { placeable.placeRelative(0, it) } }
        }
    }
}
