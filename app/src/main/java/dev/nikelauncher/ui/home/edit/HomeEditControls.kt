// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.edit

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import dev.nikelauncher.data.HomeBlock
import dev.nikelauncher.ui.home.*
import dev.nikelauncher.ui.home.stack.StackMetrics
import kotlin.math.roundToInt

/** Single- and multi-frame controls share one selection and one atomic layout draft. */
@Composable
internal fun BoxScope.HomeEditControls(editing: Boolean, editor: PinEditor, metrics: StackMetrics,
    settings: HomeSettings, data: HomeData, actions: HomeActions, available: Set<HomeBlock>, done: () -> Unit) {
    val density = LocalDensity.current
    var showSettings by remember { mutableStateOf(false) }
    fun edge(block: HomeBlock) = when (block) {
        HomeBlock.WIDGET -> settings.widgetSidePaddingDp
        HomeBlock.NOW_PLAYING -> settings.mediaSidePaddingDp
        HomeBlock.FAVORITES -> settings.sidePaddingDp
    }
    val geometry = metrics.geometry
    val targets = editor.selection
    val single = targets.singleOrNull()
    Column(Modifier.align(Alignment.BottomCenter), horizontalAlignment = Alignment.CenterHorizontally) {
        NudgePanel(editing, targets.takeIf { it.isNotEmpty() }?.let {
            NudgeState(
                topDp = with(density) { targets.minOf { geometry[it]?.first ?: metrics.top(it) }.toDp().value.roundToInt() },
                offsetDp = targets.map { data.blockOffsets[it] }.distinct().singleOrNull(),
                edgeDp = targets.map(::edge).distinct().singleOrNull(),
                onMove = { dx, dy ->
                    if (dx != 0) targets.forEach { actions.layout.onNudgeX(it, dx) }
                    if (dy != 0) editor.nudgeSelection(with(density) { dy.dp.toPx() })
                },
                onEdge = { delta -> targets.forEach { actions.layout.onNudgeEdge(it, delta) } },
            )
        })
        EditToolbar(editing, EditToolbarActions(
            onDone = done,
            onReset = { editor.resetSelected(); targets.ifEmpty { available }.forEach(actions.layout.onResetOffset) },
            onSmaller = when (single) {
                HomeBlock.WIDGET -> { { actions.widgets.onResize((data.widgetHeightDp - 20).coerceAtLeast(80)) } }
                HomeBlock.NOW_PLAYING -> { { actions.layout.onResizeNowPlaying((settings.nowPlayingHeightDp - 8).coerceAtLeast(48)) } }
                else -> null
            },
            onLarger = when (single) {
                HomeBlock.WIDGET -> { { actions.widgets.onResize((data.widgetHeightDp + 20).coerceAtMost(900)) } }
                HomeBlock.NOW_PLAYING -> { { actions.layout.onResizeNowPlaying((settings.nowPlayingHeightDp + 8).coerceAtMost(220)) } }
                else -> null
            },
            onBandRange = actions.layout.onEditScrubBand.takeIf { single == HomeBlock.FAVORITES },
            onSettings = { showSettings = true },
            onSelectAll = editor::selectAll,
            onAddWidget = actions.widgets.onAddWidget.takeIf { HomeBlock.WIDGET !in available },
        ))
    }
    if (editing && showSettings) HomeLayoutSheet(
        settings, available, targets, edge = ::edge, onToggle = editor::select,
        onGap = { dp -> actions.layout.onSetBlockGap(dp); editor.setGap(with(density) { dp.dp.roundToPx() }) },
        onRows = actions.layout.onSetRowSpacing,
        onEdge = { dp -> actions.layout.onSetEdges(targets.ifEmpty { available }, dp) },
        onDismiss = { showSettings = false },
    )
}
