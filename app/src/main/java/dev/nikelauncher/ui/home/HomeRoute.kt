// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import dev.nikelauncher.NikeApp
import dev.nikelauncher.TypedKey
import dev.nikelauncher.data.AppInfo
import dev.nikelauncher.data.EdgeSide
import dev.nikelauncher.data.EntryKind
import dev.nikelauncher.data.Folder
import dev.nikelauncher.media.NowPlayingBus
import dev.nikelauncher.media.isListenerEnabled
import dev.nikelauncher.service.SystemUi
import dev.nikelauncher.ui.home.edit.HomeGestureZones
import dev.nikelauncher.ui.home.route.ObserveScrubBand
import dev.nikelauncher.ui.home.route.HomeBandOverlays
import dev.nikelauncher.ui.applist.ScrubState
import dev.nikelauncher.R
import dev.nikelauncher.ui.common.toast
import dev.nikelauncher.data.WidgetPagesConfig
import dev.nikelauncher.ui.home.route.AppListController
import dev.nikelauncher.ui.home.route.HomePagesHost
import dev.nikelauncher.ui.home.route.WallpaperLayers
import dev.nikelauncher.ui.home.route.AppListHost
import dev.nikelauncher.ui.home.route.BandEditing
import dev.nikelauncher.ui.home.route.EdgeGestureExclusion
import dev.nikelauncher.ui.home.route.EdgeZones
import dev.nikelauncher.ui.home.route.HomeRouteHooks
import dev.nikelauncher.ui.home.route.rememberHomeActions
import dev.nikelauncher.ui.home.route.FolderPickerHost
import dev.nikelauncher.ui.home.route.rememberListModels
import dev.nikelauncher.ui.home.route.swallowTouches
import dev.nikelauncher.widget.WidgetSlotActions
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val HOME_FADE_MS = 220

/** Room each letter wants on the strip before it stops being something anyone can hit. */
private val MIN_LETTER_HEIGHT = 15.dp

/** How far a swipe up carries the app list in: the distance a pull collapses it over, run backwards. */
private val SWIPE_OPEN_DISTANCE = 150.dp

/**
 * The home destination: the home screen itself, the app-list overlay layered over it, and the edge
 * zones that move between them.
 *
 * The overlay is kept composed and measured even while hidden, just never placed, because building
 * the whole list from scratch on every open is what made it take a beat to appear.
 */
