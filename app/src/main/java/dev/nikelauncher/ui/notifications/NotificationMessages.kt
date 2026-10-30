// SPDX-License-Identifier: GPL-3.0-or-later
// Adapted from Victoria Grid (GPL-3.0-or-later), ui/notifications/NotificationConversation.kt.
package dev.nikelauncher.ui.notifications

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.nikelauncher.media.HiddenConversationMessages
import dev.nikelauncher.media.PreviewMessage
import dev.nikelauncher.media.messageIds

/** Consecutive messages share one author, avatar and timestamp, retaining provider order. */
@Composable
internal fun NotificationMessages(key: String, messages: List<PreviewMessage>, onOpen: () -> Unit) {
    val context = LocalContext.current
    val now = remember(messages) { System.currentTimeMillis() }
    val hidden by HiddenConversationMessages.values.collectAsState()
    val ids = remember(messages) { messageIds(messages) }
    val visible = messages.filterIndexed { index, _ -> ids[index] !in hidden[key].orEmpty() }
    val groups = remember(visible) { groupMessages(visible) }
    val named = visible.filter { !it.fromSelf }.map { it.sender }.distinct().size > 1
    val idsByMessage = remember(messages, ids) {
        java.util.IdentityHashMap<PreviewMessage, dev.nikelauncher.media.HiddenMessageId>().apply {
            messages.forEachIndexed { index, message -> put(message, ids[index]) }
        }
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val bubbleWidth = maxWidth * .8f - if (named) 36.dp else 0.dp
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            groups.forEach { group ->
                if (group.stampAbove && group.messages.first().time > 0) {
                    Text(messageStamp(context, group.messages.first().time, now), Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (group.fromSelf) Arrangement.End else Arrangement.Start,
                    verticalAlignment = Alignment.Bottom) {
                    if (named && !group.fromSelf) SenderAvatar(group.sender, group.icon, 28.dp, Modifier.padding(end = 8.dp))
                    Column(horizontalAlignment = if (group.fromSelf) Alignment.End else Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        if (named && !group.fromSelf && group.sender.isNotBlank()) {
                            Text(group.sender, style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        group.messages.forEach { message ->
                            val id = idsByMessage[message] ?: return@forEach
                            androidx.compose.runtime.key(id) {
                                MessageBubble(message, bubbleWidth, onOpen) { HiddenConversationMessages.hide(key, id) }
                            }
                        }
                        if (group.messages.last().time > 0) {
                            Text(messageStamp(context, group.messages.last().time, now),
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
