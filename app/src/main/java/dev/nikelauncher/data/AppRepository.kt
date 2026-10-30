// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.data

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat
import dev.nikelauncher.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class AppRepository(
    private val context: Context,
    private val prefs: Prefs,
    private val scope: CoroutineScope,
) {

    private val profileAccess = ProfileAccess(context)
    private val privateSpaces = PrivateSpaceAccess(context, profileAccess)
    private val shortcuts = ShortcutAccess(context, prefs, scope, profileAccess)

    private val pm: PackageManager get() = profileAccess.pm
    private val launcherApps: LauncherApps get() = profileAccess.launcherApps
    private val userManager: UserManager get() = profileAccess.userManager

    val shortcutChanges: StateFlow<Int> get() = shortcuts.shortcutChanges

    fun privateSpace(): PrivateSpace = privateSpaces.privateSpace()
    fun privateSpaceRow(state: PrivateSpace): List<AppInfo> = privateSpaces.privateSpaceRow(state)
    suspend fun togglePrivateSpace(): PrivateSpace? = privateSpaces.togglePrivateSpace()
    fun openPrivateSpaceStore(from: Context): Boolean = privateSpaces.openPrivateSpaceStore(from)
    fun openPrivateSpaceSettings(from: Context): Boolean = privateSpaces.openPrivateSpaceSettings(from)

    fun appShortcuts(app: AppInfo): List<ShortcutInfo> = shortcuts.appShortcuts(app)
    fun startAppShortcut(shortcut: ShortcutInfo): Boolean = shortcuts.startAppShortcut(shortcut)
    fun shortcutIcon(shortcut: ShortcutInfo, densityDpi: Int): Drawable? = shortcuts.shortcutIcon(shortcut, densityDpi)
    fun unpin(app: AppInfo) = shortcuts.unpin(app)
    fun confirmPinnedShortcut(pkg: String, id: String, user: UserHandle, serial: Long) =
        shortcuts.confirmPinnedShortcut(pkg, id, user, serial)

    /**
     * Every launchable activity across every profile the launcher may show, decided profile by
     * profile before any of it is turned into rows.
     *
     * LauncherApps rather than PackageManager, because queryIntentActivities only ever sees
     * the profile we are running in — a work profile or a private space is invisible to it.
     * LauncherApps also hands back the badged icon and the per-profile label, which is what
     * marks a work app as a work app.
     *
     * The decision is made here, by UserHandle, rather than by filtering finished rows by
     * serial afterwards, because a profile whose serial could not be read produces rows whose
     * keys are indistinguishable from the main profile's — there is nothing left to filter by
     * at that point, and those keys would be stored into favorites and launch counts.
     */
    fun queryAllApps(privateSpace: PrivateSpace = privateSpace()): List<AppInfo> {
        val profiles = runCatching { userManager.userProfiles }.getOrNull().orEmpty()
        val mainUser = Process.myUserHandle()
        return profiles
            .flatMap { user ->
                val serial = profileAccess.listableSerial(user, mainUser) ?: return@flatMap emptyList()
                val work = profileAccess.isWorkProfile(user, mainUser)
                val privateProfile = profileAccess.userType(user) == USER_TYPE_PROFILE_PRIVATE
                // One call for the lot: asking per package turns a list build into
                // hundreds of round trips to the package manager.
                val installTimes = profileAccess.installTimes()
                // Asking about a profile we are not the launcher for throws rather than
                // returning nothing, and one inaccessible profile must not lose the rest.
                runCatching { launcherApps.getActivityList(null, user) }
                    .getOrNull()
                    .orEmpty()
                    .map { info ->
                        AppInfo(
                            componentName = info.componentName,
                            label = info.label?.toString() ?: info.componentName.packageName,
                            user = user,
                            userSerial = serial,
                            work = work,
                            privateProfile = privateProfile,
                            installedAt = installTimes[info.componentName.packageName] ?: 0L,
                        )
                    }
            }
            // Launching ourselves through the MAIN+LAUNCHER filter starts a task that isn't
            // rooted at HOME: it shows up in the app switcher and leaves the system unsure
            // which task is home until the default launcher is set again. Nothing good comes
            // of listing the launcher inside its own app list.
            .filterNot { it.componentName.packageName == context.packageName }
            // After that filter rather than before it: the reason for it is that starting our
            // own launcher activity corrupts the HOME task, which says nothing about a
            // shortcut, and one we published ourselves is started through LauncherApps like
            // any other.
            .plus(shortcuts.queryPinnedShortcuts())
            // Belt as well as braces, and after the shortcuts so that it covers them too. The
            // profiles above are read one call at a time, so the lock can be granted between
            // the state this list was asked for and the quiet mode read here — and it is
            // granted before the profile has actually stopped. A caller that has just locked
            // the space hands that state in, and this drops what it names however the system
            // happens to be answering at this instant.
            .filterNot { privateSpace.conceals(it.key) }
            .distinctBy { it.key }
            .sortedBy { it.label.lowercase() }
            // After the own-package filter, which would otherwise drop it: the row is ours.
            .plus(privateSpaceRow(privateSpace))
    }

    /** Badged by the system, so a work or private-space app is recognizable at a glance. */
    fun loadIcon(app: AppInfo): Drawable {
        // Ours, and drawn from our own resources, so there is nothing to ask the system for.
        if (app.kind == EntryKind.PRIVATE_SPACE) return privateSpaceIcon(app)
        // A shortcut's own picture, badged with the app that published it. Falling through
        // means it has none of its own, and the publisher's app icon below is the answer.
        if (app.kind == EntryKind.SHORTCUT) shortcuts.iconFor(app)?.let { return it }
        val user = app.user
        if (user != null) {
            val activity = runCatching {
                launcherApps.getActivityList(app.componentName.packageName, user)
                    .firstOrNull { it.componentName == app.componentName }
            }.getOrNull()
            activity?.let { info ->
                runCatching { info.getBadgedIcon(0) }.getOrNull()?.let { return it }
            }
        }
        return try {
            pm.getActivityIcon(app.componentName)
        } catch (e: PackageManager.NameNotFoundException) {
            try {
                pm.getApplicationIcon(app.componentName.packageName)
            } catch (e2: PackageManager.NameNotFoundException) {
                pm.defaultActivityIcon
            }
        }
    }


    // An app's label comes back in the device's language, so on a Japanese phone the English
    // name is nowhere in the list. Loading it means asking the app's own resources for the
    // label a second time through an English configuration, which is a whole resource table
    // per app — so it is only ever asked for a name that has no A-Z letter of its own, and the
    // answer is kept.
    private val englishLabels = mutableMapOf<String, String?>()

    /**
     * The app's name in English, or null if it has none or it is the name we already have.
     *
     * Null for anything that is not an app: a shortcut is named by whoever pinned it, and
     * there are no resources of its own to ask a second time.
     */
    fun englishLabel(app: AppInfo): String? {
        if (app.kind != EntryKind.APP) return null
        return englishLabels.getOrPut(app.key) {
            runCatching {
                val info = pm.getActivityInfo(app.componentName, 0)
                val labelRes = if (info.labelRes != 0) info.labelRes else info.applicationInfo.labelRes
                if (labelRes == 0) return@runCatching null
                val res = pm.getResourcesForApplication(info.applicationInfo)
                val config = Configuration(res.configuration).apply { setLocale(Locale.ENGLISH) }
                res.getString(labelRes).takeIf { it.isNotBlank() }?.let { english ->
                    // The context-adjusted resources fall back to the default language when an app
                    // ships no English, which just hands the same name back.
                    Resources(res.assets, res.displayMetrics, config).getString(labelRes)
                }
            }.getOrNull()
        }
    }

    /** A closed padlock while the space is locked, an open one while it is not. */
    private fun privateSpaceIcon(app: AppInfo): Drawable {
        val locked = app.componentName.className == PRIVATE_SPACE_LOCKED_CLASS
        val id = if (locked) R.mipmap.ic_private_space_locked else R.mipmap.ic_private_space_unlocked
        return ContextCompat.getDrawable(context, id) ?: pm.defaultActivityIcon
    }

    /**
     * Returns false if the app could not be started, so callers can undo whatever they hid.
     */
    fun launch(app: AppInfo): Boolean {
        if (app.kind == EntryKind.SHORTCUT) return launchShortcut(app)
        // Only an app has an activity to start. The private space row is opened by the screen
        // that knows what it is, and arriving here means it was routed wrongly — which must not
        // launch anything and must not be counted as a launch.
        if (app.kind != EntryKind.APP) return false
        if (app.componentName.packageName == context.packageName) return false
        val started = runCatching {
            // Through LauncherApps so an app in another profile starts as that profile; a
            // plain startActivity would look for it in ours and find nothing.
            launcherApps.startMainActivity(app.componentName, app.user ?: Process.myUserHandle(), null, null)
            true
        }.getOrElse {
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_LAUNCHER)
                        .setComponent(app.componentName)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
                true
            }.getOrDefault(false)
        }
        if (started) {
            // Counted whether or not the usage sort is on, so switching it on later has a
            // history to order by instead of starting from nothing.
            scope.launch { prefs.library.incrementLaunchCount(app.key) }
        }
        return started
    }

    private fun launchShortcut(app: AppInfo): Boolean {
        val id = app.shortcutId ?: return false
        if (app.disabled) {
            // The publisher's own wording wherever there is any: it is the only thing that
            // can say why, and "sign in again to use this" beats a shrug.
            val message = shortcuts.findShortcut(app)?.disabledMessage?.toString()?.takeIf { it.isNotBlank() }
                ?: context.getString(R.string.shortcut_unavailable)
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            return false
        }
        // Through LauncherApps and nothing else. The Intent inside a ShortcutInfo belongs to
        // the app that published it and is never handed to a launcher; rebuilding one from
        // its parts would be starting something on that app's behalf that it did not ask for.
        val started = runCatching {
            launcherApps.startShortcut(
                app.packageName,
                id,
                null,
                null,
                app.user ?: Process.myUserHandle(),
            )
            true
        }.getOrDefault(false)
        if (started) {
            scope.launch { prefs.library.incrementLaunchCount(app.key) }
        }
        return started
    }

    fun openAppInfo(app: AppInfo) {
        // Only an app has one. The menus leave the item out for everything else; this is so a
        // caller that has not can do no harm.
        if (app.kind != EntryKind.APP) return
        val user = app.user
        if (user != null) {
            val shown = runCatching {
                launcherApps.startAppDetailsActivity(app.componentName, user, null, null)
                true
            }.getOrDefault(false)
            if (shown) return
        }
        openAppInfo(app.componentName.packageName)
    }

    fun openAppInfo(packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }
}
