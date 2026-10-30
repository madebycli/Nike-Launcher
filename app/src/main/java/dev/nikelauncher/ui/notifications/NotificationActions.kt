// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.notifications

import android.app.Notification
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import dev.nikelauncher.R
import dev.nikelauncher.media.NotificationPreview
import dev.nikelauncher.ui.kit.menu.Menu

/** One open action and one overflow, independent of the provider's action count. */
@Composable
internal fun NotificationActions(preview: NotificationPreview, controller: NotificationController,
    onOpen: () -> Unit, onDismiss: () -> Unit, report: (Boolean) -> Unit) {
    var expanded by remember(preview.key) { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) { Icon(Icons.Outlined.MoreVert, stringResource(R.string.settings_more)) }
        Menu(expanded, onDismissRequest = { expanded = false }) {
            item(stringResource(R.string.notification_open), { expanded = false; onOpen() }, leadingIcon = Icons.Outlined.OpenInNew)
            preview.actions.filter { it.inputs.isEmpty() && it.title.isNotBlank() }.forEach { action ->
                val symbol = when (action.semanticAction) {
                    Notification.Action.SEMANTIC_ACTION_MARK_AS_READ -> Icons.Outlined.DoneAll
                    Notification.Action.SEMANTIC_ACTION_MARK_AS_UNREAD -> Icons.Outlined.MarkEmailUnread
                    Notification.Action.SEMANTIC_ACTION_ARCHIVE -> Icons.Outlined.Archive
                    Notification.Action.SEMANTIC_ACTION_DELETE -> Icons.Outlined.DeleteOutline
                    Notification.Action.SEMANTIC_ACTION_MUTE -> Icons.Outlined.VolumeOff
                    Notification.Action.SEMANTIC_ACTION_UNMUTE -> Icons.Outlined.VolumeUp
                    else -> null
                }
                item(action.title, { expanded = false; report(controller.run(preview, action.intent)) },
                    leadingIcon = symbol, leadingImage = if (symbol == null) notificationBitmap(action.icon) else null)
            }
            gap()
            item(stringResource(R.string.notification_dismiss), { expanded = false; onDismiss() },
                leadingIcon = Icons.Outlined.DeleteOutline, destructive = true)
        }
    }
}