@Composable
fun HomeRoute(
    app: NikeApp,
    homeIntentTick: Int,
    settings: HomeSettings,
    data: HomeData,
    folders: List<Folder>,
    favoriteKeys: List<String>,
    hiddenApps: Set<String>,
    launchCounts: Map<String, Int>,
    widgetActions: WidgetSlotActions,
    cornerApp: AppInfo?,
    cornerIsSearch: Boolean,
    typedToSearch: List<TypedKey>,
    onTypedToSearchHandled: (List<TypedKey>) -> Unit,
    scrubBandFractions: Pair<Float, Float>?,
    onPeekStatusBar: () -> Unit,
    onAppListVisibleChange: (Boolean) -> Unit,
    /** Locks an open private space or asks for a locked one to be opened, off this thread. */
    onTogglePrivateSpace: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val scrub = remember { ScrubState() }
    val appsByKey = data.appsByKey

    var viewportHeightPx by remember { mutableIntStateOf(0) }
    var cornerButtonBounds by remember { mutableStateOf<Rect?>(null) }
    var homeEditMode by remember { mutableStateOf(false) }
    var folderPickerFor by remember { mutableStateOf<AppInfo?>(null) }
    var listOpenFolders by remember { mutableStateOf(setOf<String>()) }

    val models = rememberListModels(app, settings, appsByKey, hiddenApps, data.nameOverrides, launchCounts, folders, listOpenFolders)
    val listModel = models.list
    val minBandHeightPx = listModel.letters.size * with(density) { MIN_LETTER_HEIGHT.toPx() }

    val openDistancePx = with(density) { SWIPE_OPEN_DISTANCE.toPx() }
    val listState = rememberLazyListState()
    val flingDecay = rememberSplineBasedDecay<Float>()
    val currentSettings by rememberUpdatedState(settings)
    val controller = remember {
        AppListController(
            scope = scope,
            scrub = scrub,
            openAnim = Animatable(0f),
            openDistancePx = openDistancePx,
            listState = listState,
            flingDecay = flingDecay,
            swipeOpensSearch = { currentSettings.swipeUpOpensSearch },
        )
    }

    // Read through state: a lambda kept by remember would otherwise hold the first value for good,
    // so a range saved by hand was written to the store and then never shown.
    val storedBand by rememberUpdatedState(scrubBandFractions)
    val bands = remember {
        BandEditing(
            stored = { storedBand },
            viewportPx = { viewportHeightPx },
            save = { top, height -> scope.launch { app.prefs.strip.setScrubBand(top, height) } },
        )
    }

    // The one place a row press is turned into something happening. A row that is not an app is
    // recognised once, here, rather than in each place a press is wired up.
    fun launchEntry(entry: AppInfo): Boolean = when (entry.kind) {
        // Handed off rather than done here: locking or unlocking is several binder calls and
        // possibly the system's own authentication screen, which is not a press's work to do on
        // the main thread. True because the press was acted on.
        EntryKind.PRIVATE_SPACE -> {
            onTogglePrivateSpace()
            true
        }
        else -> app.appRepository.launch(entry)
    }

    val actions = rememberHomeActions(
        app = app,
        settings = settings,
        favoriteKeys = favoriteKeys,
        cornerApp = cornerApp,
        cornerIsSearch = cornerIsSearch,
        widgets = widgetActions,
        hooks = HomeRouteHooks(
            launchEntry = ::launchEntry,
            controller = controller,
            bands = bands,
            onMoveToFolder = { folderPickerFor = it },
            setCornerBounds = { cornerButtonBounds = it },
            onEditModeBand = bands::start,
            onPeekStatusBar = onPeekStatusBar,
            onNavigate = onNavigate,
            homeBusy = { controller.visible || homeEditMode },
            viewportHeightPx = { viewportHeightPx },
            blockOffsets = { data.blockOffsets },
            minBandHeightPx = { minBandHeightPx },
        ),
    )

    val nowPlaying by NowPlayingBus.state.collectAsState()
    val listenerGranted = remember(homeIntentTick) { isListenerEnabled(context) }

    // A keystroke on the home screen opens the list already searching for it. Only when there is a
    // search field to type into: without one the query filters a list showing no sign of why.
    LaunchedEffect(typedToSearch) {
        if (typedToSearch.isEmpty()) return@LaunchedEffect
        val taken = typedToSearch
        onTypedToSearchHandled(taken)
        if (!settings.appListSearch || homeEditMode || bands.editing) return@LaunchedEffect
        if (!controller.visible) controller.openFully()
        controller.query = taken.fold(controller.query) { text, key -> if (key.char == null) text.dropLast(1) else text + key.char }
    }

    LaunchedEffect(settings.edgeSide) { scrub.syncRestingSide(settings.edgeSide) }

    // BACK on the home screen must do nothing whatsoever. Without this it reaches the activity and
    // finishes it, and the system shows whatever home-ish task is underneath. Registered first so
    // it stays the lowest priority: the overlay, edit mode and the band editor take BACK before it.
    BackHandler(enabled = true) {}
    BackHandler(enabled = controller.visible) { controller.close() }
    BackHandler(enabled = homeEditMode) { homeEditMode = false }
    BackHandler(enabled = bands.editing) { bands.commit() }

    LaunchedEffect(controller.visible) { onAppListVisibleChange(controller.visible) }
    // Leaving the home destination entirely takes the overlay with it.
    DisposableEffect(Unit) { onDispose { onAppListVisibleChange(false) } }

    // Leaving the launcher (screen off, another app) always drops us back to the home screen
    // rather than reopening onto the overlay.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> controller.onPause()
                Lifecycle.Event.ON_RESUME -> controller.onResume()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(homeIntentTick) { if (homeIntentTick > 0) controller.close(snap = true) }

    // The gap left for the back gesture, per side: followed from the system when automatic (it
    // reports how far its gesture reaches and reports nothing with button navigation), the set
    // number otherwise.
    var onSidePage by remember { mutableStateOf(false) }
    val pagesConfig by app.prefs.pages.config.collectAsState(WidgetPagesConfig())
    val gestureInsets = WindowInsets.systemGestures
    val layoutDirection = LocalLayoutDirection.current
    val insetLeftDp = if (settings.edgeZoneInsetAuto) with(density) { gestureInsets.getLeft(this, layoutDirection).toDp().value.roundToInt() } else settings.edgeZoneInsetDp
    val insetRightDp = if (settings.edgeZoneInsetAuto) with(density) { gestureInsets.getRight(this, layoutDirection).toDp().value.roundToInt() } else settings.edgeZoneInsetDp
    // A widget page keeps the system gestures in the places its controls are; the strip's own
    // exclusion would overwrite them, and there is no strip to protect there.
    if (!onSidePage) ObserveScrubBand(bands) { band ->
        EdgeGestureExclusion(settings.edgeSide, settings.edgeZoneWidthDp, insetLeftDp, insetRightDp, band)
    }

    val homeContentAlpha by animateFloatAsState(if (bands.editing) 0f else 1f, label = "homeContentAlpha")
    // Edit mode borrows the app list's dim: its controls are small and a busy wallpaper made them hard to read.
    val homeDim = if (homeEditMode) maxOf(settings.dimHomeAlpha, settings.dimWallpaperAlpha) else settings.dimHomeAlpha
    val snapNow = controller.visible || controller.snapHome

    Box(Modifier.fillMaxSize().onSizeChanged { viewportHeightPx = it.height }) {
        // Hidden entirely while the list is up, so only the wallpaper sits behind it, and kept
        // composed the same way so coming back is instant. Opening answers a finger on the edge and
        // must not lag, so it snaps; coming back gets a short decelerating fade.
        val homeAlpha by animateFloatAsState(
            targetValue = if (controller.visible) 0f else 1f,
            animationSpec = if (snapNow) snap() else tween(HOME_FADE_MS, easing = LinearOutSlowInEasing),
            label = "homeAlpha",
        )
        WallpaperLayers(settings, listVisible = controller.visible, editing = bands.editing, homeDim = homeDim, snapNow = snapNow, sidePageDim = pagesConfig.wallpaperDimAlpha.takeIf { onSidePage })

        Box(
            Modifier
                .graphicsLayer { alpha = homeAlpha }
                // Hidden, but still laid out: an AppWidgetHostView that is never placed loses its
                // layout and comes back with its text collapsed.
                .then(if (controller.visible) Modifier.pointerInput(Unit) { swallowTouches() } else Modifier)
                // Setting the range takes the home screen out of the picture entirely.
                .graphicsLayer { alpha = homeContentAlpha },
        ) {
            HomePagesHost(
                app = app,
                homeIntentTick = homeIntentTick,
                gesturesEnabled = !controller.visible && !homeEditMode,
                config = pagesConfig,
                onSidePageChanged = { onSidePage = it },
            ) {
                HomeScreen(
                    settings = settings,
                    data = data.copy(nowPlayingHasContent = settings.nowPlayingEnabled && (!listenerGranted || nowPlaying != null)),
                    actions = actions,
                    homeIntentTick = homeIntentTick,
                    editMode = homeEditMode,
                    onEditModeChange = { homeEditMode = it },
                    editOverlay = {
                        if (!controller.visible && !onSidePage) ObserveScrubBand(bands) { band ->
                            HomeGestureZones(settings, band, insetLeftDp, insetRightDp, cornerButtonBounds, data.nameOverrides)
                        }
                    },
                )
            }
        }

        // Kept composed and measured even while hidden, just never placed: not placing it means it
        // neither draws nor receives touches.
        Box(
            Modifier
                .graphicsLayer { alpha = if (controller.visible) 1f else 0f }
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    if (controller.visible) layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                    else layout(0, 0) {}
                },
        ) {
            ObserveScrubBand(bands, active = controller.visible) { band ->
            AppListHost(
                app = app,
                settings = settings,
                data = data,
                models = models,
                scrub = scrub,
                band = band,
                viewportHeightPx = viewportHeightPx,
                controller = controller,
                favoriteKeys = favoriteKeys,
                hiddenApps = hiddenApps,
                insetLiveDp = when (settings.edgeSide) {
                    EdgeSide.LEFT -> insetLeftDp
                    EdgeSide.RIGHT -> insetRightDp
                    EdgeSide.BOTH -> maxOf(insetLeftDp, insetRightDp)
                },
                onMoveToFolder = { folderPickerFor = it },
                onToggleFolder = { folder ->
                    listOpenFolders = if (folder.id in listOpenFolders) listOpenFolders - folder.id else listOpenFolders + folder.id
                },
                launchEntry = ::launchEntry,
                onNavigate = onNavigate,
            )
            }
        }

        HomeBandOverlays(settings, bands, scrub, listModel.letters, controller.visible, homeEditMode, viewportHeightPx,
            onReset = { bands.live = null; scope.launch { app.prefs.strip.clearScrubBand() } })

        folderPickerFor?.let { target ->
            FolderPickerHost(app, target, folders, appsByKey, data.nameOverrides) { folderPickerFor = null }
        }

        if (!homeEditMode && !bands.editing && controller.query.isEmpty()) ObserveScrubBand(bands) { band ->
            EdgeZones(
                settings = settings,
                scrub = scrub,
                letters = listModel.letters,
                band = band,
                insetLeftDp = insetLeftDp,
                insetRightDp = insetRightDp,
                cornerButtonBounds = cornerButtonBounds,
                listOpen = controller.visible,
                controller = controller,
                onDoubleTapLock = if (settings.doubleTapToLock) {
                    { if (!SystemUi.lockScreen()) context.toast(R.string.toast_enable_accessibility_lock) }
                } else null,
            )
        }
    }
}
