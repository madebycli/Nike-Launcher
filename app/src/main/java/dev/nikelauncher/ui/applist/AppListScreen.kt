// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.applist

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import dev.nikelauncher.R
import dev.nikelauncher.data.AppInfo
import dev.nikelauncher.data.EdgeSide
import dev.nikelauncher.data.HomeAlignment
import dev.nikelauncher.data.folderToken
import dev.nikelauncher.ui.applist.rows.AppRow
import dev.nikelauncher.ui.applist.rows.AppRowActions
import dev.nikelauncher.ui.applist.rows.FOLDER_INDENT
import dev.nikelauncher.ui.applist.rows.FolderListActions
import dev.nikelauncher.ui.applist.rows.FolderListRow
import dev.nikelauncher.ui.applist.rows.ListRowStyle
import dev.nikelauncher.ui.applist.rows.SectionHeader
import dev.nikelauncher.ui.applist.rows.SettingsShortcutRow
import dev.nikelauncher.ui.applist.rows.WebSearchRow
import dev.nikelauncher.ui.common.EditAppDialog
import dev.nikelauncher.ui.theme.launcherTextShadow
import kotlin.math.abs
import kotlin.math.roundToInt

/** Breathing room above A and below the settings row when no scrub has placed the list. */
private val IDLE_TOP_PADDING = 8.dp
private val IDLE_BOTTOM_PADDING = 32.dp

/**
 * How far rows are held off an edge the strip can appear on: a floor rather than the whole answer.
 * What has to be cleared is the live touch zone, which is as wide as its own setting and is there
 * whether or not the letters are drawn.
 */
private val STRIP_INSET = 56.dp

/** Widest the list is allowed to get, so a name on a tablet stays near the strip scrubbing it. */
private val MAX_LIST_WIDTH = 600.dp

/** How far the list dissolves at each end. */
private val FADE_HEIGHT = 56.dp

/** Gap above and below search results: search has no scrub and no line, so results start where the list does. */
private val SEARCH_EDGE_PADDING = 8.dp

/** How far either end of the list may be dragged past its content. */
private val MAX_EDGE_STRETCH = 40.dp

/** How much of the collapse travel commits it, shared with the swipe that opens the list. */
private const val COMMIT_FRACTION = 0.4f

