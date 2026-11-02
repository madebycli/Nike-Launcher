// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.media

import android.app.KeyguardManager
import android.app.Notification
import android.content.Context
import android.os.Process
import android.service.notification.StatusBarNotification
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NotificationPreviewReconciliationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    @Before fun setup() {
        NotificationPreviewBus.clear()
        shadowOf(context.getSystemService(KeyguardManager::class.java)).setIsDeviceLocked(false)
    }
    @After fun cleanup() = NotificationPreviewBus.clear()

    private fun notification(title: String? = null, body: String? = null, secret: Boolean = false): StatusBarNotification {
        val item = Notification.Builder(context, "test").setSmallIcon(android.R.drawable.ic_dialog_info)
            .setVisibility(if (secret) Notification.VISIBILITY_SECRET else Notification.VISIBILITY_PRIVATE)
            .apply { if (title != null) setContentTitle(title); if (body != null) setContentText(body) }.build()
        return StatusBarNotification("example.chat", "example.chat", 1, null, Process.myUid(), 0, 0,
            item, Process.myUserHandle(), 123456L)
    }

    @Test fun emptyCallbackCanBeCompletedByFullSnapshot() {
        val empty = notification()
        NotificationPreviewBus.posted(context, empty)
        val full = notification("Alex", "Hello")
        val parsed = NotificationPreviewBus.parseSnapshot(context, arrayOf(full))
        assertTrue(NotificationPreviewBus.updateSnapshotIfUnchanged(context, NotificationPreviewBus.revision, parsed))
        assertEquals("Hello", NotificationPreviewBus.notifications.value.single().text)
    }

    @Test fun titleOnlyCallbackCanBeCompletedButExistingLiveTextWins() {
        NotificationPreviewBus.posted(context, notification("Alex"))
        val parsed = NotificationPreviewBus.parseSnapshot(context, arrayOf(notification("Alex", "Hello")))
        assertTrue(NotificationPreviewBus.updateSnapshotIfUnchanged(context, NotificationPreviewBus.revision, parsed))
        assertEquals("Hello", NotificationPreviewBus.notifications.value.single().text)
        NotificationPreviewBus.posted(context, notification("Alex", "Newer"))
        assertTrue(NotificationPreviewBus.updateSnapshotIfUnchanged(context, NotificationPreviewBus.revision, parsed))
        assertEquals("Newer", NotificationPreviewBus.notifications.value.single().text)
    }

    @Test fun explicitRemovalAndSecretCallbackBlockAnOlderSnapshot() {
        val full = notification("Alex", "Hello")
        val parsed = NotificationPreviewBus.parseSnapshot(context, arrayOf(full))
        NotificationPreviewBus.posted(context, full)
        NotificationPreviewBus.removed(full.key)
        NotificationPreviewBus.updateSnapshotIfUnchanged(context, NotificationPreviewBus.revision, parsed)
        assertTrue(NotificationPreviewBus.notifications.value.isEmpty())
        NotificationPreviewBus.posted(context, notification("Alex", "secret", secret = true))
        NotificationPreviewBus.updateSnapshotIfUnchanged(context, NotificationPreviewBus.revision, parsed)
        assertTrue(NotificationPreviewBus.notifications.value.isEmpty())
    }
}
