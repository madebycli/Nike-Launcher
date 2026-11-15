// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.notifications

import android.content.ComponentName
import android.os.Process
import android.os.UserHandle
import dev.nikelauncher.data.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AppNotificationTest {
    @Test fun `personal notification cannot leak into an identical package in another profile`() {
        val app = AppInfo(ComponentName("fake.messages", "fake.messages.Main"), "Messages")
        assertTrue(notificationEligible(app))
        assertTrue(notificationEligible(app.copy(user = Process.myUserHandle())))
        assertFalse(notificationEligible(app.copy(work = true)))
        assertFalse(notificationEligible(app.copy(privateProfile = true)))
        assertFalse(notificationEligible(app.copy(user = org.robolectric.util.ReflectionHelpers.callConstructor(UserHandle::class.java, org.robolectric.util.ReflectionHelpers.ClassParameter.from(Int::class.javaPrimitiveType, 10)))))
        assertFalse(notificationEligible(app.copy(kind = EntryKind.SHORTCUT)))
    }
    @Test fun `compact age handles future minutes hours and days with platform localization`() {
        val now = 200_000_000L
        assertEquals("0 min", notificationAge(Locale.US, now + 60_000, now))
        assertEquals("59 min", notificationAge(Locale.US, now - 59 * 60_000, now))
        assertEquals("1 hr", notificationAge(Locale.US, now - 60 * 60_000, now))
        assertEquals("1 day", notificationAge(Locale.US, now - 1440 * 60_000, now))
        assertNotEquals(notificationAge(Locale.US, 0, now), notificationAge(Locale.GERMAN, 0, now))
    }
}
