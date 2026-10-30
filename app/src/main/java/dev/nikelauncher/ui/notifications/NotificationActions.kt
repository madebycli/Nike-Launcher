// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.notifications

import android.app.Notification
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.nikelauncher.R
import dev.nikelauncher.media.NotificationPreview

/** Provider metadata supplies symbols; actions without metadata retain their actual label. */
@Composable
internal fun NotificationActions(preview: NotificationPreview, controller: NotificationController, onOpen: () -> Unit, report: (Boolean) -> Unit) {
    FlowRow(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilledTonalButton(onClick = onOpen) { Text(stringResource(R.string.notification_open)) }
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
            val bitmap = if (symbol == null) notificationBitmap(action.icon) else null
            if (symbol != null || bitmap != null) {
                IconButton(onClick = { report(controller.run(preview, action.intent)) }, modifier = Modifier.semantics { contentDescription = action.title }) {
                    if (symbol != null) Icon(symbol, null) else Image(checkNotNull(bitmap), null, Modifier.size(24.dp))
                }
            } else TextButton(onClick = { report(controller.run(preview, action.intent)) }) { Text(action.title) }
        }
    }
}
