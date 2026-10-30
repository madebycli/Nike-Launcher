// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.layout.height
import androidx.compose.ui.platform.testTag
import dev.nikelauncher.ui.home.edit.HomeEditControls
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import dev.nikelauncher.R
import dev.nikelauncher.data.HomeBlock
import dev.nikelauncher.service.HapticUtil
import dev.nikelauncher.ui.home.corner.CornerButton
import dev.nikelauncher.ui.home.blocks.FavoritesBlock
import dev.nikelauncher.ui.home.blocks.NowPlayingBlock
import dev.nikelauncher.ui.home.blocks.WidgetBlock
import dev.nikelauncher.ui.home.edit.BlockFrame
import dev.nikelauncher.ui.home.edit.FrameCallbacks
import dev.nikelauncher.ui.home.edit.PinEditor
import dev.nikelauncher.ui.home.edit.SnapGuideOverlay
import dev.nikelauncher.ui.home.rows.RowStyle
import dev.nikelauncher.ui.home.rows.wideScreenAlignment
import dev.nikelauncher.ui.home.stack.HomeStack
import dev.nikelauncher.ui.home.stack.SNAP_DISTANCE_DP
import dev.nikelauncher.ui.home.stack.StackMetrics
import dev.nikelauncher.ui.home.stack.homeBlock
import dev.nikelauncher.ui.kit.menu.TouchMenu
import dev.nikelauncher.ui.theme.launcherTextShadow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Widest the home stack is allowed to get, so rows on a tablet stay readable. */
private val MAX_CONTENT_WIDTH = 600.dp

/** How much room edit mode leaves under the last block for its toolbar. */
private val EDIT_BOTTOM_ROOM = 120.dp


/**
 * The home screen: the widget, Now Playing and the favorites, each held where it was put.
 *
 * Out of edit mode the stack is dragged freely with a spring at both ends. In edit mode every block
 * wears a frame and is moved by dragging its chip; the rows of the favorites reorder by their own
 * handles.
 */
