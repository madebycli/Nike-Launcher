// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.widget

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.os.Bundle
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dev.nikelauncher.NikeApp
import dev.nikelauncher.ui.common.TouchAnchoredMenu
import kotlinx.coroutines.delay
import dev.nikelauncher.R
import androidx.compose.ui.res.stringResource

/** How long the page dots stay up after a swipe settles. */
private const val DOTS_LINGER_MS = 1200L

data class WidgetSlotActions(
    val onAddWidget: () -> Unit,
    val onRemoveWidget: (widgetId: Int) -> Unit,
    val onWidgetSettings: (widgetId: Int) -> Unit,
    val onAppInfo: (widgetId: Int) -> Unit,
    val onResize: (heightDp: Int) -> Unit,
    val onOpenSettings: () -> Unit,
    /** The same picker, with whatever it binds kept in a folder instead (#115). */
    val onAddWidgetToFolder: (folderId: String) -> Unit = {},
    val onRemoveFolderWidget: (folderId: String, widgetId: Int) -> Unit = { _, _ -> },
    val onResizeFolderWidget: (folderId: String, widgetId: Int, heightDp: Int) -> Unit = { _, _, _ -> },
    val onSetFolderNowPlaying: (folderId: String, on: Boolean) -> Unit = { _, _ -> },
)

/**
 * The home screen's widget area: one widget on screen at a time, swiped between, or with
 * [stacked] all of them one under another.
 *
 * Every page shares [heightDp] rather than carrying its own, so swiping never moves the
 * favorites underneath — a widget that changed the layout of the rest of the screen as it
 * scrolled past would be far more distracting than the widget itself.
 */
@Composable
fun WidgetSlot(
    widgetIds: List<Int>,
    heightDp: Int,
    stacked: Boolean,
    onEditLayout: () -> Unit,
    actions: WidgetSlotActions,
    /** Bumped when HOME is pressed on a home screen already showing, so the stack goes back. */
    homeIntentTick: Int,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var menuOffset by remember { mutableStateOf(DpOffset.Zero) }
    var menuForId by remember { mutableStateOf(-1) }
    val density = LocalDensity.current

    fun openMenu(widgetId: Int, x: Float, y: Float) {
        menuForId = widgetId
        menuOffset = with(density) { DpOffset(x.toDp(), y.toDp()) }
        menuExpanded = true
    }

    val rows = if (stacked) widgetIds.size.coerceAtLeast(1) else 1
    Box(modifier = modifier.height((heightDp * rows).dp)) {
        if (stacked && widgetIds.isNotEmpty()) {
            Column(Modifier.fillMaxSize()) {
                widgetIds.forEach { id ->
                    key(id) {
                        Box(Modifier.fillMaxWidth().height(heightDp.dp)) {
                            WidgetPage(widgetId = id, onPressAndHold = { x, y -> openMenu(id, x, y) })
                        }
                    }
                }
            }
        } else if (widgetIds.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { actions.onAddWidget() },
                            onLongPress = { offset -> openMenu(-1, offset.x, offset.y) },
                        )
                    },
                color = Color.Black.copy(alpha = 0.25f),
                shape = MaterialTheme.shapes.largeIncreased,
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.widget_add), tint = Color.White)
                    Text(stringResource(R.string.widget_add), color = Color.White.copy(alpha = 0.8f))
                }
            }
        } else {
            val pagerState = rememberPagerState(pageCount = { widgetIds.size })
            // HOME means the home screen as you left it set up, not as you last flicked
            // through it. Animated rather than snapped: the screen is in front of you when it
            // happens, and a stack that jumps has nothing to say about where it went.
            LaunchedEffect(homeIntentTick) {
                if (homeIntentTick > 0 && pagerState.currentPage != 0) {
                    pagerState.animateScrollToPage(0)
                }
            }
            HorizontalPager(
                state = pagerState,
                // Keyed by widget id so adding or removing one doesn't rebuild every host
                // view; neighbors stay alive so a swipe doesn't land on a blank page.
                key = { widgetIds[it] },
                beyondViewportPageCount = 1,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                val id = widgetIds[page]
                WidgetPage(widgetId = id, onPressAndHold = { x, y -> openMenu(id, x, y) })
            }

            if (widgetIds.size > 1) {
                // Only worth showing while there is something to say; they fade out rather
                // than sitting permanently on top of somebody's clock.
                var dotsVisible by remember { mutableStateOf(false) }
                LaunchedEffect(pagerState.isScrollInProgress) {
                    if (pagerState.isScrollInProgress) {
                        dotsVisible = true
                    } else {
                        delay(DOTS_LINGER_MS)
                        dotsVisible = false
                    }
                }
                val dotsAlpha by animateFloatAsState(
                    targetValue = if (dotsVisible) 1f else 0f,
                    label = "widgetDots",
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 6.dp)
                        .graphicsLayer { alpha = dotsAlpha },
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    repeat(widgetIds.size) { index ->
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    color = Color.White.copy(alpha = if (index == pagerState.currentPage) 0.9f else 0.35f),
                                    shape = CircleShape,
                                ),
                        )
                    }
                }
            }
        }

        TouchAnchoredMenu(expanded = menuExpanded, offset = menuOffset, onDismissRequest = { menuExpanded = false }) {
            val id = menuForId
            if (id > 0) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_app_info)) },
                    leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null) },
                    onClick = { menuExpanded = false; actions.onAppInfo(id) },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_edit_layout)) },
                    leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                    onClick = { menuExpanded = false; onEditLayout() },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.widget_change_settings)) },
                    leadingIcon = { Icon(Icons.Filled.Tune, contentDescription = null) },
                    onClick = { menuExpanded = false; actions.onWidgetSettings(id) },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.widget_add_another)) },
                    leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    onClick = { menuExpanded = false; actions.onAddWidget() },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.widget_remove_this)) },
                    leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                    onClick = { menuExpanded = false; actions.onRemoveWidget(id) },
                )
            } else {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.widget_add)) },
                    leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    onClick = { menuExpanded = false; actions.onAddWidget() },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_edit_layout)) },
                    leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                    onClick = { menuExpanded = false; onEditLayout() },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_open_settings)) },
                leadingIcon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                onClick = { menuExpanded = false; actions.onOpenSettings() },
            )
        }
    }
}
