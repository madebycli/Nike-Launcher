// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui

import android.app.Activity.RESULT_OK
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.os.UserHandle
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.nikelauncher.TypedKey
import dev.nikelauncher.NikeApp
import android.widget.Toast
import dev.nikelauncher.data.IconShape
import dev.nikelauncher.data.AppFont
import dev.nikelauncher.data.AppInfo
import dev.nikelauncher.data.EntryKind
import dev.nikelauncher.data.PrivateSpace
import dev.nikelauncher.data.AzStripVisibility
import dev.nikelauncher.data.EdgeSide
import dev.nikelauncher.data.RippleParams
import dev.nikelauncher.data.ScrubStyle
import dev.nikelauncher.data.ScrubBubbleShape
import dev.nikelauncher.data.EntryKeys
import dev.nikelauncher.data.FavoritesSource
import dev.nikelauncher.data.ShortcutSwipe
import dev.nikelauncher.data.HomeAlignment
import dev.nikelauncher.data.IconSide
import dev.nikelauncher.data.HomePaddings
import dev.nikelauncher.data.QuickLaunchSlot
import dev.nikelauncher.data.TextColorMode
import androidx.compose.ui.res.stringResource
import dev.nikelauncher.R
import dev.nikelauncher.data.folderIdFromToken
import dev.nikelauncher.data.stripsOtherProfiles
import dev.nikelauncher.media.isListenerEnabled
import dev.nikelauncher.service.SystemUi
import dev.nikelauncher.ui.common.IconPickerScreen
import dev.nikelauncher.ui.common.IconStyle
import dev.nikelauncher.ui.common.LocalIconConfig
import dev.nikelauncher.ui.common.clearIconCache
import dev.nikelauncher.ui.common.encodePackOverride
import dev.nikelauncher.ui.common.warmIconCache
import dev.nikelauncher.ui.common.iconCacheGeneration
import dev.nikelauncher.ui.home.FavoriteEntry
import dev.nikelauncher.ui.home.HomeRoute
import dev.nikelauncher.ui.home.HomeSettings
import dev.nikelauncher.ui.settings.AppPickerScreen
import dev.nikelauncher.ui.settings.FolderAppsScreen
import dev.nikelauncher.ui.settings.HiddenAppsScreen
import dev.nikelauncher.ui.theme.DEFAULT_SHADOW_STRENGTH
import dev.nikelauncher.ui.settings.ManageFavoritesScreen
import dev.nikelauncher.ui.settings.SettingsNav
import dev.nikelauncher.ui.settings.SettingsRoute
import dev.nikelauncher.ui.theme.rememberContentColor
import dev.nikelauncher.widget.ClockWidgetProvider
import dev.nikelauncher.widget.WidgetPickerActivity
import dev.nikelauncher.widget.WidgetSlotActions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Collects the stored settings once and hosts the navigation graph.
 *
 * Everything the destinations need is read here rather than in each screen, so the DataStore
 * is collected once per key instead of once per consumer.
 */
