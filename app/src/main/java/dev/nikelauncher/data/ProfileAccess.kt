// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.data

import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Build
import android.os.UserHandle
import android.os.UserManager

/**
 * The platform's account of the profiles on this device: what each one is, whether it is
 * switched off and whether anything from it may be listed. Shared by the app list, the private
 * space and the pinned shortcuts, which must all reach the same verdict about a profile.
 */
internal class ProfileAccess(private val context: Context) {

    val pm: PackageManager get() = context.packageManager
    val launcherApps: LauncherApps
        get() = context.getSystemService(LauncherApps::class.java)
    val userManager: UserManager
        get() = context.getSystemService(UserManager::class.java)

    /**
     * The serial to key a profile's rows by, or null when nothing from it may be listed. Asked
     * once per profile and used for apps and pinned shortcuts alike: a shortcut pinned inside
     * the private space belongs to it exactly as its apps do.
     */
    fun listableSerial(user: UserHandle, mainUser: UserHandle): Long? {
        val isMain = user == mainUser
        val serial = runCatching { userManager.getSerialNumberForUser(user) }.getOrNull()
        // Only asked about a profile the answers could change anything for: the one we
        // run in is always listed, and below Android 15 there is no private space for
        // either answer to describe.
        val classify = !isMain && Build.VERSION.SDK_INT >= PRIVATE_SPACE_SDK
        val listed = shouldListProfile(
            isMainUser = isMain,
            sdk = Build.VERSION.SDK_INT,
            userType = if (classify) userType(user) else null,
            quietMode = if (classify) quietMode(user) else null,
            serial = serial,
        )
        if (!listed) return null
        // Null only reaches here for the profile we run in, whose serial is zero anyway; every
        // other profile without one was refused above.
        return serial ?: 0L
    }

    /**
     * What kind of profile this is, or null when the platform will not say — which everything
     * here reads as "it might be the private one" and treats accordingly.
     */
    fun userType(user: UserHandle): String? =
        if (Build.VERSION.SDK_INT < PRIVATE_SPACE_SDK) null
        else runCatching { launcherApps.getLauncherUserInfo(user)?.userType }.getOrNull()

    /** First-install time per package, read in one pass. */
    fun installTimes(): Map<String, Long> = runCatching {
        pm.getInstalledPackages(0).associate { it.packageName to it.firstInstallTime }
    }.getOrDefault(emptyMap())

    /**
     * Whether the owner has asked for the private space's entry to be hidden while locked.
     *
     * Android 16 says so through LauncherUserInfo's user config, under a key it documents as a
     * plain string. Reached by reflection because the method arrived in API 36 and this builds
     * against 35; below 36 there is no such setting to honour, and anything that fails reads as
     * not hidden, which is how this has always behaved.
     */
    fun entrypointHidden(user: UserHandle): Boolean {
        if (Build.VERSION.SDK_INT < 36) return false
        return runCatching {
            val info = launcherApps.getLauncherUserInfo(user) ?: return false
            val config = info.javaClass.getMethod("getUserConfig").invoke(info) as? android.os.Bundle
            config?.getBoolean(PRIVATE_SPACE_ENTRYPOINT_HIDDEN, false) == true
        }.getOrDefault(false)
    }

    /**
     * Whether a profile is a work one.
     *
     * Exact from API 35, where the platform will name a profile's type. Below that it will not,
     * and any profile other than the main one is a work profile in practice — the private space
     * arrived with that same API, so there was nothing else a second profile could be.
     */
    fun isWorkProfile(user: UserHandle, mainUser: UserHandle): Boolean {
        if (user == mainUser) return false
        val type = userType(user) ?: return true
        return type == UserManager.USER_TYPE_PROFILE_MANAGED
    }

    /** Whether the profile is switched off, or null when the platform will not say. */
    fun quietMode(user: UserHandle): Boolean? =
        runCatching { userManager.isQuietModeEnabled(user) }.getOrNull()
}

/** LauncherUserInfo.PRIVATE_SPACE_ENTRYPOINT_HIDDEN, by its documented value. */
internal const val PRIVATE_SPACE_ENTRYPOINT_HIDDEN = "private_space_entrypoint_hidden"