@Composable
fun HomeScreen(
    settings: HomeSettings,
    data: HomeData,
    actions: HomeActions,
    homeIntentTick: Int,
    editMode: Boolean,
    onEditModeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    editOverlay: @Composable () -> Unit = {},
) {
    val density = LocalDensity.current
    val view = LocalView.current
    val style = remember(settings) {
        RowStyle(
            iconSizeDp = settings.iconSizeDp,
            labelSizeSp = settings.labelSizeSp,
            sidePaddingDp = settings.sidePaddingDp,
            contentColor = settings.contentColor,
            showLabels = settings.showFavoriteLabels,
            alignment = settings.alignment,
            iconSide = settings.iconSide,
            compactTapArea = settings.compactTapArea,
            shortcutSwipe = settings.shortcutSwipe,
        )
    }
    val insets = rememberHomeInsets(
        stripInsetSide = settings.stripInsetSide,
        edgeZoneSide = settings.edgeSide,
        edgeZoneExtentDp = settings.edgeZoneWidthDp + settings.edgeZoneInsetLiveDp,
        alignment = settings.alignment,
    )

    val metrics = remember { StackMetrics() }
    val bottomInsetPx = WindowInsets.navigationBars.getBottom(density)
    val snapPx = with(density) { SNAP_DISTANCE_DP.dp.toPx() }.roundToInt()
    val currentData by rememberUpdatedState(data)
    val currentActions by rememberUpdatedState(actions)
    val currentSettings by rememberUpdatedState(settings)
    val editor = remember(metrics, snapPx) {
        PinEditor(
            metrics = metrics,
            snapDistancePx = snapPx,
            stored = { currentData.pins },
            commit = { currentActions.layout.onCommitPins(it) },
            tick = { HapticUtil.tick(view, currentSettings.hapticsEnabled) },
        )
    }
    LaunchedEffect(data.pins) { editor.onStoredChanged() }

    val scope = rememberCoroutineScope()
    val scroll = rememberHomeScroll(actions.gestures, settings.swipeUpOpensAppList, homeIntentTick)
    val editScroll = rememberScrollState()
    LaunchedEffect(editMode) {
        scroll.reset()
        editScroll.scrollTo(0)
        if (!editMode) editor.clear()
    }
    var rootY by remember { mutableFloatStateOf(0f) }
    var backgroundMenu by remember { mutableStateOf(false) }
    var backgroundMenuOffset by remember { mutableStateOf(DpOffset.Zero) }

    // Empty media reserves its field; editing does not add a widget placeholder to the layout.
    val showWidget = data.widgetIds.isNotEmpty()
    val showNowPlaying = settings.nowPlayingEnabled
    val available = buildSet {
        if (showWidget) add(HomeBlock.WIDGET)
        if (showNowPlaying) add(HomeBlock.NOW_PLAYING)
        add(HomeBlock.FAVORITES)
    }
    SideEffect { editor.setAvailable(available) }

    // The rows take their style from the ambient one rather than naming it, so the shadow can be
    // put behind all of them at once instead of at every call that draws a name.
    CompositionLocalProvider(
        LocalTextStyle provides LocalTextStyle.current.copy(
            shadow = launcherTextShadow(settings.textShadow, settings.contentColor, settings.textShadowStrength),
        ),
    ) {
        Box(
            modifier
                .fillMaxSize()
                .onSizeChanged { scroll.viewportHeight = it.height }
                .onGloballyPositioned { rootY = it.positionInWindow().y }
                .then(scroll.dragModifier(enabled = !editMode))
                .then(scroll.quickLaunchModifier(enabled = settings.quickLaunchEnabled, editMode = editMode, region = settings.quickLaunchRegion))
                // Rows, the widget and the edge zones claim their own presses, so only bare
                // wallpaper reaches this.
                .pointerInput(editMode) {
                    if (editMode) return@pointerInput
                    detectTapGestures(onLongPress = { offset ->
                        backgroundMenuOffset = with(density) { DpOffset(offset.x.toDp(), offset.y.toDp()) }
                        backgroundMenu = true
                    })
                },
        ) {
            Box(
                Modifier
                    // Sideways the camera cut-out is down one edge rather than along the top.
                    .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
                    .widthIn(max = MAX_CONTENT_WIDTH)
                    .align(settings.alignment.wideScreenAlignment())
                    .fillMaxWidth()
                    .then(if (editMode) Modifier.verticalScroll(editScroll) else scroll.contentModifier()),
            ) {
                HomeStack(
                    modifier = Modifier.fillMaxWidth().padding(bottom = if (editMode) EDIT_BOTTOM_ROOM else 0.dp)
                        .onSizeChanged { scroll.contentHeight = it.height },
                    viewportHeightPx = { scroll.viewportHeight },
                    bottomInsetPx = { bottomInsetPx },
                    pins = { editor.pins },
                    metrics = metrics,
                    gapDp = settings.blockGapDp,
                ) {
                    fun frame(block: HomeBlock) = FrameCallbacks(
                        onSelect = { editor.select(block) },
                        onDragStart = { editor.begin(block) },
                        onDrag = { editor.move(block, it) },
                        onDragEnd = { editor.end(block) },
                        onDragCancel = editor::cancel,
                    )
                    if (showWidget) {
                        BlockFrame(
                            block = HomeBlock.WIDGET,
                            contentOffsetDp = data.blockOffsets.widget,
                            editing = editMode,
                            selected = HomeBlock.WIDGET in editor.selection,
                            takeByBody = true,
                            coverBody = true,
                            topInsidePx = { metrics.top(HomeBlock.WIDGET) },
                            callbacks = frame(HomeBlock.WIDGET),
                            modifier = Modifier.homeBlock(HomeBlock.WIDGET),
                        ) {
                            WidgetBlock(
                                widgetIds = data.widgetIds,
                                heightDp = data.widgetHeightDp,
                                stacked = settings.widgetsStacked,
                                sidePaddingDp = settings.widgetSidePaddingDp,
                                offsetXDp = settings.widgetOffsetXDp,
                                homeIntentTick = homeIntentTick,
                                actions = actions.widgets,
                                onEditLayout = { onEditModeChange(true) },
                            )
                        }
                    }
                    if (showNowPlaying) {
                        BlockFrame(
                            block = HomeBlock.NOW_PLAYING,
                            contentOffsetDp = data.blockOffsets.nowPlaying,
                            editing = editMode,
                            selected = HomeBlock.NOW_PLAYING in editor.selection,
                            takeByBody = true,
                            coverBody = true,
                            topInsidePx = { metrics.top(HomeBlock.NOW_PLAYING) },
                            callbacks = frame(HomeBlock.NOW_PLAYING),
                            modifier = Modifier.homeBlock(HomeBlock.NOW_PLAYING),
                        ) {
                            Box(Modifier.fillMaxWidth().height(settings.nowPlayingHeightDp.dp).testTag("home-media-slot")) {
                            if (data.nowPlayingHasContent) NowPlayingBlock(
                                heightDp = settings.nowPlayingHeightDp,
                                contentColor = settings.contentColor,
                                alignment = settings.alignment,
                                sidePaddingDp = settings.mediaSidePaddingDp,
                                showPlaceholder = false,
                                onEditLayout = { onEditModeChange(true) },
                                onOpenSettings = actions.onOpenSettings,
                            )
                            }
                        }
                    }
                    BlockFrame(
                        block = HomeBlock.FAVORITES,
                        contentOffsetDp = data.blockOffsets.favorites,
                        editing = editMode,
                        selected = HomeBlock.FAVORITES in editor.selection,
                        takeByBody = false,
                        coverBody = false,
                        topInsidePx = { metrics.top(HomeBlock.FAVORITES) },
                        callbacks = frame(HomeBlock.FAVORITES),
                        modifier = Modifier
                            .homeBlock(HomeBlock.FAVORITES)
                            .onGloballyPositioned { coords ->
                                // Handed up so the A-Z strip can match the span of the favorites.
                                if (editor.dragging == null) {
                                    val top = coords.positionInWindow().y
                                    actions.layout.onFavoritesBoundsChanged(top, top + coords.size.height)
                                }
                            },
                    ) {
                        FavoritesBlock(
                            data = data,
                            settings = settings,
                            style = style,
                            actions = actions,
                            editMode = editMode,
                            onEditModeChange = onEditModeChange,
                            stripInset = insets.rows,
                            folderControlsInset = insets.folderControls,
                            onFolderOpened = { top, height ->
                                val topInView = top - rootY
                                val overflow = topInView + height - (scroll.viewportHeight - bottomInsetPx)
                                scope.launch { scroll.revealBelow(overflow, topInView) }
                            },
                        )
                    }
                }
            }

            if (!editMode) {
                CornerButton(
                    corner = actions.corner,
                    alignment = settings.alignment,
                    iconSizeDp = settings.iconSizeDp,
                    contentColor = settings.contentColor,
                )
            }

            TouchMenu(expanded = backgroundMenu, offset = backgroundMenuOffset, onDismissRequest = { backgroundMenu = false }) {
                item(stringResource(R.string.action_edit_layout), { backgroundMenu = false; onEditModeChange(true) }, leadingIcon = Icons.Filled.Tune)
                item(stringResource(R.string.widget_add), { backgroundMenu = false; actions.widgets.onAddWidget() }, leadingIcon = Icons.Filled.Add)
                item(stringResource(R.string.home_choose_favorites), { backgroundMenu = false; actions.onManageFavorites() }, leadingIcon = Icons.Filled.Checklist)
                gap()
                item(stringResource(R.string.action_open_settings), { backgroundMenu = false; actions.onOpenSettings() }, leadingIcon = Icons.Filled.Settings)
            }

            if (editMode) editOverlay()
            SnapGuideOverlay(
                visible = editMode && editor.dragging != null,
                active = editor.guide,
                viewportPx = { scroll.viewportHeight },
                bottomInsetPx = { bottomInsetPx },
                modifier = Modifier.fillMaxSize(),
            )
            HomeEditControls(editMode, editor, metrics, settings, data, actions, available) { onEditModeChange(false) }
        }
    }
}
