// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
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
import dev.nikelauncher.ui.kit.motion.AppMotion
import dev.nikelauncher.ui.kit.orion.OrionEmptyState

/**
 * Every notification as one row of a segmented group: who and what, from which app, and when. A tap opens its page, a
 * swipe either way clears it. The header is the title and one action, nothing else; the sheet closes by its handle.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun NotificationOverview(
    notifications: List<NotificationPreview>,
    onSelect: (NotificationPreview) -> Unit,
    onDismiss: (NotificationPreview) -> Boolean,
    onClearAll: () -> Unit,
) {
    val context = LocalContext.current
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(start = 24.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.notifications_title), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (notifications.isNotEmpty()) {
                IconButton(onClick = onClearAll) {
                    Icon(Icons.Outlined.DeleteSweep, stringResource(R.string.notification_clear_all))
                }
            }
        }
        if (notifications.isEmpty()) {
            OrionEmptyState(Icons.Outlined.NotificationsNone, stringResource(R.string.notification_none))
            return@Column
        }
        val now = remember(notifications) { System.currentTimeMillis() }
        val effects = AppMotion.effects<Float>()
        val placement = AppMotion.offset()
        val count = notifications.size
        LazyColumn(
            Modifier.fillMaxWidth().heightIn(max = 600.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            itemsIndexed(notifications, key = { _, it -> it.key }) { index, preview ->
                val shapes = ListItemDefaults.segmentedShapes(index, count)
                val app = appLabel(context, preview.packageName)
                val title = preview.title.ifBlank { app }
                val line = preview.messages.lastOrNull()?.text?.ifBlank { null } ?: preview.text
                NotificationDismissBox(
                    preview, onDismiss, shapes.shape,
                    Modifier.animateItem(fadeInSpec = effects, placementSpec = placement, fadeOutSpec = effects),
                ) {
                    SegmentedListItem(
                        onClick = { onSelect(preview) },
                        shapes = shapes,
                        colors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        leadingContent = { SenderAvatar(title, preview.conversationIcon ?: preview.smallIcon, 40.dp) },
                        overlineContent = if (app != title) ({ Text(app, maxLines = 1, overflow = TextOverflow.Ellipsis) }) else null,
                        supportingContent = if (line.isNotBlank()) ({ Text(line, maxLines = 2, overflow = TextOverflow.Ellipsis) }) else null,
                        trailingContent = { Text(messageStamp(context, preview.time, now), style = MaterialTheme.typography.labelSmall) },
                        content = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
                }
            }
        }
    }
}
