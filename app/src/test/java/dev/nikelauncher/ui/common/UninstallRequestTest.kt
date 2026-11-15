// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.common

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Process
import dev.nikelauncher.data.AppInfo
import dev.nikelauncher.data.EntryKind
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class UninstallRequestTest {
    @Test fun declaresSystemUninstallPermissionAndRequestsConfirmationForSelectedProfile() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val declared = activity.packageManager.getPackageInfo(activity.packageName, PackageManager.GET_PERMISSIONS)
        assertTrue(declared.requestedPermissions.orEmpty().contains(Manifest.permission.REQUEST_DELETE_PACKAGES))
        val user = Process.myUserHandle()
        val app = AppInfo(ComponentName("example.app", "example.app.Main"), "Example", user = user)
        assertTrue(activity.requestUninstall(app))
        val intent = shadowOf(activity).nextStartedActivity
        assertEquals(Intent.ACTION_DELETE, intent.action)
        assertEquals("package:example.app", intent.data.toString())
        assertEquals(user, intent.getParcelableExtra(Intent.EXTRA_USER, android.os.UserHandle::class.java))
        assertFalse(intent.getBooleanExtra(Intent.EXTRA_RETURN_RESULT, false))
    }

    @Test fun shortcutDoesNotRequestAnUninstall() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val app = AppInfo(ComponentName("example.app", "example.app.Main"), "Example", kind = EntryKind.SHORTCUT, shortcutId = "demo")
        assertFalse(activity.requestUninstall(app))
        assertNull(shadowOf(activity).nextStartedActivity)
    }
}
