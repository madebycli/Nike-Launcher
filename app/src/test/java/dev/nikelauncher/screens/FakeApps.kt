// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.screens

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.os.Process
import org.robolectric.Shadows.shadowOf
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter

/** Names with a plausible spread of first letters, so a list and its A-Z strip have something to show. */
internal val SAMPLE_APPS = listOf(
    "Calendar", "Camera", "Clock", "Contacts", "Files", "Firefox", "Gallery", "Gmail", "Maps",
    "Messages", "Music", "Notes", "Phone", "Photos", "Settings", "Signal", "Spotify", "Weather",
)

/** Registers [labels] as launcher activities, as if they were installed. */
internal fun installFakeApps(context: Context, labels: List<String> = SAMPLE_APPS) {
    val launcherApps = context.getSystemService(LauncherApps::class.java)
    val user = Process.myUserHandle()
    labels.forEach { label ->
        val pkg = "fake." + label.lowercase()
        val app = ApplicationInfo().apply { packageName = pkg; flags = ApplicationInfo.FLAG_INSTALLED }
        val info = ActivityInfo().apply {
            packageName = pkg
            name = "$pkg.Main"
            applicationInfo = app
            nonLocalizedLabel = label
            exported = true
        }
        // Both classes are hidden, so they are built by name.
        val internal = ReflectionHelpers.callConstructor(
            Class.forName("android.content.pm.LauncherActivityInfoInternal"),
            ClassParameter.from(ActivityInfo::class.java, info),
            ClassParameter.from(Class.forName("android.content.pm.IncrementalStatesInfo"), null),
            ClassParameter.from(android.os.UserHandle::class.java, user),
        )
        val launcher = ReflectionHelpers.callConstructor(
            LauncherActivityInfo::class.java,
            ClassParameter.from(Context::class.java, context),
            ClassParameter.from(Class.forName("android.content.pm.LauncherActivityInfoInternal"), internal),
        )
        shadowOf(launcherApps).addActivity(user, launcher)
    }
}
