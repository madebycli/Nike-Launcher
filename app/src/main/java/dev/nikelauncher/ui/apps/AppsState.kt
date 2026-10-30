// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.apps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.os.UserHandle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import dev.nikelauncher.NikeApp
import dev.nikelauncher.data.AppInfo
import dev.nikelauncher.data.EntryKind
import dev.nikelauncher.data.PrivateSpace
import dev.nikelauncher.ui.ReloadOrder
import dev.nikelauncher.ui.common.clearIconCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The app list and the private-space state it was enumerated against, held as one value.
 *
 * Two pieces of state would let a composition see a list from one read beside a state from
 * another, and every consumer needs them to agree: the settings screens decide what to conceal
 * from the state and then look the rest up in the list, and the home screen draws the padlock
 * from one and the rows from the other. Published together, there is no such moment.
 */
@Stable
class AppsState internal constructor(private val app: NikeApp, private val scope: CoroutineScope) {
    private var snapshot by mutableStateOf(Snapshot(PrivateSpace.Absent, emptyList()))
    private val order = ReloadOrder()

    val apps: List<AppInfo> get() = snapshot.apps
    val privateSpace: PrivateSpace get() = snapshot.privateSpace

    private class Snapshot(val privateSpace: PrivateSpace, val apps: List<AppInfo>)

    /**
     * [known] is the state to enumerate against when the caller already has one it trusts more than
     * a fresh read, such as a lock it has just been granted, which the system has not finished
     * applying. [pass] is the number this reload took from [order] before it read anything.
     */
    internal suspend fun reload(known: PrivateSpace?, pass: Long) {
        val (state, apps) = withContext(Dispatchers.Default) {
            // Resolved once and handed on, so the list and what the settings screens conceal can
            // never disagree about whether the space was open when it was read.
            val state = known ?: app.appRepository.privateSpace()
            state to app.appRepository.queryAllApps(state)
        }
        // Overtaken while it ran: this list was enumerated against a state that is no longer the
        // newest thing known, and publishing it would put a locked space's apps back under an open
        // padlock. The pass that overtook this one publishes a list of its own.
        if (order.mayPublish(pass)) snapshot = Snapshot(state, apps)
    }

    /**
     * Acts on a state that has just been resolved at once, on the list already in hand, before the
     * reload that will take a moment. A lock that only takes effect at the end of a full
     * enumeration has left the names on a home screen for exactly that long.
     */
    internal fun adopt(state: PrivateSpace, pass: Long) {
        if (!order.mayPublish(pass)) return
        snapshot = Snapshot(
            state,
            snapshot.apps.filterNot { it.kind == EntryKind.PRIVATE_SPACE || state.conceals(it.key) } +
                app.appRepository.privateSpaceRow(state),
        )
    }

    /** Re-reads the private space first and on its own, then everything, as a package change does. */
    internal fun refresh() {
        scope.launch {
            // Taken before the read, so anything already enumerating is superseded by the moment
            // this pass looked rather than by the moment it finished.
            val pass = order.begin()
            val state = withContext(Dispatchers.Default) { app.appRepository.privateSpace() }
            adopt(state, pass)
            // An app that ships a new icon in an update changes none of the cache key's
            // components, so nothing else would invalidate the stale bitmap.
            clearIconCache()
            reload(state, pass)
        }
    }

    /** Locks an open private space or asks for a locked one to be opened. */
    fun togglePrivateSpace() {
        scope.launch {
            val locked = app.appRepository.togglePrivateSpace()
            // Taken after the system has answered, not before: with a screen lock the
            // authentication in front of an unlock takes as long as the person does, and a pass
            // held open across it would supersede every reload started while it was up.
            val pass = order.begin()
            // Granted means locked, and locked is concealed here and now: the broadcast that says
            // so arrives well after the profile has stopped.
            if (locked != null) adopt(locked, pass)
            clearIconCache()
            // Enumerated against the lock rather than against what the system says this instant.
            reload(locked, pass)
        }
    }

