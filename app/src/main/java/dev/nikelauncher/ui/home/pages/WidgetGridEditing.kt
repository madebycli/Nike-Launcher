// SPDX-License-Identifier: GPL-3.0-or-later
// Split from WidgetGridPage.kt, adapted from Victoria Grid (GPL-3.0-or-later).
package dev.nikelauncher.ui.home.pages

import androidx.compose.material3.AlertDialog
import android.appwidget.AppWidgetManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.nikelauncher.R
import dev.nikelauncher.data.GridWidget
import dev.nikelauncher.data.WidgetPageSide
import dev.nikelauncher.data.resizeGridWidget
import dev.nikelauncher.data.moveGridWidget
import dev.nikelauncher.ui.kit.settings.ChoiceChips
import dev.nikelauncher.ui.kit.settings.SettingsSlider
import dev.nikelauncher.widget.WidgetPage
import dev.nikelauncher.widget.WidgetSettingsButton

@Composable
internal fun WidgetResizeHandle(
    widget: GridWidget,
    edge: ResizeEdge,
    resizeScroll: ResizeScrollState,
    columnStepPx: Float,
    rowStepPx: Float,
    modifier: Modifier,
    onStart: () -> Unit,
    onPreview: (GridWidget) -> Unit,
    onCommit: (GridWidget) -> Unit,
    onCancel: () -> Unit,
    onOpenSize: () -> Unit,
) {
    val label = stringResource(if (edge == ResizeEdge.TOP || edge == ResizeEdge.BOTTOM)
        R.string.widget_pages_height else R.string.widget_pages_width)
    val accent = MaterialTheme.colorScheme.primary
    // Layout coordinates are deliberately not Compose state: moving a grip must not
    // recompose the editor on every pointer sample.
    val position = remember { ResizeHandlePosition() }
    Box(
        modifier.size(48.dp)
            .onGloballyPositioned { position.coordinates = it }
            .semantics { contentDescription = label; onClick { onOpenSize(); true } }
            .pointerInput(widget, edge, columnStepPx, rowStepPx) { detectTapGestures(onTap = { onOpenSize() }) }
            .pointerInput(widget, edge, columnStepPx, rowStepPx) {
                var origin = Offset.Zero
                var preview = widget
                var pointer = Offset.Zero
                fun update() {
                    val drag = pointer - origin + Offset(0f,resizeScroll.scrolled)
                    preview = when (edge) {
                        ResizeEdge.TOP -> resizeGridWidget(widget, rowsDelta = -drag.y / rowStepPx)
                        ResizeEdge.BOTTOM -> resizeGridWidget(widget, rowsDelta = drag.y / rowStepPx)
                        ResizeEdge.LEFT -> resizeGridWidget(widget, columnsDelta = -drag.x / columnStepPx)
                        ResizeEdge.RIGHT -> resizeGridWidget(widget, columnsDelta = drag.x / columnStepPx)
                    }
                    onPreview(preview)
                }
                fun finish() { resizeScroll.update = null; resizeScroll.vertical = false }
                detectDragGestures(
                    onDragStart = { local ->
                        origin = position.coordinates?.localToWindow(local) ?: local
                        preview = widget
                        pointer = origin
                        resizeScroll.pointerY = origin.y
                        resizeScroll.scrolled = 0f
                        resizeScroll.vertical = edge == ResizeEdge.TOP || edge == ResizeEdge.BOTTOM
                        resizeScroll.update = ::update
                        onStart()
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        // A left/top grip itself moves when the grid snaps to a new size.
                        // Window coordinates prevent that movement from feeding back into the resize.
                        pointer = position.coordinates?.localToWindow(change.position) ?: change.position
                        resizeScroll.pointerY = pointer.y
                        update()
                    },
                    onDragCancel = { finish(); onCancel() },
                    onDragEnd = { finish(); onCommit(preview) },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Surface(shape = CircleShape, color = accent, border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface),
            modifier = Modifier.size(14.dp)) {}
    }
}

internal class ResizeScrollState {
    var pointerY = 0f
    var viewportTop = 0f
    var viewportHeight = 0f
    var scrolled = 0f
    var vertical = false
    var update: (() -> Unit)? = null
}

internal class ResizeHandlePosition { var coordinates: LayoutCoordinates? = null }

@Composable
internal fun WidgetSizeDialog(widget: GridWidget, onDismiss: () -> Unit, onSave: (GridWidget) -> Unit) {
    var columns by remember(widget.id) { mutableIntStateOf(widget.columns) }
    var rows by remember(widget.id) { mutableIntStateOf(widget.rows) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.widget_pages_resize)) },
        text = {
            Column {
                Text(stringResource(R.string.widget_pages_width), style = MaterialTheme.typography.labelLarge)
                ChoiceChips(
                    options = listOf(1, 2),
                    selected = columns,
                    label = { stringResource(if (it == 1) R.string.widget_pages_narrow else R.string.widget_pages_wide) },
                    onSelect = { columns = it },
                )
                Text(
                    stringResource(R.string.widget_pages_height) + " · " + pluralStringResource(R.plurals.widget_pages_rows, rows, rows),
                    style = MaterialTheme.typography.labelLarge,
                )
                SettingsSlider(rows.toFloat(), 1f..dev.nikelauncher.data.MAX_GRID_WIDGET_ROWS.toFloat(), { rows = it.toInt() })
            }
        },
        confirmButton = { TextButton(onClick = { onSave(widget.copy(columns = columns, rows = rows, configuredHeightDp = null)) }) { Text(stringResource(R.string.widget_pages_done)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
    )
}

internal enum class ResizeEdge { TOP, BOTTOM, LEFT, RIGHT }
