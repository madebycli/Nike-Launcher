// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.pages

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.os.Build
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.LayoutDirection
import dev.nikelauncher.R
import dev.nikelauncher.NikeApp
import dev.nikelauncher.data.GridWidget
import dev.nikelauncher.data.MAX_GRID_WIDGET_ROWS
import dev.nikelauncher.data.configuredGridWidget
import dev.nikelauncher.data.WidgetPageSide
import dev.nikelauncher.data.WidgetPagesConfig
import dev.nikelauncher.widget.WidgetPickerActivity
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.abs

/** The physical left/right pages keep their meaning even when the app's language is RTL. */
@Composable
internal fun WidgetHomePages(
    app: NikeApp,
    homeIntentTick: Int,
    gesturesEnabled: Boolean,
    config: WidgetPagesConfig,
    gestureExclusions: PageGestureExclusions,
    onSidePageChanged: (Boolean) -> Unit,
    lockResetSignal: Int = 0,
    home: @Composable (pagesEnabled: Boolean) -> Unit,
) {
    val left by remember(app) { app.prefs.pages.widgets(WidgetPageSide.LEFT) }.collectAsState(initial = emptyList())
    val right by remember(app) { app.prefs.pages.widgets(WidgetPageSide.RIGHT) }.collectAsState(initial = emptyList())
    val pages = remember(config.leftEnabled, config.rightEnabled) {
        buildList<WidgetPageSide?> {
            if (config.leftEnabled) add(WidgetPageSide.LEFT)
            add(null)
            if (config.rightEnabled) add(WidgetPageSide.RIGHT)
        }
    }
    val homeIndex = pages.indexOf(null)
    val pager = rememberPagerState(initialPage = homeIndex, pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val manager = remember(context) { AppWidgetManager.getInstance(context) }
    var editingSide by rememberSaveable { mutableStateOf<WidgetPageSide?>(null) }
    var pendingSide by rememberSaveable { mutableStateOf<WidgetPageSide?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val side = pendingSide
        pendingSide = null
        val id = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
        if (result.resultCode == Activity.RESULT_OK && id > 0) {
            val info = manager.getAppWidgetInfo(id)
            val reconfigured = result.data?.getBooleanExtra(WidgetPickerActivity.EXTRA_RECONFIGURED, false) == true
            if (side == null || info == null) {
                // A pre-existing ID remains owned by its current page even when it becomes
                // temporarily unavailable while its settings activity is returning.
                if (!reconfigured) runCatching { app.widgetHost.deleteAppWidgetId(id) }
                Toast.makeText(context, R.string.widget_pages_add_failed, Toast.LENGTH_SHORT).show()
            } else {
                val availableWidth = (screenWidthDp - 32).coerceAtLeast(1)
                val requestedWidth = result.data?.getIntExtra(WidgetPickerActivity.EXTRA_CONFIGURED_WIDTH_DP, -1)?.takeIf { it > 0 }
                val requestedHeight = result.data?.getIntExtra(WidgetPickerActivity.EXTRA_CONFIGURED_HEIGHT_DP, -1)?.takeIf { it > 0 }
                if (reconfigured) {
                    val current = (if (side == WidgetPageSide.LEFT) left else right).firstOrNull { it.id == id }
                    if (current != null && requestedHeight != null) {
                        val sized = configuredGridWidget(current, requestedWidth, requestedHeight, availableWidth)
                        scope.launch {
                            app.prefs.pages.resize(side, id, sized.columns, sized.rows, sized.configuredHeightDp)
                        }
                    }
                } else {
                    val density = context.resources.displayMetrics.density
                    val rows = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && info.targetCellHeight > 0) {
                        info.targetCellHeight
                    } else ceil((info.minHeight / density + 12) / 100f).toInt()).coerceIn(1, MAX_GRID_WIDGET_ROWS)
                    val columns = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && info.targetCellWidth > 0) {
                        ceil(info.targetCellWidth / 2f).toInt().coerceIn(1, 2)
                    } else if (info.minWidth / density <= (availableWidth - 12) / 2f) 1 else 2
                    val widget = configuredGridWidget(GridWidget(id, columns, rows), requestedWidth, requestedHeight, availableWidth)
                    scope.launch { app.prefs.pages.add(side, widget) }
                    editingSide = side
                }
            }
        }
    }

    val onSide by remember(pager, homeIndex) {
        derivedStateOf { pager.currentPage != homeIndex || abs(pager.currentPageOffsetFraction) > 0.001f }
    }
    SideEffect { onSidePageChanged(onSide) }

    LaunchedEffect(config.leftEnabled, config.rightEnabled) {
        editingSide = null
        pager.scrollToPage(homeIndex)
    }
    LaunchedEffect(homeIntentTick) {
        if (homeIntentTick > 0) {
            editingSide = null
            pager.animateScrollToPage(homeIndex)
        }
    }
    LaunchedEffect(lockResetSignal) {
        if (lockResetSignal > 0) {
            editingSide = null
            pager.scrollToPage(homeIndex)
        }
    }
    LaunchedEffect(pager.settledPage) {
        if (pages.getOrNull(pager.settledPage) != editingSide) editingSide = null
    }
    BackHandler(enabled = gesturesEnabled && (onSide || editingSide != null)) {
        if (editingSide != null) editingSide = null
        else scope.launch { pager.animateScrollToPage(homeIndex) }
    }

    val originalDirection = LocalLayoutDirection.current
    // With no widget page there is nothing to swipe to, so the home screen is not put behind a
    // pager at all: its own vertical drag and spring are then the only gesture on it.
    if (pages.size == 1) {
        Box(Modifier.fillMaxSize()) { home(false) }
        return
    }
    Box(Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            HorizontalPager(
                state = pager,
                key = { pages[it]?.name ?: "home" },
                beyondViewportPageCount = 2,
                userScrollEnabled = gesturesEnabled && editingSide == null,
                modifier = Modifier.fillMaxSize(),
            ) { index ->
                CompositionLocalProvider(LocalLayoutDirection provides originalDirection) {
                    val side = pages[index]
                    if (side == null) {
                        home(pages.size > 1)
                    } else {
                        WidgetGridPage(
                            side = side,
                            widgets = if (side == WidgetPageSide.LEFT) left else right,
                            editing = editingSide == side,
                            active = pager.settledPage == index,
                            gestureExclusions = gestureExclusions,
                            onEdit = { editingSide = side },
                            onDone = { editingSide = null },
                            onWidgetSettings = { id ->
                                pendingSide = side
                                picker.launch(Intent(context, WidgetPickerActivity::class.java)
                                    .putExtra(WidgetPickerActivity.EXTRA_RECONFIGURE_WIDGET_ID, id))
                            },
                            onAdd = {
                                pendingSide = side
                                picker.launch(Intent(context, WidgetPickerActivity::class.java))
                            },
                            onRemove = { id ->
                                scope.launch {
                                    app.prefs.pages.remove(side, id)
                                    runCatching { app.widgetHost.deleteAppWidgetId(id) }
                                }
                            },
                            onResize = { widget -> scope.launch { app.prefs.pages.resize(side, widget.id, widget.columns, widget.rows, widget.configuredHeightDp) } },
                            onMove = { id, target -> scope.launch { app.prefs.pages.move(side, id, target) } },
                        )
                    }
                }
            }
        }
    }
}