    internal fun probeOnStart() {
        scope.launch {
            val probe = withContext(Dispatchers.Default) { app.appRepository.privateSpace() }
            if (probe == privateSpace) {
                // Android's "hide when locked" is set in Settings while the space stays locked, so
                // the state reads the same and only the padlock row differs.
                val row = withContext(Dispatchers.Default) { app.appRepository.privateSpaceRow(probe) }
                val shown = apps.filter { it.kind == EntryKind.PRIVATE_SPACE }
                if (row.map { it.componentName } == shown.map { it.componentName }) return@launch
                val pass = order.begin()
                adopt(withContext(Dispatchers.Default) { app.appRepository.privateSpace() }, pass)
                return@launch
            }
            // Only once there is something to do: a number taken to decide nothing changed would
            // supersede a reload already running. The number is taken first and the space is read
            // again under it, since something newer may have published in between.
            val pass = order.begin()
            val state = withContext(Dispatchers.Default) { app.appRepository.privateSpace() }
            adopt(state, pass)
            clearIconCache()
            reload(state, pass)
        }
    }

    internal fun beginPass(): Long = order.begin()
}

/**
 * Enumerating every installed app costs a PackageManager round trip per app; doing it in the first
 * composition is what stalled the cold start, so it is loaded off the main thread and the home screen
 * renders against an empty list for the first frame. Kept current as packages, shortcuts and the
 * private space change.
 */
@Composable
fun rememberAppsState(app: NikeApp): AppsState {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state = remember(app) { AppsState(app, scope) }

    // A shortcut pinned through the confirm screen, or unpinned from a menu, changes what there is
    // to list without any package changing, so nothing else would notice.
    val shortcutChanges by app.appRepository.shortcutChanges.collectAsState()
    LaunchedEffect(shortcutChanges) { state.reload(known = null, pass = state.beginPass()) }

    DisposableEffect(state) {
        val launcherApps = context.getSystemService(LauncherApps::class.java)
        // LauncherApps reports package changes across every profile. The PACKAGE_* broadcasts only
        // describe the profile we run in, so an app installed into a work profile or a private
        // space would never reach the list.
        val callback = object : LauncherApps.Callback() {
            override fun onPackageAdded(packageName: String?, user: UserHandle?) = state.refresh()
            override fun onPackageRemoved(packageName: String?, user: UserHandle?) = state.refresh()
            override fun onPackageChanged(packageName: String?, user: UserHandle?) = state.refresh()
            override fun onPackagesAvailable(names: Array<out String>?, user: UserHandle?, replacing: Boolean) = state.refresh()
            override fun onPackagesUnavailable(names: Array<out String>?, user: UserHandle?, replacing: Boolean) = state.refresh()
            // A shortcut can be added, renamed, re-iconed or switched off while we are showing it.
            override fun onShortcutsChanged(packageName: String, shortcuts: MutableList<ShortcutInfo>, user: UserHandle) = state.refresh()
        }
        runCatching { launcherApps.registerCallback(callback) }

        // Locking or unlocking a private space changes no package, so it arrives as one of these.
        // Written as strings because the Intent constants are newer than this app's minimum.
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE)
            addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE)
            listOf("AVAILABLE", "UNAVAILABLE", "ACCESSIBLE", "INACCESSIBLE", "ADDED", "REMOVED")
                .forEach { addAction("android.intent.action.PROFILE_$it") }
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) = state.refresh()
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose {
            runCatching { launcherApps.unregisterCallback(callback) }
            context.unregisterReceiver(receiver)
        }
    }

    // The system re-locks the space by itself whenever the screen goes off, and a lock that was
    // missed stays missed until something else reloads. Coming back to the launcher is the moment
    // that matters, so the state is read again there.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, state) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_START) state.probeOnStart() }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(state) { state.reload(known = null, pass = state.beginPass()) }
    return state
}
