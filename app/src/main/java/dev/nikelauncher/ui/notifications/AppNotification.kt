// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.notifications

import android.os.Process
import androidx.compose.runtime.*
import dev.nikelauncher.data.AppInfo
import dev.nikelauncher.data.EntryKind
import dev.nikelauncher.media.NotificationPreview
import dev.nikelauncher.media.NotificationPreviewBus
import dev.nikelauncher.ui.common.LocalIconConfig

/** Notification snapshots belong to the personal profile; identical package names are not identities. */
internal fun notificationEligible(app: AppInfo): Boolean = app.kind == EntryKind.APP &&
    !app.work && !app.privateProfile && (app.user == null || app.user == Process.myUserHandle())

/** Same profile and opt-in gates on home, folder members and the app list. */
@Composable
internal fun appNotification(app: AppInfo, active: Boolean = true): NotificationPreview? {
    if (!active || !LocalIconConfig.current.notificationPreviews || !notificationEligible(app)) return null
    val posted by NotificationPreviewBus.notifications.collectAsState()
    return posted.firstOrNull { it.packageName == app.packageName }
}
