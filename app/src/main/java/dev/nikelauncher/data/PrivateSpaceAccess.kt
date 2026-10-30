// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Build
import android.os.Process
import android.os.UserHandle
import dev.nikelauncher.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reading, opening and locking the private space, and the row that stands for it in the list. */
internal class PrivateSpaceAccess(
    private val context: Context,
    private val profileAccess: ProfileAccess,
) {
    private val launcherApps: LauncherApps get() = profileAccess.launcherApps
    private val userManager get() = profileAccess.userManager

    /**
     * The last serial a private space positively reported, kept only so that a read which
     * fails does not un-conceal what an earlier read concealed. In memory and never written
     * down: it is a fact about this device right now, and a file recording that a private
     * space exists is itself something for someone to find on the phone.
     */
    @Volatile
    private var lastKnownPrivateSerial: Long = 0L

    /**
     * The private space as this launcher can see it right now.
     *
     * Asked fresh every time rather than remembered, because the system locks the space on its
     * own — it re-locks when the screen goes off — and a remembered answer would go stale with
     * nothing here being called.
     *
     * Every call is runCatching-wrapped: they throw when the launcher is not the default home,
     * which is the condition Android puts on the permission, and a launcher that is not the
     * default home must behave exactly as it did before any of this existed. What a failure
     * never produces is [PrivateSpace.Absent], which conceals nothing: not being able to read
     * the space is not the same as there not being one, and only the second of those is safe
     * to act on.
     */
    fun privateSpace(): PrivateSpace {
        // getLauncherUserInfo arrived in API 35, which is also the first Android to have a
        // private space at all, so below it there is nothing to look for.
        if (Build.VERSION.SDK_INT < PRIVATE_SPACE_SDK) return PrivateSpace.Absent
        val mainUser = Process.myUserHandle()
        // LauncherApps.profiles rather than UserManager.userProfiles: without the permission
        // UserManager still lists the private profile while LauncherApps does not, and it is
        // LauncherApps that decides whether anything inside it can be read.
        val profiles = runCatching { launcherApps.profiles }.getOrNull()
            // The list itself refusing is not an answer of "there is no private space".
            ?: return uncertain(user = null)
        // A profile that would not say what it is could be the private one, so a pass that
        // found no private space but did meet one of those has not established anything.
        var unclassified = false
        for (user in profiles) {
            if (user == mainUser) continue
            val type = profileAccess.userType(user)
            if (type == null) {
                unclassified = true
                continue
            }
            if (type != USER_TYPE_PROFILE_PRIVATE) continue
            // Zero is the main profile's serial, so a profile reporting it means the lookup
            // failed. Concealing by a zero serial conceals nothing, which is the one outcome
            // worth refusing outright — so it becomes an uncertain space rather than an open
            // one, and falls back to the last serial this space was known by.
            val serial = runCatching { userManager.getSerialNumberForUser(user) }.getOrNull()
                ?.takeIf { it != 0L }
                ?: return uncertain(user)
            lastKnownPrivateSerial = serial
            return when (privateSpaceKind(type, profileAccess.quietMode(user))) {
                PrivateSpaceKind.UNLOCKED -> PrivateSpace.Unlocked(user, serial)
                // Locked, and also the unreachable case: the type was matched just above, and
                // locked is the reading to take if that ever stopped being true.
                else -> PrivateSpace.Locked(user, serial)
            }
        }
        // Nothing here said it was a private space. That is only an absence if every profile
        // did say what it was, and if no space has answered earlier in this session — a space
        // that has gone missing from a list it used to be in is a read that failed, not a
        // space that was deleted, and the difference is not one to guess in this direction.
        return if (unclassified || lastKnownPrivateSerial != 0L) uncertain(null) else PrivateSpace.Absent
    }

    /**
     * A space we know is there, or might be, and cannot describe: concealed by the last serial
     * it was known by, which is zero when it has never given one up.
     */
    private fun uncertain(user: UserHandle?): PrivateSpace =
        PrivateSpace.Uncertain(user, lastKnownPrivateSerial)

    /**
     * Locks an open space and asks for a locked one to be opened. What that takes is the
     * system's to decide: on a phone with a screen lock it puts its own authentication in
     * front of the unlock, and this answers null until that has been answered.
     *
     * Returns the state to hold from now on when a lock was granted, so whoever pressed the
     * row can conceal at that moment rather than waiting for the broadcast that follows. Null
     * when nothing was granted, and null for an unlock: nothing is exposed by an unlock being
     * a moment late, and there is nothing to list until the profile is actually up again.
     *
     * Suspending because this is several binder calls and, on a phone with a screen lock, the
     * system's own authentication — none of which belongs on the main thread, and all of which
     * starts from a press on a row.
     */
    /**
     * Opens the app store inside an open private space, the way the stock launcher's install
     * button does. Settings' own route to it can come up empty while another launcher is the
     * default (#124). False when there is no open space or no store to open.
     */
    fun openPrivateSpaceStore(from: Context): Boolean {
        if (Build.VERSION.SDK_INT < PRIVATE_SPACE_SDK) return false
        val user = (privateSpace() as? PrivateSpace.Unlocked)?.user ?: return false
        return runCatching {
            startSystemSender(from, launcherApps.getAppMarketActivityIntent(null, user) ?: return false)
        }.getOrDefault(false)
    }

    // Settings opened this way runs as the main user, which its "Install available apps" page
    // needs to list anything to copy over.
    fun openPrivateSpaceSettings(from: Context): Boolean {
        if (Build.VERSION.SDK_INT < PRIVATE_SPACE_SDK) return false
        return runCatching {
            // Public from Android 16, past the SDK this builds against.
            val sender = LauncherApps::class.java.getMethod("getPrivateSpaceSettingsIntent")
                .invoke(launcherApps) as? android.content.IntentSender ?: return false
            startSystemSender(from, sender)
        }.getOrDefault(false)
    }

    @androidx.annotation.RequiresApi(PRIVATE_SPACE_SDK)
    private fun startSystemSender(from: Context, sender: android.content.IntentSender): Boolean {
        // The system made this sender, so starting it rests on our being in front, which
        // Android 14 only lends to it when asked.
        val options = android.app.ActivityOptions.makeBasic()
            .setPendingIntentBackgroundActivityStartMode(
                android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
            )
        from.startIntentSender(
            sender, null,
            Intent.FLAG_ACTIVITY_NEW_TASK, Intent.FLAG_ACTIVITY_NEW_TASK, 0,
            options.toBundle(),
        )
        return true
    }

    suspend fun togglePrivateSpace(): PrivateSpace? = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < PRIVATE_SPACE_SDK) return@withContext null
        val state = privateSpace()
        // Nothing to ask about without a profile to ask about it. An uncertain space with one
        // is treated as locked, like everywhere else, so pressing the row tries to open it.
        val user = state.user ?: return@withContext null
        val lock = state is PrivateSpace.Unlocked
        val granted = runCatching { userManager.requestQuietModeEnabled(lock, user) }.getOrDefault(false)
        if (!granted || !lock) return@withContext null
        // The profile takes a moment to actually stop after the lock is granted. This state is
        // what the caller holds and hands to the enumeration that follows: asked again right
        // now, the system would describe an open space and list everything inside it.
        PrivateSpace.Locked(user, state.serial)
    }

    /**
     * The row that opens and closes the space, or nothing when there is no space to open.
     *
     * It is not an app and has no activity of its own, so it names itself after this package
     * and says which padlock it is in its class name — which is also what tells the icon cache
     * the two apart, since that cache is keyed by the row and not by the state of the device.
     */
    fun privateSpaceRow(state: PrivateSpace): List<AppInfo> {
        // Offered for any profile this pass could name, including one it could not describe:
        // that one is treated as locked, and a locked space has to keep its way back in. And
        // for a space that named itself earlier in this session and will not now, where there
        // is no profile left to point at — see PrivateSpace.offersPadlockRow.
        if (!state.offersPadlockRow) return emptyList()
        // Hidden, if that is what its owner told Android. Holding ACCESS_HIDDEN_PROFILES is
        // what lets a launcher see a private space at all — including one its owner has asked
        // to be kept out of sight while locked, which this showed regardless. Only while it is
        // locked: once open it is in use, and the row is also the way to lock it again.
        if (state is PrivateSpace.Locked && profileAccess.entrypointHidden(state.user)) return emptyList()
        // Unlocked is the only state with an open padlock, and it always names its profile,
        // so a row offered without one is always the closed padlock.
        val className =
            if (state is PrivateSpace.Unlocked) PRIVATE_SPACE_UNLOCKED_CLASS else PRIVATE_SPACE_LOCKED_CLASS
        return listOf(
            AppInfo(
                componentName = ComponentName(context.packageName, className),
                label = context.getString(R.string.private_space),
                kind = EntryKind.PRIVATE_SPACE,
            )
        )
    }
}
