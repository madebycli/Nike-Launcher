// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.notifications

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.nikelauncher.R
import dev.nikelauncher.media.NotificationPreview

/** Every notification as a row: who, what, when. A tap opens its page, the cross clears it. */
@Composable
internal fun NotificationOverview(
    notifications: List<NotificationPreview>,
    onSelect: (NotificationPreview) -> Unit,
    onDismiss: (NotificationPreview) -> Boolean,
    onClearAll: () -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.notifications_title), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
            if (notifications.isNotEmpty()) {
                TextButton(onClick = onClearAll) { Text(stringResource(R.string.notification_clear_all)) }
            }
            IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, stringResource(R.string.action_close)) }
        }
        if (notifications.isEmpty()) {
            Text(
                stringResource(R.string.notification_none),
                Modifier.padding(24.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }
        val now = remember(notifications) { System.currentTimeMillis() }
        LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
            items(notifications, key = { it.key }) { preview ->
                NotificationDismissBox(preview, onDismiss) {
                Row(
                    Modifier.fillMaxWidth().clickable { onSelect(preview) }.padding(start = 24.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SenderAvatar(preview.title.ifBlank { appLabel(context, preview.packageName) }, preview.conversationIcon ?: preview.smallIcon, 40.dp)
                    Column(Modifier.weight(1f)) {
                        Text(
                            preview.title.ifBlank { appLabel(context, preview.packageName) },
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val line = preview.messages.lastOrNull()?.text?.ifBlank { null } ?: preview.text
                        if (line.isNotBlank()) {
                            Text(
                                line,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Text(
                            appLabel(context, preview.packageName) + " · " + messageStamp(context, preview.time, now),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = { onDismiss(preview) }) {
                        Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.notification_dismiss), modifier = Modifier.size(20.dp))
                    }
                }
                }
            }
        }
    }
}
