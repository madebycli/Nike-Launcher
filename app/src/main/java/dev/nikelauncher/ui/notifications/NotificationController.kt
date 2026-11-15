// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.notifications

import android.app.KeyguardManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.LauncherApps
import android.os.Process
import dev.nikelauncher.NikeApp
import dev.nikelauncher.data.AppInfo
import dev.nikelauncher.media.NotificationPreview
import dev.nikelauncher.media.NotificationPreviewBus
import dev.nikelauncher.media.PreviewReplyTarget

/**
 * Everything the notification sheet may do to a live notification.
 *
 * The sheet only draws and decides; the bus owns the PendingIntents and checks every request
 * against the notification as it is now, so a replaced notification cannot be retargeted.
 */
internal interface NotificationController {
    fun open(preview: NotificationPreview, intent: PendingIntent): Boolean
    /** Opens the app itself, never a notification's own destination. */
    fun launchApp(packageName: String): Boolean
    fun dismiss(preview: NotificationPreview): Boolean
    fun reply(preview: NotificationPreview, target: PreviewReplyTarget, text: String): Boolean
    fun run(preview: NotificationPreview, intent: PendingIntent): Boolean
}

internal class BusNotificationController(private val context: Context) : NotificationController {
    override fun launchApp(packageName: String): Boolean {
        if (context.getSystemService(KeyguardManager::class.java)?.isDeviceLocked != false) return false
        // The parser only accepts this profile's notifications, so never look across profiles.
        val user = Process.myUserHandle()
        val launchable = runCatching {
            context.getSystemService(LauncherApps::class.java)
                ?.getActivityList(packageName, user)?.firstOrNull()
                ?.let { AppInfo(it.componentName, it.label.toString(), user) }
        }.getOrNull() ?: return false
        return (context.applicationContext as NikeApp).appRepository.launch(launchable)
    }

    override fun open(preview: NotificationPreview, intent: PendingIntent) =
        NotificationPreviewBus.open(context, preview.key, intent)

    override fun dismiss(preview: NotificationPreview) = NotificationPreviewBus.dismiss(context, preview.key)

    override fun reply(preview: NotificationPreview, target: PreviewReplyTarget, text: String) =
        NotificationPreviewBus.sendReply(context, preview.key, target, text)

    override fun run(preview: NotificationPreview, intent: PendingIntent) =
        NotificationPreviewBus.runAction(context, preview.key, intent)
}
