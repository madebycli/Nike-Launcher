// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.drawable.Drawable
import android.os.Process
import android.os.UserHandle
import android.widget.Toast
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

/** A few tries spread over about a second, since a pin request's shortcut can take a moment
 *  to show up in the system's own pinned set after [LauncherApps.PinItemRequest.accept]
 *  returns, not before. */
private const val PIN_CONFIRM_ATTEMPTS = 5
private const val PIN_CONFIRM_DELAY_MS = 200L

/** Pinned shortcuts as rows, the shortcuts an app publishes, and pinning and unpinning them. */
internal class ShortcutAccess(
    private val context: Context,
    private val prefs: Prefs,
    private val scope: CoroutineScope,
    private val profileAccess: ProfileAccess,
) {
    private val launcherApps: LauncherApps get() = profileAccess.launcherApps
    private val userManager get() = profileAccess.userManager

    /**
     * Bumped when the pinned set changes without any package changing — the confirm screen
     * accepting one, or a menu unpinning one. Nothing else would tell the list to be built
     * again, because no app was installed, removed or updated.
     */
    private val _shortcutChanges = MutableStateFlow(0)
    val shortcutChanges: StateFlow<Int> = _shortcutChanges.asStateFlow()

    private fun noteShortcutsChanged() {
        _shortcutChanges.update { it + 1 }
    }

    private data class PinnedShortcut(val info: ShortcutInfo, val user: UserHandle, val serial: Long)

    /**
     * Only the device's current home app may ask about shortcuts at all; anyone else is
     * answered with a SecurityException rather than an empty list. So every call below asks
     * this first, and a launcher that is not the one in use simply lists no shortcuts.
     */
    private fun hasShortcutHostPermission(): Boolean =
        runCatching { launcherApps.hasShortcutHostPermission() }.getOrDefault(false)

    private fun pinnedShortcuts(): List<PinnedShortcut> {
        if (!hasShortcutHostPermission()) return emptyList()
        val profiles = runCatching { userManager.userProfiles }.getOrNull().orEmpty()
        val mainUser = Process.myUserHandle()
        return profiles.flatMap { user ->
            // The same decision the app list makes, for the same reason: a shortcut read from a
            // profile whose serial could not be read would be keyed as the main profile's, and
            // nothing that conceals by serial could reach it afterwards.
            val serial = profileAccess.listableSerial(user, mainUser) ?: return@flatMap emptyList()
            val query = LauncherApps.ShortcutQuery()
                .setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
            runCatching { launcherApps.getShortcuts(query, user) }
                .getOrNull()
                .orEmpty()
                .map { PinnedShortcut(it, user, serial) }
        }
    }

    /** Every pinned shortcut, as an ordinary row. */
    fun queryPinnedShortcuts(): List<AppInfo> = pinnedShortcuts()
        // An id that would corrupt the newline-joined favorites list (CR-4) is not something
        // this store can hold at all, so such a shortcut never becomes a row in the first
        // place — PinShortcutActivity applies the same check before it can ever be pinned.
        .filter { EntryKeys.isStorableShortcutId(it.info.id) }
        .map { pinned ->
            val info = pinned.info
            AppInfo(
                // Always the publisher's own package, never `info.activity`'s — the activity
                // names which of the publisher's screens the shortcut opens, and is not
                // guaranteed to agree with `info.package` in shape even though it always does
                // in practice. Everything downstream (AppInfo.key, AppInfo.packageName, the
                // in-list package search, the icon-pack lookup skip, unpin's own
                // `pinShortcuts(pkg, …)` call) has to agree on one package, and the publisher's
                // is the only one guaranteed to be that shortcut's.
                componentName = ComponentName(info.`package`, info.activity?.className ?: ""),
                label = info.shortLabel?.toString() ?: info.longLabel?.toString() ?: info.`package`,
                user = pinned.user,
                userSerial = pinned.serial,
                kind = EntryKind.SHORTCUT,
                shortcutId = info.id,
                disabled = !info.isEnabled,
            )
        }

    /**
     * The shortcuts an app publishes about itself — what a long press on it offers.
     *
     * Both the ones declared in its manifest and the ones it adds as it runs, ordered the way
     * the app ranked them. Asked of the activity first, since an app with more than one
     * launcher icon ranks them per icon; a package that answers nothing that way is asked as a
     * whole rather than left looking as though it publishes none.
     */
    fun appShortcuts(app: AppInfo): List<ShortcutInfo> {
        if (app.kind != EntryKind.APP) return emptyList()
        val user = app.user ?: Process.myUserHandle()
        val flags = LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
            LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC

        fun query(withActivity: Boolean): List<ShortcutInfo> {
            val q = LauncherApps.ShortcutQuery()
                .setPackage(app.componentName.packageName)
                .setQueryFlags(flags)
            if (withActivity) q.setActivity(app.componentName)
            // Throws rather than returning nothing when this launcher does not hold the home
            // role, which is a state it can be in for a moment after being switched to.
            return runCatching { launcherApps.getShortcuts(q, user) }.getOrNull().orEmpty()
        }

        val found = query(withActivity = true).ifEmpty { query(withActivity = false) }
        return found.filter { it.isEnabled }.sortedBy { it.rank }
    }

    /** Starts one of [appShortcuts]; the publisher's own Intent is never touched. */
    fun startAppShortcut(shortcut: ShortcutInfo): Boolean = runCatching {
        launcherApps.startShortcut(shortcut, null, null)
        true
    }.getOrDefault(false)

    /** The icon for a shortcut offered in a menu. */
    fun shortcutIcon(shortcut: ShortcutInfo, densityDpi: Int): Drawable? =
        runCatching { launcherApps.getShortcutIconDrawable(shortcut, densityDpi) }.getOrNull()

    /** The live ShortcutInfo behind a row, for its icon, its reason for being off, or a pin. */
    fun findShortcut(app: AppInfo): ShortcutInfo? {
        val id = app.shortcutId ?: return null
        if (!hasShortcutHostPermission()) return null
        val query = LauncherApps.ShortcutQuery()
            .setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
            .setPackage(app.packageName)
            .setShortcutIds(listOf(id))
        return runCatching { launcherApps.getShortcuts(query, app.user ?: Process.myUserHandle()) }
            .getOrNull()
            ?.firstOrNull()
    }

    /**
     * Hands the package back every id it still has pinned, minus this one: pinShortcuts does
     * not remove one shortcut, it replaces the whole pinned set for a package in a profile,
     * so anything left out of that list is unpinned along with it.
     *
     * Then everything stored under the key goes too, but only once that call has actually
     * gone through: if it threw — the host permission was lost, the profile went away mid-
     * call — the shortcut is still pinned with the system, and forgetting its name, icon,
     * folder, hidden flag, launch count and quick-launch slot now would strand them with
     * nothing left to reconnect them to. An app can be reinstalled and find those waiting;
     * an unpinned shortcut never comes back, so what is left behind after a real unpin is
     * only clutter nothing can reach.
     */
    fun unpin(app: AppInfo) {
        if (app.kind != EntryKind.SHORTCUT) return
        val id = app.shortcutId ?: return
        val removed = EntryKeys.ShortcutRef(app.packageName, id, app.userSerial)
        val pinned = pinnedShortcuts().map {
            EntryKeys.ShortcutRef(it.info.`package`, it.info.id, it.serial)
        }
        // pinShortcuts replaces the whole pinned set for a package in a profile. If the list
        // just read does not even contain the shortcut being removed, that profile could not
        // be read this time, and what would be handed back is an empty list — unpinning every
        // other shortcut the package has there. Better to do nothing and say so.
        if (removed !in pinned) {
            Toast.makeText(context, R.string.shortcut_unpin_failed, Toast.LENGTH_SHORT).show()
            return
        }
        val unpinned = runCatching {
            launcherApps.pinShortcuts(
                app.packageName,
                PinnedShortcuts.remainingIds(pinned, removed),
                app.user ?: Process.myUserHandle(),
            )
        }.isSuccess
        if (!unpinned) {
            Toast.makeText(context, R.string.shortcut_unpin_failed, Toast.LENGTH_SHORT).show()
            return
        }
        scope.launch { prefs.library.forgetEntry(app.key) }
        noteShortcutsChanged()
    }

    /**
     * Called once [LauncherApps.PinItemRequest.accept] has returned true for a shortcut from
     * [pkg] with id [id] in profile [user] (serial [serial]): confirms the system actually
     * pinned it, then joins it to the favorites, because "add to the home screen" is what the
     * user was asked.
     *
     * Deliberately on this repository's own scope rather than the confirm screen's. That
     * screen finishes the instant it calls this, and accept() taking effect can lag its own
     * return by a moment (see [waitUntilPinned]) — if the write instead rode the screen's own
     * scope, Cancel, a tap outside, rotation, or the screen merely stopping in that window
     * would cancel it, leaving a shortcut the system has pinned but that never shows up here.
     *
     * Verifying rather than trusting accept()'s own answer is also what a forged pin request
     * cannot get past: its binder can answer accept() however it likes, but it cannot make
     * [waitUntilPinned] find a shortcut pinned that genuinely is not.
     */
    fun confirmPinnedShortcut(pkg: String, id: String, user: UserHandle, serial: Long) {
        scope.launch {
            if (waitUntilPinned(pkg, id, user)) {
                prefs.library.addFavorite(EntryKeys.shortcut(pkg, id, serial))
                noteShortcutsChanged()
            } else {
                // This whole coroutine runs on the repository's Default-dispatched scope, so
                // a Toast here needs Main asked for explicitly rather than however Toast.show
                // elsewhere in this class gets it for free by already being called from the UI.
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, R.string.shortcut_pin_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Polls rather than trusting one look, since the shortcut a request just accepted can take
     * a moment to reach the system's own pinned set.
     */
    private suspend fun waitUntilPinned(pkg: String, id: String, user: UserHandle): Boolean {
        val query = LauncherApps.ShortcutQuery()
            .setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
            .setPackage(pkg)
            .setShortcutIds(listOf(id))
        repeat(PIN_CONFIRM_ATTEMPTS) { attempt ->
            val pinned = runCatching { launcherApps.getShortcuts(query, user) }
                .getOrNull()
                .orEmpty()
                .any { it.id == id }
            if (pinned) return true
            if (attempt < PIN_CONFIRM_ATTEMPTS - 1) delay(PIN_CONFIRM_DELAY_MS)
        }
        return false
    }

    /** The icon of a pinned shortcut row, badged with its publisher when the system can. */
    fun iconFor(app: AppInfo): Drawable? {
        val info = findShortcut(app) ?: return null
        val density = context.resources.configuration.densityDpi
        return runCatching { launcherApps.getShortcutBadgedIconDrawable(info, density) }.getOrNull()
            ?: runCatching { launcherApps.getShortcutIconDrawable(info, density) }.getOrNull()
    }
}
