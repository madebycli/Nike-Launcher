// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.pages

import android.appwidget.AppWidgetManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.nikelauncher.R
import dev.nikelauncher.ui.home.pages.components.WidgetGridToolbar
import dev.nikelauncher.data.GridWidget
import dev.nikelauncher.data.WidgetPageSide
import dev.nikelauncher.data.moveGridWidget
import dev.nikelauncher.widget.WidgetPage
import dev.nikelauncher.widget.WidgetSettingsButton

/** Widgets over the wallpaper. Controls exist only while the user is editing. */
@Composable
internal fun WidgetGridPage(
    side: WidgetPageSide,
    widgets: List<GridWidget>,
    editing: Boolean,
    active: Boolean,
    gestureExclusions: PageGestureExclusions,
    onEdit: () -> Unit,
    onDone: () -> Unit,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit,
    onResize: (GridWidget) -> Unit,
    onMove: (Int, Int) -> Unit,
    onWidgetSettings: (Int) -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val manager = remember(context) { AppWidgetManager.getInstance(context) }
    val grid = rememberLazyGridState()
    val resizeScroll = remember { ResizeScrollState() }
    val editBottom = LocalConfiguration.current.screenHeightDp.dp
    var selectedId by rememberSaveable { mutableStateOf<Int?>(null) }
    var sizeDialog by remember { mutableStateOf<GridWidget?>(null) }
    var resizePreview by remember { mutableStateOf<GridWidget?>(null) }
    var resizingId by remember { mutableStateOf<Int?>(null) }
    var draggedId by remember { mutableStateOf<Int?>(null) }
    var dragPosition by remember { mutableStateOf(Offset.Zero) }
    var dragPointer by remember { mutableStateOf(Offset.Zero) }
    var dropping by remember { mutableStateOf(false) }
    var pageWidthPx by remember { mutableIntStateOf(0) }
    var previewOrder by remember { mutableStateOf<List<Int>?>(null) }
    var pendingOrder by remember { mutableStateOf<List<Int>?>(null) }
    val displayedWidgets = remember(widgets, previewOrder) { orderedGridWidgets(widgets, previewOrder) }
    val latestWidgets by rememberUpdatedState(displayedWidgets)
    val dropProgress = remember { Animatable(0f) }
    val latestMove by rememberUpdatedState(onMove)
    val latestEdit by rememberUpdatedState(onEdit)
    val sideLabel = stringResource(if (side == WidgetPageSide.LEFT) R.string.widget_pages_left else R.string.widget_pages_right)
    val editLabel = stringResource(R.string.widget_pages_edit)
    val resizeLabel = stringResource(R.string.widget_pages_resize)
    val removeLabel = stringResource(R.string.widget_pages_remove)
    val earlierLabel = stringResource(R.string.widget_pages_move_before)
    val laterLabel = stringResource(R.string.widget_pages_move_after)
    val changeSettingsLabel = stringResource(R.string.widget_change_settings)
    val shape = MaterialTheme.shapes.extraLarge
    val accent = MaterialTheme.colorScheme.primary
    val rowStepPx = with(density) { 100.dp.toPx() }
    val columnStepPx = ((pageWidthPx - with(density) { 20.dp.toPx() }) / 2f).coerceAtLeast(1f)
    val handleSizePx = with(density) { 48.dp.toPx() }
    val selected = widgets.firstOrNull { it.id == selectedId }.takeIf { editing }

    fun visibleItems() = grid.layoutInfo.visibleItemsInfo.map {
        GridDragItem(it.key as Int, it.index, it.offset.x.toFloat(), it.offset.y.toFloat(), it.size.width.toFloat(), it.size.height.toFloat())
    }

    fun updateDragTarget() {
        val id = draggedId ?: return
        val items = visibleItems()
        // Wait for the previous optimistic insertion to reach layout before moving again.
        if (items.any { latestWidgets.getOrNull(it.index)?.id != it.id }) return
        val target = gridDragTarget(items, id, dragPosition.x, dragPosition.y) ?: return
        previewOrder = moveGridWidget(latestWidgets, id, target).map { it.id }
    }

    val latestUpdateTarget by rememberUpdatedState(::updateDragTarget)

    LaunchedEffect(editing, active) {
        if (!editing || !active) {
            selectedId = null
            sizeDialog = null
            draggedId = null
            dropping = false
            resizingId = null
            resizePreview = null
            resizeScroll.update = null
            resizeScroll.vertical = false
            previewOrder = null
            pendingOrder = null
        }
    }
    LaunchedEffect(widgets) {
        if (widgets.none { it.id == selectedId }) selectedId = null
        val preview = resizePreview
        if (preview != null && resizingId == null && widgets.firstOrNull { it.id == preview.id } == preview) {
            resizePreview = null
        }
        if (widgets.none { it.id == draggedId } && draggedId != null) {
            draggedId = null
            previewOrder = null
        }
    }
    LaunchedEffect(widgets, pendingOrder) {
        if (pendingOrder == widgets.map { it.id }) {
            pendingOrder = null
            previewOrder = null
        }
    }
    DisposableEffect(gestureExclusions, side, active, editing, selectedId, draggedId) {
        onDispose { gestureExclusions.clearWidgetBounds(side) }
    }
    LaunchedEffect(dropping) {
        dropProgress.snapTo(0f)
        if (dropping) {
            dropProgress.animateTo(1f, spring(stiffness = 650f))
            draggedId = null
            dropping = false
        }
    }
    // Frame-aligned, density-independent autoscroll also updates order under a stationary finger.
    LaunchedEffect(draggedId) {
        var previousFrame = withFrameNanos { it }
        while (draggedId != null) {
            val frame = withFrameNanos { it }
            val seconds = ((frame - previousFrame) / 1_000_000_000f).coerceAtMost(0.05f)
            previousFrame = frame
            val edge = with(density) { 80.dp.toPx() }
            val speed = widgetDragScrollSpeed(dragPointer.y, 0f, grid.layoutInfo.viewportEndOffset.toFloat(), edge,
                with(density) { 700.dp.toPx() })
            if (speed != 0f && !dropping) {
                grid.scrollBy(speed * seconds)
                latestUpdateTarget()
            }
        }
    }

    LaunchedEffect(resizingId) {
        if(resizingId == null) return@LaunchedEffect
        var previous = withFrameNanos { it }
        while(resizingId != null) {
            val frame = withFrameNanos { it }
            val seconds = ((frame - previous) / 1_000_000_000f).coerceIn(0f,.05f)
            previous = frame
            if(resizeScroll.vertical) {
                val speed = widgetDragScrollSpeed(resizeScroll.pointerY,resizeScroll.viewportTop,
                    resizeScroll.viewportTop + resizeScroll.viewportHeight,with(density) { 80.dp.toPx() },with(density) { 560.dp.toPx() })
                if(speed != 0f) {
                    val consumed = grid.scrollBy(speed * seconds)
                    resizeScroll.scrolled += consumed
                    if(consumed != 0f) resizeScroll.update?.invoke()
                }
            }
        }
    }

    Column(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
            .onSizeChanged { pageWidthPx = it.width }
            .semantics {
                // Accessibility-only page name; the normal page has no visible heading.
                contentDescription = sideLabel
                onLongClick(editLabel) { selectedId = null; latestEdit(); true }
            }
            .pointerInput(editing) {
                detectTapGestures(
                    onTap = { if (editing) selectedId = null },
                    onLongPress = { selectedId = null; latestEdit() },
                )
            },
    ) {
        WidgetGridToolbar(editing, selected != null, onPrimary = {
            if (selected == null) onAdd() else { onRemove(selected.id); selectedId = null }
        }, onDone = onDone)
        // Also intentionally empty when there are no widgets. A hold opens Add / Done.
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = grid,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = if(editing) editBottom else 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            userScrollEnabled = draggedId == null && resizingId == null,
            modifier = Modifier.fillMaxSize().onGloballyPositioned {
                resizeScroll.viewportTop = it.localToWindow(Offset.Zero).y
                resizeScroll.viewportHeight = it.size.height.toFloat()
            }.pointerInput(editing, active) {
                if (!editing || !active) return@pointerInput
                // Observe in the fixed viewport, not a widget that moves during reflow.
                // Normal swipes are left to LazyGrid; only a completed hold claims a drag.
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (draggedId != null || resizingId != null) return@awaitEachGesture
                    val hit = visibleItems().firstOrNull { it.contains(down.position.x, down.position.y) }
                        ?: return@awaitEachGesture
                    if (selectedId == hit.id && hit.hitsResizeHandle(down.position.x, down.position.y, handleSizePx)) {
                        return@awaitEachGesture
                    }
                    val hold = awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture
                    val item = visibleItems().firstOrNull { it.id == hit.id } ?: return@awaitEachGesture
                    selectedId = item.id
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    previewOrder = latestWidgets.map { it.id }
                    val origin = Offset(item.centerX, item.centerY)
                    val pointerOrigin = hold.position
                    dragPosition = origin
                    dragPointer = pointerOrigin
                    dropping = false
                    draggedId = item.id
                    val completed = drag(hold.id) { change ->
                        change.consume()
                        dragPointer = change.position
                        dragPosition = origin + change.position - pointerOrigin
                        latestUpdateTarget()
                    }
                    if (completed) {
                        dropping = true
                        val order = previewOrder
                        pendingOrder = order
                        val target = order?.indexOf(item.id) ?: -1
                        if (target >= 0) latestMove(item.id, target)
                    } else {
                        draggedId = null
                        previewOrder = null
                    }
                }
            },
        ) {
            itemsIndexed(displayedWidgets, key = { _, widget -> widget.id }, span = { _, widget ->
                GridItemSpan(resizePreview?.takeIf { it.id == widget.id }?.columns ?: widget.columns)
            }) { index, widget ->
                val isSelected = editing && selectedId == widget.id
                val isDragging = draggedId == widget.id
                val displayed = resizePreview?.takeIf { it.id == widget.id } ?: widget
                val unavailableLabel = stringResource(R.string.widget_unavailable)
                val label = remember(widget.id, active, unavailableLabel) {
                    manager.getAppWidgetInfo(widget.id)?.loadLabel(context.packageManager) ?: unavailableLabel
                }
                Box(
                    Modifier.animateItem(placementSpec = if (isDragging) null else spring(stiffness = 500f))
                        .fillMaxWidth().height(displayed.heightDp.dp)
                        .zIndex(if (draggedId == widget.id) 2f else if (isSelected) 1f else 0f)
                        .graphicsLayer {
                            if (draggedId == widget.id) {
                                val item = grid.layoutInfo.visibleItemsInfo.firstOrNull { it.key == widget.id }
                                if (item != null) {
                                    val remaining = 1f - dropProgress.value
                                    translationX = (dragPosition.x - item.offset.x - item.size.width / 2f) * remaining
                                    translationY = (dragPosition.y - item.offset.y - item.size.height / 2f) * remaining
                                    scaleX = 1f + 0.025f * remaining
                                    scaleY = 1f + 0.025f * remaining
                                }
                            }
                        }
                        .onGloballyPositioned { coordinates ->
                            if (isSelected && active && !isDragging) {
                                val position = coordinates.localToWindow(Offset.Zero)
                                gestureExclusions.setWidgetBounds(side, position.x, position.y, coordinates.size.width, coordinates.size.height)
                            }
                        }
                        // Every widget is outlined while arranging, as the blocks of the home screen are;
                        // the one in hand gets the accent and a thicker line.
                        .then(
                            if (isSelected) Modifier.border(2.dp, accent, shape)
                            else if (editing) Modifier.border(1.dp, Color.White.copy(alpha = 0.55f), shape)
                            else Modifier
                        )
                        .semantics {
                            if (editing) {
                                contentDescription = label
                                onClick(resizeLabel) { selectedId = widget.id; sizeDialog = widget; true }
                                customActions = buildList {
                                    add(CustomAccessibilityAction(resizeLabel) { selectedId = widget.id; sizeDialog = widget; true })
                                    add(CustomAccessibilityAction(removeLabel) { onRemove(widget.id); true })
                                    if (manager.getAppWidgetInfo(widget.id)?.configure != null) {
                                        add(CustomAccessibilityAction(changeSettingsLabel) {
                                            onWidgetSettings(widget.id); true
                                        })
                                    }
                                    if (index > 0) add(CustomAccessibilityAction(earlierLabel) { onMove(widget.id, index - 1); true })
                                    if (index < displayedWidgets.lastIndex) add(CustomAccessibilityAction(laterLabel) { onMove(widget.id, index + 1); true })
                                }
                            }
                        },
                ) {
                    WidgetPage(widget.id, interactionsEnabled = !editing, visible = active) { _, _ -> selectedId = widget.id; onEdit() }
                    if (editing) {
                        // The widget keeps its view/state, but editor touches cannot launch its app.
                        Box(
                            Modifier.fillMaxSize()
                                .pointerInput(widget.id) { detectTapGestures(onTap = { selectedId = widget.id }) },
                        )
                    }
                    if (isSelected && draggedId == null) {
                        if (manager.getAppWidgetInfo(widget.id)?.configure != null) {
                            WidgetSettingsButton(
                                onClick = { onWidgetSettings(widget.id) },
                                modifier = Modifier.align(Alignment.Center),
                            )
                        }
                        ResizeEdge.entries.forEach { edge ->
                            val placement = when (edge) {
                                ResizeEdge.TOP -> Modifier.align(Alignment.TopCenter)
                                ResizeEdge.BOTTOM -> Modifier.align(Alignment.BottomCenter)
                                ResizeEdge.LEFT -> Modifier.align(AbsoluteAlignment.CenterLeft)
                                ResizeEdge.RIGHT -> Modifier.align(AbsoluteAlignment.CenterRight)
                            }
                            WidgetResizeHandle(
                                widget = widget, edge = edge, resizeScroll = resizeScroll, columnStepPx = columnStepPx, rowStepPx = rowStepPx,
                                modifier = placement,
                                onStart = { resizingId = widget.id },
                                onPreview = { resizePreview = it },
                                onCommit = { onResize(it); resizingId = null },
                                onCancel = { resizePreview = null; resizingId = null },
                                onOpenSize = { sizeDialog = displayed },
                            )
                        }
                    }
                }
            }
        }
    }
    sizeDialog?.let { widget ->
        WidgetSizeDialog(widget, onDismiss = { sizeDialog = null }, onSave = {
            resizePreview = it
            onResize(it)
            sizeDialog = null
        })
    }
}