@Composable
fun NikeNavHost(
    app: NikeApp,
    homeIntentTick: Int,
    typedToSearch: List<TypedKey>,
    onTypedToSearchHandled: (List<TypedKey>) -> Unit,
    font: AppFont,
    hideStatusBar: Boolean,
    hideStatusBarAppList: Boolean,
    iconPackPackage: String?,
    iconOverrides: Map<String, String>,
    onPeekStatusBar: () -> Unit,
    /** Reported up so the status bar can stay put while the overlay is showing. */
    onAppListVisibleChange: (Boolean) -> Unit,
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Enumerating every installed app costs a PackageManager round trip per app; doing it in
    // the first composition is what stalled the cold start. Load it off the main thread and
    // let the home screen render against an empty list for the first frame.
    var appsAndSpace by remember { mutableStateOf(AppsAndSpace(PrivateSpace.Absent, emptyList())) }
    val allApps = appsAndSpace.apps
    // Kept beside the list because the settings screens need it for keys whose rows are
    // deliberately absent: a favorite inside a locked private space has nothing to look up.
    val privateSpace = appsAndSpace.privateSpace
    // Reloads are started from half a dozen independent places and overlap; this is what keeps
    // the most recently read answer the one on screen. See [ReloadOrder].
    val reloadOrder = remember { ReloadOrder() }

    /**
     * [known] is the state to enumerate against when the caller already has one it trusts more
     * than a fresh read would be — a lock it has just been granted, which the system has not
     * finished applying and would still describe as an open space.
     *
     * [pass] is the number this reload took from [reloadOrder] before it read anything.
     */
    suspend fun reloadApps(known: PrivateSpace?, pass: Long) {
        val (state, apps) = withContext(Dispatchers.Default) {
            // Resolved once and handed on, so the list and what the settings screens conceal
            // can never disagree about whether the space was open when it was read.
            val state = known ?: app.appRepository.privateSpace()
            state to app.appRepository.queryAllApps(state)
        }
        // Overtaken while it ran: this list was enumerated against a state that is no longer
        // the newest thing known about the device, and publishing it would put a locked
        // space's apps back under an open padlock. Nothing is lost by dropping it — the pass
        // that overtook this one publishes a list of its own.
        if (reloadOrder.mayPublish(pass)) appsAndSpace = AppsAndSpace(state, apps)
    }

    // A shortcut pinned through the confirm screen, or unpinned from a menu, changes what
    // there is to list without any package changing — so nothing here would otherwise notice.
    val shortcutChanges by app.appRepository.shortcutChanges.collectAsState()
    LaunchedEffect(shortcutChanges) { reloadApps(known = null, pass = reloadOrder.begin()) }

    /**
     * Takes a state that has just been resolved and acts on what it conceals at once, on the
     * list already in hand, before the reload that will take a moment.
     *
     * The reload is a full enumeration: every profile, every activity, an icon cache thrown
     * away and rebuilt. That is long enough to read the names off a home screen, and a lock
     * that only takes effect at the end of it has left them there for exactly that long.
     *
     * Publishes nothing once [pass] has been superseded, for the same reason the reload does
     * not: this state was read before a newer one, and the direction a stale state goes wrong
     * in is the one that un-conceals.
     */
    fun adoptPrivateSpace(state: PrivateSpace, pass: Long) {
        if (!reloadOrder.mayPublish(pass)) return
        appsAndSpace = AppsAndSpace(
            state,
            appsAndSpace.apps.filterNot { it.kind == EntryKind.PRIVATE_SPACE || state.conceals(it.key) } +
                app.appRepository.privateSpaceRow(state),
        )
    }
    LaunchedEffect(Unit) { reloadApps(known = null, pass = reloadOrder.begin()) }

    // Before anything the user does can write to the store, so "the store is empty" still
    // means "this is a first run" when it is read.
    LaunchedEffect(Unit) { app.prefs.home.ensureInstallMarker() }

    DisposableEffect(Unit) {
        val launcherApps = context.getSystemService(LauncherApps::class.java)
        fun refresh() {
            scope.launch {
                // Taken before the read, so anything already enumerating is superseded by the
                // moment this pass looked rather than by the moment it finished.
                val pass = reloadOrder.begin()
                // The private space first and on its own: whatever it conceals leaves the list
                // that is on screen now, rather than when the enumeration below comes back.
                val state = withContext(Dispatchers.Default) { app.appRepository.privateSpace() }
                adoptPrivateSpace(state, pass)
                // An app that ships a new icon in an update changes none of the cache
                // key's components, so nothing else would invalidate the stale bitmap.
                clearIconCache()
                reloadApps(state, pass)
            }
        }

        // LauncherApps reports package changes across every profile. The PACKAGE_* broadcasts
        // only ever describe the profile we run in, so an app installed into a work profile or
        // a private space would never reach the list.
        val callback = object : LauncherApps.Callback() {
            override fun onPackageAdded(packageName: String?, user: UserHandle?) = refresh()
            override fun onPackageRemoved(packageName: String?, user: UserHandle?) = refresh()
            override fun onPackageChanged(packageName: String?, user: UserHandle?) = refresh()
            override fun onPackagesAvailable(names: Array<out String>?, user: UserHandle?, replacing: Boolean) = refresh()
            override fun onPackagesUnavailable(names: Array<out String>?, user: UserHandle?, replacing: Boolean) = refresh()
            // A shortcut can be added, renamed, re-iconed or switched off while we are
            // showing it, none of which is a package change.
            override fun onShortcutsChanged(
                packageName: String,
                shortcuts: MutableList<ShortcutInfo>,
                user: UserHandle,
            ) = refresh()
        }
        runCatching { launcherApps.registerCallback(callback) }

        // Locking or unlocking a private space changes no package, so no package callback
        // ever fires for it — it arrives as one of these instead. Android 15 sends PROFILE_-
        // UNAVAILABLE then PROFILE_INACCESSIBLE on locking and the matching pair on
        // unlocking, including when the screen going off re-locks the space on its own.
        // Written as strings because the Intent constants are newer than this app's minimum.
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE)
            addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE)
            addAction("android.intent.action.PROFILE_AVAILABLE")
            addAction("android.intent.action.PROFILE_UNAVAILABLE")
            addAction("android.intent.action.PROFILE_ACCESSIBLE")
            addAction("android.intent.action.PROFILE_INACCESSIBLE")
            addAction("android.intent.action.PROFILE_ADDED")
            addAction("android.intent.action.PROFILE_REMOVED")
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) = refresh()
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        onDispose {
            runCatching { launcherApps.unregisterCallback(callback) }
            context.unregisterReceiver(receiver)
        }
    }

    // A broadcast is the only other thing that ever says the space has locked, and it is one
    // thing: it is only heard while this is composed, the system re-locks the space by itself
    // whenever the screen goes off, and a lock that was missed stays missed until something
    // else happens to reload. Coming back to the launcher is the moment that matters, so the
    // state is read again there — a handful of binder calls, with the enumeration behind it
    // only when the answer actually changed.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event != Lifecycle.Event.ON_START) return@LifecycleEventObserver
            scope.launch {
                val probe = withContext(Dispatchers.Default) { app.appRepository.privateSpace() }
                if (probe == appsAndSpace.privateSpace) {
                    // Android's "hide when locked" is set in Settings while the space stays
                    // locked, so the state reads the same and only the padlock row differs.
                    val row = withContext(Dispatchers.Default) { app.appRepository.privateSpaceRow(probe) }
                    val shown = appsAndSpace.apps.filter { it.kind == EntryKind.PRIVATE_SPACE }
                    if (row.map { it.componentName } == shown.map { it.componentName }) return@launch
                    val pass = reloadOrder.begin()
                    val state = withContext(Dispatchers.Default) { app.appRepository.privateSpace() }
                    adoptPrivateSpace(state, pass)
                    return@launch
                }
                // Only once there is something to do: a number taken to decide nothing changed
                // would supersede a reload already running and leave the list as it was. But
                // the read above was made before the number, so something newer may have
                // published in between — a lock, say — and acting on it now would outrank that.
                // So the number is taken first and the space is read again under it.
                val pass = reloadOrder.begin()
                val state = withContext(Dispatchers.Default) { app.appRepository.privateSpace() }
                adoptPrivateSpace(state, pass)
                clearIconCache()
                reloadApps(state, pass)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val hiddenApps by app.prefs.library.hiddenApps.collectAsState(initial = emptySet())
    val favoriteKeys by app.prefs.library.favorites.collectAsState(initial = emptyList())
    val frequentExcluded by app.prefs.library.frequentExcluded.collectAsState(initial = emptySet())
    val favoritesEverSet by app.prefs.library.favoritesEverSet.collectAsState(initial = true)
    val edgeZoneBandOnly by app.prefs.strip.edgeZoneBandOnly.collectAsState(initial = false)
    val holdScroll by app.prefs.strip.holdScroll.collectAsState(initial = false)
    val dimOthersScrubbing by app.prefs.strip.dimOthersScrubbing.collectAsState(initial = true)
    val markWorkApps by app.prefs.appearance.markWorkApps.collectAsState(initial = true)
    val textShadow by app.prefs.appearance.textShadow.collectAsState(initial = false)
    val textShadowStrength by app.prefs.appearance.textShadowStrength.collectAsState(initial = DEFAULT_SHADOW_STRENGTH)
    val compactTapArea by app.prefs.appearance.compactTapArea.collectAsState(initial = false)
    val stayAfterPrivateUnlock by app.prefs.appList.stayAfterPrivateUnlock.collectAsState(initial = false)
    val privateSpaceSection by app.prefs.appList.privateSpaceSection.collectAsState(initial = false)
    val foldersInAppList by app.prefs.appList.foldersInAppList.collectAsState(initial = false)
    val recentSection by app.prefs.appList.recentSection.collectAsState(initial = false)
    val recentSectionCount by app.prefs.appList.recentSectionCount.collectAsState(initial = 5)
    val edgeZoneInsetDp by app.prefs.strip.edgeZoneInsetDp.collectAsState(initial = 0)
    val edgeZoneInsetAuto by app.prefs.strip.edgeZoneInsetAuto.collectAsState(initial = true)
    val webSearchFallback by app.prefs.appList.webSearchFallback.collectAsState(initial = false)
    val swipeUpOpensSearch by app.prefs.appList.swipeUpOpensSearch.collectAsState(initial = false)
    val holdScrollSpeed by app.prefs.strip.holdScrollSpeed.collectAsState(initial = 4)
    // Noted the first time there is one, and never unset. Whether the hint is still wanted is a
    // question about whether someone has done this before, not about the list being empty now.
    LaunchedEffect(favoriteKeys.isNotEmpty()) {
        if (favoriteKeys.isNotEmpty()) app.prefs.library.markFavoritesEverSet()
    }
    val nameOverrides by app.prefs.library.nameOverrides.collectAsState(initial = emptyMap())
    val iconSizeDp by app.prefs.appearance.iconSizeDp.collectAsState(initial = 56)
    val labelSizeSp by app.prefs.appearance.labelSizeSp.collectAsState(initial = 16)
    val itemSpacingDp by app.prefs.appearance.itemSpacingDp.collectAsState(initial = 10)
    val sidePaddingDp by app.prefs.appearance.sidePaddingDp.collectAsState(initial = 20)
    val nowPlayingHeightDp by app.prefs.home.nowPlayingHeightDp.collectAsState(initial = 64)
    val homePaddings by app.prefs.home.homePaddings.collectAsState(initial = HomePaddings.Default)
    val edgeSide by app.prefs.strip.edgeSide.collectAsState(initial = EdgeSide.RIGHT)
    val scrubStyle by app.prefs.strip.scrubStyle.collectAsState(initial = ScrubStyle.BELL)
    val scrubBubbleShape by app.prefs.strip.scrubBubbleShape.collectAsState(initial = ScrubBubbleShape.ROUNDED)
    val rippleParams by app.prefs.strip.rippleParams.collectAsState(initial = RippleParams())
    val azStripVisibility by app.prefs.strip.azStripVisibility.collectAsState(initial = AzStripVisibility.NEVER)
    val showAlphabet by app.prefs.strip.showAlphabet.collectAsState(initial = true)
    val alignRight by app.prefs.appearance.alignRight.collectAsState(initial = false)
    val dimWallpaperAlpha by app.prefs.appearance.dimWallpaperAlpha.collectAsState(initial = 0.35f)
    val hapticsEnabled by app.prefs.strip.hapticsEnabled.collectAsState(initial = true)
    val dimHomeAlpha by app.prefs.appearance.dimHomeAlpha.collectAsState(initial = 0f)
    val showFavoriteLabels by app.prefs.appearance.showFavoriteLabels.collectAsState(initial = true)
    val textColorMode by app.prefs.appearance.textColorMode.collectAsState(initial = TextColorMode.AUTO)
    val doubleTapToLock by app.prefs.home.doubleTapToLock.collectAsState(initial = false)
    val widgetId by app.prefs.home.widgetId.collectAsState(initial = -1)
    val widgetPosition by app.prefs.home.widgetPosition.collectAsState(initial = 0)
    val widgetHeightDp by app.prefs.home.widgetHeightDp.collectAsState(initial = 180)
    val widgetsStacked by app.prefs.home.widgetsStacked.collectAsState(initial = false)
    val nowPlayingEnabled by app.prefs.home.nowPlayingEnabled.collectAsState(initial = false)
    val folders by app.prefs.library.folders.collectAsState(initial = emptyList())
    val widgetIds by app.prefs.home.widgetIds.collectAsState(initial = emptyList())
    val widgetSidePaddingDp by app.prefs.home.widgetSidePaddingDp.collectAsState(initial = sidePaddingDp)
    val widgetOffsetXDp by app.prefs.home.widgetOffsetXDp.collectAsState(initial = 0)
    val swipeUpOpensList by app.prefs.home.swipeUpOpensList.collectAsState(initial = false)
    val appListSearchEnabled by app.prefs.appList.appListSearchEnabled.collectAsState(initial = false)
    val appListSearchBottom by app.prefs.appList.appListSearchBottom.collectAsState(initial = false)
    val sectionTopPercent by app.prefs.appList.sectionTopPercent.collectAsState(initial = 26)
    val closeFolderOnLaunch by app.prefs.home.closeFolderOnLaunch.collectAsState(initial = false)
    val showListHeaders by app.prefs.appList.showListHeaders.collectAsState(initial = true)
    val showSettingsRow by app.prefs.home.showSettingsRow.collectAsState(initial = true)
    val cornerButtonKey by app.prefs.library.cornerButtonKey.collectAsState(initial = null)
    val autoKeyboard by app.prefs.appList.autoKeyboard.collectAsState(initial = false)
    val lastLetterToLine by app.prefs.appList.lastLetterToLine.collectAsState(initial = false)
    val showFavoriteIcons by app.prefs.appearance.showFavoriteIcons.collectAsState(initial = true)
    val favoritesSource by app.prefs.library.favoritesSource.collectAsState(initial = FavoritesSource.MANUAL)
    val frequentCount by app.prefs.library.frequentCount.collectAsState(initial = 6)
    val appListSearchHidden by app.prefs.appList.appListSearchHidden.collectAsState(initial = false)
    val sortByUsage by app.prefs.library.sortByUsage.collectAsState(initial = false)
    val launchCounts by app.prefs.library.launchCounts.collectAsState(initial = emptyMap())
    val edgeZoneWidthDp by app.prefs.strip.edgeZoneWidthDp.collectAsState(initial = 56)
    val quickLaunchLeftKey by app.prefs.library.quickLaunchLeft.collectAsState(initial = null)
    val quickLaunchRightKey by app.prefs.library.quickLaunchRight.collectAsState(initial = null)
    val showAppIcons by app.prefs.appearance.showAppIcons.collectAsState(initial = true)
    val notificationBadges by app.prefs.appList.notificationBadges.collectAsState(initial = false)
    val notificationPreviews by app.prefs.appList.notificationPreviews.collectAsState(initial = false)
    val notificationTextSp by app.prefs.appList.notificationTextSp.collectAsState(initial = 12)
    val fontFile by app.prefs.appearance.fontFile.collectAsState(initial = null)
    val textColorCustom by app.prefs.appearance.textColorCustom.collectAsState(initial = 0xFFFFFFFF.toInt())
    val dimColor by app.prefs.appearance.dimColor.collectAsState(initial = 0xFF000000.toInt())
    val rotatesByDefault = LocalConfiguration.current.smallestScreenWidthDp >= 600
    val allowRotationPref by app.prefs.home.allowRotation.collectAsState(initial = null)
    val shortcutSwipe by app.prefs.home.shortcutSwipe.collectAsState(initial = ShortcutSwipe.RIGHT)
    val allowRotation = allowRotationPref ?: rotatesByDefault
    val iconShape by app.prefs.appearance.iconShape.collectAsState(initial = IconShape.SYSTEM)
    val themedIcons by app.prefs.appearance.themedIcons.collectAsState(initial = false)
    val alignment by app.prefs.appearance.alignment.collectAsState(initial = HomeAlignment.LEFT)
    val appListAlignment by app.prefs.appearance.appListAlignment.collectAsState(initial = HomeAlignment.LEFT)
    val iconSide by app.prefs.appearance.iconSide.collectAsState(initial = IconSide.LEFT)
    val statusBarPeekSeconds by app.prefs.appearance.statusBarPeekSeconds.collectAsState(initial = 5)
    val scrubBand by app.prefs.strip.scrubBand.collectAsState(initial = null)
    val layoutDefaultsVersion by app.prefs.home.layoutDefaultsVersion.collectAsState(initial = null)

    // A brand new launcher gets its own clock, so the home screen is not simply empty on first
    // sight. Only ever on an install that has never been set up — the marker is written once,
    // before anything the user does — and only while there is no widget at all, so nobody's
    // arrangement is added to and a widget they removed does not come back.
    LaunchedEffect(layoutDefaultsVersion, widgetIds) {
        if (layoutDefaultsVersion != 1 || widgetIds.isNotEmpty()) return@LaunchedEffect
        val manager = AppWidgetManager.getInstance(context)
        val id = app.widgetHost.allocateAppWidgetId()
        val bound = runCatching {
            manager.bindAppWidgetIdIfAllowed(id, ClockWidgetProvider.componentName(context))
        }.getOrDefault(false)
        if (!bound) {
            // Binding is refused unless this launcher holds the home role, which it may not
            // yet on the very first run. The id is given back rather than left stranded.
            runCatching { app.widgetHost.deleteAppWidgetId(id) }
            return@LaunchedEffect
        }
        ClockWidgetProvider.render(context, manager, id)
        app.prefs.home.addWidgetId(id)
    }
    val welcomeSeen by app.prefs.home.welcomeSeen.collectAsState(initial = true)
    val hasCustomLayout by app.prefs.home.hasCustomLayout.collectAsState(initial = true)
    val hasCustomFavoritesTop by app.prefs.home.hasCustomFavoritesTop.collectAsState(initial = true)
    var settingsTick by remember { mutableIntStateOf(0) }
    val contentColor = rememberContentColor(textColorMode, textColorCustom)

    val appsByKey = remember(allApps) { allApps.associateBy { it.key } }
    // The number the hidden-apps screen itself arrives at, rather than the size of the stored
    // set: that screen lists rows, and a hidden app inside a locked private space has no row.
    // Counting the stored set in the subtitle says out loud how many apps are in there.
    val hiddenShownCount = remember(allApps, hiddenApps) { allApps.count { it.key in hiddenApps } }
    val foldersById = remember(folders) { folders.associateBy { it.id } }

    // A favorites row is an app or a folder; both come out of the same ordered token list.
    //
    // Or out of what has actually been opened, when the section is set to keep itself. The rows
    // are the same rows either way — it is the list that is computed, not what is drawn from it
    // — so nothing downstream knows or cares which of the two it was handed.
    //
    // Folders are not in the computed list: a folder is something someone made, and there is no
    // count of openings that would put one together.
    val favoriteEntries = remember(
        favoritesSource,
        favoriteKeys,
        appsByKey,
        foldersById,
        if (favoritesSource == FavoritesSource.FREQUENT) launchCounts else emptyMap(),
        frequentCount,
        frequentExcluded,
    ) {
        if (favoritesSource == FavoritesSource.FREQUENT) {
            launchCounts.asSequence()
                .filter { it.value > 0 }
                // Before take, so the next most-used app moves up into the slot rather than
                // the row simply going missing.
                .filter { it.key !in frequentExcluded }
                .mapNotNull { (key, count) -> appsByKey[key]?.let { it to count } }
                .sortedWith(
                    compareByDescending<Pair<AppInfo, Int>> { it.second }
                        .thenBy { it.first.label.lowercase() }
                )
                .take(frequentCount)
                .map { FavoriteEntry.App(it.first) }
                .toList()
        } else {
            favoriteKeys.mapNotNull { token ->
                val folderId = folderIdFromToken(token)
                if (folderId != null) {
                    foldersById[folderId]?.let { FavoriteEntry.FolderRef(it) }
                } else {
                    appsByKey[token]?.let { FavoriteEntry.App(it) }
                }
            }
        }
    }

    // Rasterise icons in the background so opening the A-Z list doesn't have to. Favorites and
    // folder members go first: they are what the home screen needs before anything else.
    val listIconPx = with(LocalDensity.current) { iconSizeDp.dp.roundToPx() }
    val priorityKeys = remember(favoriteKeys, folders) {
        favoriteKeys.toSet() + folders.flatMap { it.apps }
    }
    // Same style the rows will ask for, or the warm pass fills the cache under keys nothing
    // then looks up, and every icon is rasterised twice.
    val iconCfg = LocalIconConfig.current
    val scheme = MaterialTheme.colorScheme
    val iconStyle = remember(iconCfg, scheme) {
        IconStyle(
            shape = iconCfg.shape,
            themed = iconCfg.themed,
            background = scheme.primaryContainer.toArgb(),
            foreground = scheme.onPrimaryContainer.toArgb(),
        )
    }
    val cacheGeneration by iconCacheGeneration.collectAsState()
    LaunchedEffect(allApps, iconPackPackage, iconOverrides, listIconPx, priorityKeys, iconStyle, cacheGeneration) {
        warmIconCache(context, allApps, iconPackPackage, iconOverrides, listIconPx, iconStyle, priorityKeys)
    }

    val settings = HomeSettings(
        iconSizeDp = iconSizeDp,
        labelSizeSp = labelSizeSp,
        itemSpacingDp = itemSpacingDp,
        sidePaddingDp = sidePaddingDp,
        nowPlayingHeightDp = nowPlayingHeightDp,
        nowPlayingEnabled = nowPlayingEnabled,
        edgeSide = edgeSide,
        scrubStyle = scrubStyle,
        scrubBubbleShape = scrubBubbleShape,
        rippleParams = rippleParams,
        azStripVisibility = azStripVisibility,
        shortcutSwipe = shortcutSwipe,
        showAlphabet = showAlphabet,
        alignment = alignment,
        appListAlignment = appListAlignment,
        iconSide = iconSide,
        widgetSidePaddingDp = widgetSidePaddingDp,
        widgetOffsetXDp = widgetOffsetXDp,
        edgeZoneWidthDp = edgeZoneWidthDp,
        swipeUpOpensAppList = swipeUpOpensList,
        appListSearch = appListSearchEnabled,
        appListSearchBottom = appListSearchBottom,
        sectionTopPercent = sectionTopPercent,
        closeFolderOnLaunch = closeFolderOnLaunch,
        showListHeaders = showListHeaders,
        showSettingsRow = showSettingsRow,
        showFavoriteIcons = showFavoriteIcons,
        favoritesEverSet = favoritesEverSet,
        edgeZoneBandOnly = edgeZoneBandOnly,
        holdScroll = holdScroll,
        dimOthersScrubbing = dimOthersScrubbing,
        markWorkApps = markWorkApps,
        textShadow = textShadow,
        textShadowStrength = textShadowStrength,
        compactTapArea = compactTapArea,
        stayAfterPrivateUnlock = stayAfterPrivateUnlock,
        privateSpaceSection = privateSpaceSection,
        foldersInAppList = foldersInAppList,
        recentSection = recentSection,
        recentSectionCount = recentSectionCount,
        edgeZoneInsetDp = edgeZoneInsetDp,
        edgeZoneInsetAuto = edgeZoneInsetAuto,
        webSearchFallback = webSearchFallback,
        swipeUpOpensSearch = swipeUpOpensSearch,
        holdScrollSpeed = holdScrollSpeed,
        favoritesSource = favoritesSource,
        appListSearchHidden = appListSearchHidden,
        hideStatusBarAppList = hideStatusBarAppList,
        sortByUsage = sortByUsage,
        quickLaunchLeft = quickLaunchLeftKey?.let { appsByKey[it] },
        quickLaunchRight = quickLaunchRightKey?.let { appsByKey[it] },
        // Both flows start null/true so nothing is centered or offered before the stored
        // answer arrives; a legacy install is stamped 0 and never enters either path.
        centerFavorites = layoutDefaultsVersion == 1 && !hasCustomFavoritesTop,
        dimWallpaperAlpha = dimWallpaperAlpha,
        dimHomeAlpha = dimHomeAlpha,
        dimColor = dimColor,
        hapticsEnabled = hapticsEnabled,
        showFavoriteLabels = showFavoriteLabels,
        doubleTapToLock = doubleTapToLock,
        contentColor = contentColor,
    )

    var pendingIconTarget by remember { mutableStateOf<String?>(null) }

    val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        val target = pendingIconTarget
        if (uri != null && target != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val folderId = folderIdFromToken(target)
            scope.launch {
                if (folderId != null) {
                    app.prefs.library.setFolderIcon(folderId, uri.toString())
                } else {
                    app.prefs.library.setIconOverride(target, uri.toString())
                }
            }
        }
        pendingIconTarget = null
    }

    val widgetPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val id = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
            // Appended rather than replacing: picking a widget now adds a page.
            if (id != -1) scope.launch { app.prefs.home.addWidgetId(id) }
        }
    }

    // The folder a widget being picked is for. Held here rather than passed through the
    // picker, which only knows about widget ids.
    var folderWidgetTarget by rememberSaveable { mutableStateOf<String?>(null) }
    val folderWidgetPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val folderId = folderWidgetTarget
        folderWidgetTarget = null
        if (result.resultCode == RESULT_OK && folderId != null) {
            val id = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
            if (id != -1) scope.launch { app.prefs.library.addFolderWidget(folderId, id) }
        }
    }

    val widgetActions = remember {
        WidgetSlotActions(
            onAddWidget = { widgetPickerLauncher.launch(Intent(context, WidgetPickerActivity::class.java)) },
            onRemoveWidget = { id ->
                scope.launch {
                    // Releasing the host id matters: left allocated, its provider keeps
                    // broadcasting updates to a widget nobody can see.
                    if (id > 0) app.widgetHost.deleteAppWidgetId(id)
                    app.prefs.home.removeWidgetId(id)
                }
            },
            onWidgetSettings = { id ->
                val configure = AppWidgetManager.getInstance(context).getAppWidgetInfo(id)?.configure
                if (configure != null) {
                    val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                        component = configure
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                    }
                    runCatching { context.startActivity(intent) }
                }
            },
            onAppInfo = { id ->
                AppWidgetManager.getInstance(context).getAppWidgetInfo(id)
                    ?.let { app.appRepository.openAppInfo(it.provider.packageName) }
            },
            onResize = { newHeight -> scope.launch { app.prefs.home.setWidgetHeightDp(newHeight) } },
            onOpenSettings = { navController.navigate("settings") },
            onAddWidgetToFolder = { folderId ->
                folderWidgetTarget = folderId
                folderWidgetPickerLauncher.launch(Intent(context, WidgetPickerActivity::class.java))
            },
            onRemoveFolderWidget = { folderId, id ->
                scope.launch {
                    if (id > 0) runCatching { app.widgetHost.deleteAppWidgetId(id) }
                    app.prefs.library.removeFolderWidget(folderId, id)
                }
            },
            onResizeFolderWidget = { folderId, id, height ->
                scope.launch { app.prefs.library.setFolderWidgetHeight(folderId, id, height) }
            },
            onSetFolderNowPlaying = { folderId, on ->
                scope.launch { app.prefs.library.setFolderNowPlaying(folderId, on) }
            },
        )
    }

    // HOME has to unwind the whole stack. The equivalent effect inside the home destination
    // can't do this: that destination isn't composed while Settings is on screen.
    LaunchedEffect(homeIntentTick) {
        if (homeIntentTick > 0 && navController.currentDestination?.route != "home") {
            navController.popBackStack("home", inclusive = false)
        }
    }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeRoute(
                cornerButtonApp = cornerButtonKey?.takeIf { it != EntryKeys.SEARCH }?.let { appsByKey[it] },
                cornerButtonIsSearch = cornerButtonKey == EntryKeys.SEARCH,
                autoKeyboard = autoKeyboard,
                lastLetterToLine = lastLetterToLine,
                app = app,
                homeIntentTick = homeIntentTick,
                typedToSearch = typedToSearch,
                onTypedToSearchHandled = onTypedToSearchHandled,
                settings = settings,
                homePaddings = homePaddings,
                favorites = favoriteEntries,
                appsByKey = appsByKey,
                folders = folders,
                favoriteKeys = favoriteKeys,
                hiddenApps = hiddenApps,
                nameOverrides = nameOverrides,
                widgetIds = widgetIds,
                widgetPosition = widgetPosition,
                widgetHeightDp = widgetHeightDp,
                widgetsStacked = widgetsStacked,
                widgetActions = widgetActions,
                launchCounts = launchCounts,
                // Both flows start at a value that shows nothing, so the dialog can't flash
                // before the stored answer arrives. A legacy install is stamped 0 and never
                // qualifies.
                showWelcome = layoutDefaultsVersion == 1 && !welcomeSeen,
                onWelcomeDismissed = {
                    scope.launch {
                        app.prefs.home.applyNewInstallDefaults()
                        app.prefs.home.setWelcomeSeen(true)
                    }
                },
                scrubBandFractions = scrubBand,
                onSetScrubBand = { top, height -> scope.launch { app.prefs.strip.setScrubBand(top, height) } },
                onClearScrubBand = { scope.launch { app.prefs.strip.clearScrubBand() } },
                onPeekStatusBar = onPeekStatusBar,
                onAppListVisibleChange = onAppListVisibleChange,
                onTogglePrivateSpace = {
                    scope.launch {
                        val locked = app.appRepository.togglePrivateSpace()
                        // Taken after the system has answered, not before: on a phone with a
                        // screen lock the authentication in front of an unlock takes as long
                        // as the person does, and a pass held open across it would supersede
                        // every reload started while the dialog was up.
                        val pass = reloadOrder.begin()
                        // Granted means locked, and locked is concealed here and now: the
                        // broadcast that says so arrives well after the profile has stopped.
                        if (locked != null) adoptPrivateSpace(locked, pass)
                        clearIconCache()
                        // Enumerated against the lock rather than against what the system says
                        // this instant, which for the next moment is still an open space.
                        reloadApps(locked, pass)
                    }
                },
                onNavigate = { route -> navController.navigate(route) },
            )
        }

        composable("settings") {
            SettingsRoute(
                app = app,
                allApps = allApps,
                privateSpace = privateSpace,
                homeTick = homeIntentTick,
                nav = SettingsNav(
                    onHiddenApps = { navController.navigate("settings/hidden") },
                    onFrequentExcluded = { navController.navigate("settings/frequentexcluded") },
                    onFavorites = { navController.navigate("favorites") },
                    onQuickLaunchPicker = { slot -> navController.navigate("apppicker/" + slot.name) },
                    onCornerPicker = { navController.navigate("cornerpicker") },
                ),
                onBack = { navController.popBackStack() },
            )
        }

        composable("favorites") {
            ManageFavoritesScreen(
                allApps = allApps,
                favoriteKeys = favoriteKeys,
                folders = folders,
                privateSpace = privateSpace,
                nameOverrides = nameOverrides,
                iconSizeDp = iconSizeDp,
                onReorder = { keys -> scope.launch { app.prefs.library.setFavorites(keys) } },
                onSetFavorite = { appInfo, add ->
                    scope.launch {
                        if (add) app.prefs.library.addFavorite(appInfo.key) else app.prefs.library.removeFavorite(appInfo.key)
                    }
                },
                // Missing here can just mean a locked private space or a paused work profile,
                // both of which come back — so this only drops the favorite, the same as
                // unticking a row that IS resolved. forgetEntry's wider wipe (rename, icon,
                // folder, hidden, launch count) is for a shortcut that is actually gone for
                // good, which AppRepository.unpin already calls on its own.
                onForget = { key -> scope.launch { app.prefs.library.removeFavorite(key) } },
                onBack = { navController.popBackStack() },
            )
        }

        composable("folder/{id}") { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            FolderAppsScreen(
                folder = foldersById[id],
                allApps = allApps,
                privateSpace = privateSpace,
                nameOverrides = nameOverrides,
                iconSizeDp = iconSizeDp,
                onSetInFolder = { appInfo, inFolder ->
                    scope.launch {
                        if (inFolder) {
                            app.prefs.library.addAppToFolder(id, appInfo.key)
                        } else {
                            app.prefs.library.removeAppFromFolder(id, appInfo.key)
                        }
                    }
                },
                onReorder = { keys -> scope.launch { app.prefs.library.setFolderApps(id, keys) } },
                onForget = { key -> scope.launch { app.prefs.library.removeAppFromFolder(id, key) } },
                onBack = { navController.popBackStack() },
            )
        }

        composable("apppicker/{slot}") { entry ->
            val slot = runCatching {
                QuickLaunchSlot.valueOf(entry.arguments?.getString("slot").orEmpty())
            }.getOrDefault(QuickLaunchSlot.LEFT)
            AppPickerScreen(
                title = stringResource(
                    if (slot == QuickLaunchSlot.LEFT) R.string.settings_quick_launch_left
                    else R.string.settings_quick_launch_right
                ),
                allApps = allApps,
                selectedKey = if (slot == QuickLaunchSlot.LEFT) quickLaunchLeftKey else quickLaunchRightKey,
                nameOverrides = nameOverrides,
                iconSizeDp = iconSizeDp,
                onPick = { picked ->
                    scope.launch { app.prefs.library.setQuickLaunch(slot, picked?.key) }
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable("cornerpicker") {
            AppPickerScreen(
                title = stringResource(R.string.settings_corner_button),
                allApps = allApps,
                selectedKey = cornerButtonKey,
                extraChoice = EntryKeys.SEARCH to stringResource(R.string.settings_corner_search),
                onPickExtra = { key ->
                    scope.launch { app.prefs.library.setCornerButton(key) }
                    navController.popBackStack()
                },
                nameOverrides = nameOverrides,
                iconSizeDp = iconSizeDp,
                onPick = { picked ->
                    scope.launch { app.prefs.library.setCornerButton(picked?.key) }
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable("settings/hidden") {
            HiddenAppsScreen(
                allApps = allApps,
                hiddenApps = hiddenApps,
                nameOverrides = nameOverrides,
                iconSizeDp = iconSizeDp,
                onToggleHidden = { appInfo, hidden ->
                    scope.launch { app.prefs.library.setHidden(appInfo.key, hidden) }
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable("settings/frequentexcluded") {
            HiddenAppsScreen(
                allApps = allApps,
                hiddenApps = frequentExcluded,
                nameOverrides = nameOverrides,
                iconSizeDp = iconSizeDp,
                onToggleHidden = { appInfo, excluded ->
                    scope.launch { app.prefs.library.setFrequentExcluded(appInfo.key, excluded) }
                },
                onBack = { navController.popBackStack() },
                titleRes = R.string.settings_frequent_excluded,
                countRes = R.string.frequent_excluded_count,
                addRes = R.string.frequent_excluded_add,
            )
        }

        composable("iconpicker/{key}") { entry ->
            val key = entry.arguments?.getString("key")?.let { Uri.decode(it) }.orEmpty()
            val folderId = folderIdFromToken(key)
            val targetApp = allApps.find { it.key == key }
            val label = when {
                folderId != null -> foldersById[folderId]?.name.orEmpty()
                else -> nameOverrides[key] ?: targetApp?.label.orEmpty()
            }

            fun applyIcon(value: String?) {
                scope.launch {
                    if (folderId != null) {
                        app.prefs.library.setFolderIcon(folderId, value)
                    } else {
                        app.prefs.library.setIconOverride(key, value)
                    }
                }
            }

            IconPickerScreen(
                appLabel = label,
                onPickPackIcon = { packPkg, drawableName ->
                    applyIcon(encodePackOverride(packPkg, drawableName))
                    navController.popBackStack()
                },
                onPickFromGallery = {
                    pendingIconTarget = key
                    pickImageLauncher.launch(arrayOf("image/*"))
                },
                onResetIcon = {
                    applyIcon(null)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() },
            )
        }
    }
}

/**
 * The app list and the private-space state it was enumerated against, held as one value.
 *
 * Two pieces of state would let a composition see a list from one read beside a state from
 * another, and every consumer needs them to agree: the settings screens decide what to conceal
 * from the state and then look the rest up in the list, and the home screen draws the padlock
 * from one and the rows from the other. Published together, there is no such moment.
 */
private data class AppsAndSpace(val privateSpace: PrivateSpace, val apps: List<AppInfo>)