/**
 * The A-Z list of every app: placed by the strip, searched by the field, dismissed by pulling it
 * off either end.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AppListScreen(
    model: AppListModel,
    searchModel: AppListModel,
    nameOverrides: Map<String, String>,
    scrub: ScrubState,
    style: ListRowStyle,
    options: AppListOptions,
    actions: AppListActions,
    band: ScrubBand,
    viewportHeightPx: Int,
    visible: Boolean,
    favoriteKeys: Set<String>,
    /** Which apps are hidden, so a search that turns one up can say so and put it back. */
    hiddenApps: Set<String>,
    /** Hoisted so a swipe that overshoots the opening animation can keep scrolling it. */
    listState: LazyListState,
    /** How far the list still has to travel to be fully open; 0 once it has arrived. */
    enterPullPx: Float,
    searchFocusTick: Int,
    query: String,
    onQueryChange: (String) -> Unit,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    // Appended at the point of showing rather than stored in the label, so a rename still wins and
    // turning this off needs no re-reading of every app.
    val workSuffix = stringResource(R.string.app_work_suffix)
    fun displayName(app: AppInfo): String {
        val base = nameOverrides[app.key] ?: app.label
        return if (options.markWorkApps && app.work) base + workSuffix else base
    }
    // The gesture handlers below outlive the composition that created them, so they must not capture
    // this frame's callbacks.
    val currentDismiss by rememberUpdatedState(actions.onDismiss)

    // Reading these here confines the invalidation to this composable: the home screen behind the
    // overlay never sees the letter change. scrubY/pullPx stay function references so their callers
    // read them in the draw phase, not composition.
    val scrubLetter = scrub.letter
    val scrubbing = scrub.scrubbing
    val activeSide = scrub.side
    val scrubY = remember(scrub) { scrub::currentY }
    val pullPx = remember(scrub) { scrub::currentPull }

    // Searching is a different mode from scrubbing: the letters shrink to whatever matched, so the
    // strip is hidden and placement stays out of it until the query is cleared.
    val searching = options.searchEnabled && query.isNotBlank()
    val stripInset = maxOf(STRIP_INSET, options.edgeZoneWidthDp.dp)
    val displayModel = remember(model, searchModel, query, options.searchEnabled) {
        if (!searching) {
            model
        } else {
            val term = query.trim()
            searchModel
                .filtered { app ->
                    displayName(app).contains(term, ignoreCase = true) ||
                        // An app names itself in the language of the device, so on a Japanese phone
                        // Settings calls itself 設定 and no typing of "settings" reaches it. Package
                        // names are ASCII almost without exception.
                        app.componentName.packageName.contains(term, ignoreCase = true)
                }
                .rankedBy { searchRank(displayName(it), it.componentName.packageName, term) }
        }
    }
    // Animated rather than switched, so an end does not snap from crisp to faded.
    val topFade by animateFloatAsState(if (listState.canScrollBackward) 1f else 0f, label = "topFade")
    val bottomFade by animateFloatAsState(if (listState.canScrollForward) 1f else 0f, label = "bottomFade")

    /** Height of the pinned search field, so list-relative offsets can be compared to taps. */
    var searchHeightPx by remember { mutableIntStateOf(0) }
    // Each query is a fresh list, so it starts at the top.
    LaunchedEffect(query) { if (query.isNotBlank()) listState.scrollToItem(0) }

    // Rows outside the scrubbed letter fade out; only read inside a graphicsLayer, so the fade runs
    // in the draw phase instead of recomposing every visible row 60 times a second. Only while a
    // finger is travelling through the alphabet, and not while a held letter is walking the list.
    val othersAlpha by animateFloatAsState(
        targetValue = if (scrubLetter != null && scrubbing && options.dimOthers && !scrub.holdScrolling) 0f else 1f,
        animationSpec = tween(durationMillis = 180),
        label = "othersAlpha",
    )
    // Where a scrubbed letter is parked: a fact about the hand and the phone, so a setting.
    val sectionTopPx = (viewportHeightPx * options.sectionTopPercent / 100f).roundToInt()

    // The idle gaps are there so a scrubbed letter can be placed on the scrub line with room to
    // spare; whichever end a pinned search field is on gets the search gap either way.
    val restingTopPadding = if (options.searchEnabled && !options.searchAtBottom) SEARCH_EDGE_PADDING else IDLE_TOP_PADDING
    val restingBottomPadding = if (options.searchEnabled && options.searchAtBottom) SEARCH_EDGE_PADDING else IDLE_BOTTOM_PADDING
    val placement = rememberScrubPlacement(
        listState = listState,
        scrub = scrub,
        model = displayModel,
        lastLetterToLine = options.lastLetterToLine,
        holdScrollSpeed = options.holdScrollSpeed,
        sectionTopPx = sectionTopPx,
        idleTopPaddingPx = with(density) { restingTopPadding.roundToPx() },
        idleBottomPaddingPx = with(density) { restingBottomPadding.roundToPx() },
        viewportHeightPx = viewportHeightPx,
    )

    val dismissPullPx = with(density) { 150.dp.toPx() }
    val pull = remember(listState, placement, density) {
        ListPull(
            scope = scope,
            listState = listState,
            placement = placement,
            focusManager = focusManager,
            distances = PullDistances(
                dismissPx = dismissPullPx,
                maxPullPx = with(density) { 320.dp.toPx() },
                maxStretchPx = with(density) { MAX_EDGE_STRETCH.toPx() },
                fastDragPx = with(density) { 12.dp.toPx() },
                nearEndPx = with(density) { 48.dp.toPx() },
                commitPx = dismissPullPx * COMMIT_FRACTION,
            ),
            viewportHeightPx = { viewportHeightPx },
            onDismiss = { currentDismiss() },
        )
    }

    val searchFocus = remember { FocusRequester() }
    // Asked for after the overlay is actually placed: a focus request against a field that has not
    // been laid out yet throws, and the field arrives with the rest of the list.
    LaunchedEffect(searchFocusTick) {
        if (searchFocusTick > 0 && options.searchEnabled) runCatching { searchFocus.requestFocus() }
    }
    LaunchedEffect(visible, options.autoKeyboard, options.searchEnabled) {
        if (visible && options.autoKeyboard && options.searchEnabled) runCatching { searchFocus.requestFocus() }
    }
    // Everything the overlay left behind is cleared explicitly, because it stays composed while hidden.
    LaunchedEffect(visible) {
        if (!visible) {
            focusManager.clearFocus()
            placement.highlightRange = IntRange.EMPTY
            pull.reset()
            scrub.clearPlacement()
            return@LaunchedEffect
        }
        // Back to the top on the way in rather than on the way out, and only when nothing has asked
        // for a letter. Hidden, this overlay is measured but never placed, and a scroll waits for a
        // layout that is not coming, so one issued on close sat pending until the list next opened and
        // then raced the tap's own scroll for the scroll mutex.
        if (scrub.placement == null) listState.scrollToItem(0)
    }

    // Long-press anywhere in the list to edit that app. Keyed by row rather than by app: the same
    // app can be listed twice, under its letter and in an open folder, and a long press opened the
    // menu on both.
    var menuForKey by remember { mutableStateOf<String?>(null) }
    var menuOffset by remember { mutableStateOf(DpOffset.Zero) }
    var editDialogFor by remember { mutableStateOf<AppInfo?>(null) }
    val touchPosition = remember { mutableStateOf(Offset.Zero) }
    val folderById = remember(displayModel) {
        displayModel.rows.filterIsInstance<AppListRow.FolderRow>().associate { it.folder.id to it.folder }
    }

    // The vertical span the rows actually occupy. A tap inside it belongs to the list even when it
    // misses a label, and dismissing on those is what made the list feel like it was fighting you.
    fun isOnListContent(y: Float): Boolean {
        val info = listState.layoutInfo
        val first = info.visibleItemsInfo.firstOrNull() ?: return false
        val last = info.visibleItemsInfo.last()
        return y >= first.offset - info.viewportStartOffset && y < last.offset + last.size - info.viewportStartOffset
    }

    // The rows inherit the ambient style, so one provider puts the shadow behind every name.
    CompositionLocalProvider(
        LocalTextStyle provides LocalTextStyle.current.copy(
            shadow = launcherTextShadow(options.textShadow, style.contentColor, options.textShadowStrength),
        ),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .nestedScroll(pull.connection)
                .pointerInput(listState) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        val start = down.position
                        var claimed = false
                        var moved = false
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Final)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (change.isConsumed) claimed = true
                            if ((change.position - start).getDistance() > viewConfiguration.touchSlop) moved = true
                            if (!change.pressed) break
                        }
                        // A tap no row, letter or scroll claimed, landing clear of the list itself, is
                        // a tap on the wallpaper. Taps are in overlay space and the list is in its own;
                        // only a field above it shifts the two apart.
                        val listOffset = if (options.searchEnabled && !options.searchAtBottom) searchHeightPx else 0
                        if (claimed || moved || isOnListContent(start.y - listOffset)) return@awaitEachGesture
                        currentDismiss()
                    }
                },
        ) {
            Box(
                Modifier.fillMaxSize().graphicsLayer {
                    val pulled = pull.collapseProvider(enterPullPx)
                    val progress = (abs(pulled) / dismissPullPx).coerceIn(0f, 1f)
                    translationY = pulled * 0.6f
                    val scale = 1f - 0.12f * progress
                    scaleX = scale
                    scaleY = scale
                    alpha = 1f - 0.85f * progress
                },
            ) {
                // The keyboard covers the bottom of the overlay; insetting instead of panning the
                // window leaves the overlay where it is and gives the list the space that is left.
                // Ignoring visibility deliberately: a plain statusBarsPadding follows the bar as it
                // fades and the whole overlay jumped up into the space.
                Column(
                    Modifier
                        // Sideways the camera cut-out runs down an edge rather than along the top.
                        .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
                        .widthIn(max = MAX_LIST_WIDTH)
                        .align(if (activeSide == EdgeSide.LEFT) Alignment.TopStart else Alignment.TopEnd)
                        .fillMaxSize()
                        .then(if (options.statusBarHidden) Modifier else Modifier.windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility))
                        .imePadding(),
                ) {
                    // Pinned above the list rather than scrolling with it as a first item: every row
                    // index the scrub placement works from would shift by one.
                    if (options.searchEnabled && !options.searchAtBottom) {
                        AppListSearchField(
                            query, onQueryChange, options.edgeSide, stripInset,
                            onGo = { launchFirstMatch(displayModel, actions) },
                            atBottom = false,
                            modifier = Modifier.onSizeChanged { searchHeightPx = it.height }.focusRequester(searchFocus),
                        )
                    }
                    Box(Modifier.weight(1f)) {
                        CompositionLocalProvider(LocalOverscrollFactory provides null) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        translationY = pull.stretchProvider()
                                        // The mask below blends against this layer alone.
                                        compositingStrategy = CompositingStrategy.Offscreen
                                    }
                                    .drawWithContent {
                                        drawContent()
                                        // Dissolves the rows at both ends rather than painting over
                                        // them, so they fade into the background however light or dark
                                        // the wallpaper dim has made it. Each end fades only while
                                        // something is actually scrolled past it.
                                        val fade = (FADE_HEIGHT.toPx() / size.height).coerceIn(0f, 0.5f)
                                        if (fade > 0f) {
                                            drawRect(
                                                brush = Brush.verticalGradient(
                                                    0f to Color.Black.copy(alpha = 1f - topFade),
                                                    fade to Color.Black,
                                                    1f - fade to Color.Black,
                                                    1f to Color.Black.copy(alpha = 1f - bottomFade),
                                                ),
                                                blendMode = BlendMode.DstIn,
                                            )
                                        }
                                    },
                                // Room above A and below Z, so every letter lands on the same line,
                                // including the last (#71). Held on every side the strip may appear on,
                                // not on the one this scrub came from, so rows do not jump across the
                                // screen when the list is next opened from the other edge.
                                contentPadding = with(density) {
                                    PaddingValues(
                                        start = if (options.edgeSide != EdgeSide.RIGHT) stripInset else 0.dp,
                                        end = if (options.edgeSide != EdgeSide.LEFT) stripInset else 0.dp,
                                        top = when {
                                            searching -> SEARCH_EDGE_PADDING
                                            scrubLetter != null || !placement.highlightRange.isEmpty() -> sectionTopPx.toDp()
                                            else -> restingTopPadding
                                        },
                                        bottom = when {
                                            searching -> SEARCH_EDGE_PADDING
                                            // Whatever is left of the screen below the line, held for as
                                            // long as the list is open: added at the moment of placement
                                            // it was a jump.
                                            options.lastLetterToLine && placement.bottomRoomLive ->
                                                (viewportHeightPx - sectionTopPx).coerceAtLeast(0).toDp()
                                            else -> restingBottomPadding
                                        },
                                    )
                                },
                            ) {
                                if (searching && options.webSearchFallback && displayModel.rows.isEmpty()) {
                                    item(key = "websearch") {
                                        val term = query.trim()
                                        WebSearchRow(term) { actions.onWebSearch(term) }
                                    }
                                }
                                itemsIndexed(
                                    items = displayModel.rows,
                                    // The section is part of the key because the same app legitimately
                                    // appears twice: under its letter and under a section that repeats it.
                                    key = { _, row -> rowKey(row) },
                                    // Headers and app rows are laid out nothing alike; telling the list so
                                    // lets it reuse each kind against its own pool while scrubbing.
                                    contentType = { _, row ->
                                        when (row) {
                                            is AppListRow.Header -> 0
                                            is AppListRow.Entry -> 1
                                            is AppListRow.FolderRow -> 2
                                        }
                                    },
                                ) { index, row ->
                                    // Read in the draw phase on purpose: the scrub fade would otherwise
                                    // recompose every visible row on every frame of the tween.
                                    Box(Modifier.graphicsLayer { alpha = if (index in placement.highlightRange) 1f else othersAlpha }) {
                                        val key = rowKey(row)
                                        when (row) {
                                            is AppListRow.Header -> SectionHeader(row.text, style)
                                            is AppListRow.FolderRow -> FolderListRow(
                                                row = row,
                                                style = style,
                                                onHome = favoriteKeys.contains(folderToken(row.folder.id)),
                                                menuExpanded = menuForKey == key,
                                                menuOffset = menuOffset,
                                                touchPosition = touchPosition,
                                                actions = FolderListActions(
                                                    onToggle = { actions.onToggleFolder(row.folder) },
                                                    onManage = { menuForKey = null; actions.onManageFolder(row.folder) },
                                                    onSetOnHome = { menuForKey = null; actions.onSetFolderOnHome(row.folder, it) },
                                                    onDelete = { menuForKey = null; actions.onDeleteFolder(row.folder) },
                                                ),
                                                onLongPress = { offset -> menuOffset = offset; menuForKey = key },
                                                onDismissMenu = { menuForKey = null },
                                            )
                                            is AppListRow.Entry -> Box(
                                                // A folder's apps sit in from it, so where the folder ends is plain.
                                                Modifier.padding(
                                                    start = if (row.inFolder && style.alignment == HomeAlignment.LEFT) FOLDER_INDENT else 0.dp,
                                                    end = if (row.inFolder && style.alignment == HomeAlignment.RIGHT) FOLDER_INDENT else 0.dp,
                                                ),
                                            ) {
                                                AppRow(
                                                    app = row.app,
                                                    label = displayName(row.app),
                                                    style = style,
                                                    isFavorite = favoriteKeys.contains(row.app.key),
                                                    isHidden = row.app.key in hiddenApps,
                                                    menuExpanded = menuForKey == key,
                                                    menuOffset = menuOffset,
                                                    touchPosition = touchPosition,
                                                    actions = AppRowActions(
                                                        onLaunch = { actions.onLaunch(row.app) },
                                                        onSetFavorite = { actions.onSetFavorite(row.app, it) },
                                                        onEdit = { editDialogFor = row.app },
                                                        onAppInfo = { actions.onAppInfo(row.app) },
                                                        onUnpin = { actions.onUnpinShortcut(row.app) },
                                                        onHide = { actions.onHideApp(row.app, row.app.key !in hiddenApps) },
                                                        onMoveToFolder = { actions.onMoveToFolder(row.app) },
                                                        onRemoveFromFolder = row.section?.removePrefix("folder:")
                                                            ?.takeIf { row.inFolder }
                                                            ?.let { id -> folderById[id] }
                                                            ?.let { folder -> { actions.onRemoveFromFolder(folder, row.app) } },
                                                    ),
                                                    onLongPress = { offset -> menuOffset = offset; menuForKey = key },
                                                    onDismissMenu = { menuForKey = null },
                                                )
                                            }
                                        }
                                    }
                                }
                                if (options.showSettingsRow) {
                                    item(key = "settings", contentType = "settings") {
                                        SettingsShortcutRow(style, { othersAlpha }, actions.onOpenSettings)
                                    }
                                }
                            }
                        }
                    }
                    if (options.searchEnabled && options.searchAtBottom) {
                        AppListSearchField(
                            query, onQueryChange, options.edgeSide, stripInset,
                            onGo = { launchFirstMatch(displayModel, actions) },
                            atBottom = true,
                            modifier = Modifier.focusRequester(searchFocus),
                        )
                    }
                }

                if (options.showAlphabet && !searching) {
                    EdgeScrubber(
                        letters = displayModel.letters,
                        scrubY = scrubY,
                        pullPx = pullPx,
                        band = band,
                        side = activeSide,
                        style = options.scrubStyle,
                        ripple = options.rippleParams,
                        modifier = Modifier.align(if (activeSide == EdgeSide.LEFT) Alignment.CenterStart else Alignment.CenterEnd),
                    )
                }

                editDialogFor?.let { target ->
                    EditAppDialog(
                        currentName = displayName(target),
                        onConfirmName = { name -> actions.onSetName(target, name); editDialogFor = null },
                        onChangeIcon = { name ->
                            if (name.trim() != displayName(target)) actions.onSetName(target, name.trim())
                            actions.onChangeIcon(target)
                            editDialogFor = null
                        },
                        onDismiss = { editDialogFor = null },
                    )
                }

                if (scrubLetter != null && options.scrubBubbleShape != dev.nikelauncher.data.ScrubBubbleShape.HIDDEN) {
                    ScrubBubble(scrubLetter, options.scrubBubbleShape, activeSide, scrubY, pullPx)
                }
            }
        }
    }
}

/** The keyboard's action key launches the one app you have just finished spelling; nothing happens when nothing matched. */
private fun launchFirstMatch(model: AppListModel, actions: AppListActions) {
    model.rows.filterIsInstance<AppListRow.Entry>().firstOrNull()?.let { actions.onLaunch(it.app) }
}

private fun rowKey(row: AppListRow): String = when (row) {
    is AppListRow.Header -> "header:${row.text}"
    is AppListRow.FolderRow -> "folderrow:${row.folder.id}"
    is AppListRow.Entry -> row.section?.let { "$it:${row.app.key}" } ?: row.app.key
}
