// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.notifications

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.nikelauncher.R
import dev.nikelauncher.media.HiddenConversationMessages
import dev.nikelauncher.media.PreviewMessage
import dev.nikelauncher.media.messageIds
import dev.nikelauncher.ui.kit.menu.Menu

/** How many of the newest messages show before the rest are folded away. */
private const val RECENT_MESSAGES = 6

/**
 * A conversation as bubbles: the owner's on the trailing side, everyone else's on the leading
 * side. A long press hides one from this view only; nothing is deleted from the app it came from.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun NotificationMessages(key: String, messages: List<PreviewMessage>) {
    val context = LocalContext.current
    val now = remember(messages) { System.currentTimeMillis() }
    val hiddenByKey by HiddenConversationMessages.values.collectAsState()
    val ids = remember(messages) { messageIds(messages) }
    val visible = messages.indices.filter { ids[it] !in hiddenByKey[key].orEmpty() }
    var showAll by remember(key) { mutableStateOf(false) }
    val shown = if (showAll) visible else visible.takeLast(RECENT_MESSAGES)
    val earlier = visible.size - shown.size
    var menuFor by remember { mutableStateOf<Int?>(null) }
    val hideTitle = stringResource(R.string.notification_hide_message)

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (earlier > 0) {
            TextButton(onClick = { showAll = true }) {
                Text(pluralStringResource(R.plurals.notification_earlier_messages, earlier, earlier))
            }
        }
        shown.forEach { index ->
            val message = messages[index]
            val own = message.fromSelf
            Row(Modifier.fillMaxWidth(), horizontalArrangement = if (own) Arrangement.End else Arrangement.Start, verticalAlignment = Alignment.Bottom) {
                if (!own) SenderAvatar(message.sender, message.senderIcon, 28.dp, Modifier.padding(end = 8.dp))
                Box {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = if (own) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.widthIn(max = 280.dp).combinedClickable(onClick = {}, onLongClick = { menuFor = index }),
                    ) {
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            if (!own && message.sender.isNotBlank()) {
                                Text(message.sender, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            }
                            attachmentBitmap(message.attachment)?.let {
                                Image(it, stringResource(R.string.notification_photo), Modifier.heightIn(max = 160.dp), contentScale = ContentScale.Fit)
                            }
                            if (message.text.isNotBlank()) Text(message.text, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                messageStamp(context, message.time, now),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Menu(expanded = menuFor == index, onDismissRequest = { menuFor = null }) {
                        item(hideTitle, { menuFor = null; HiddenConversationMessages.hide(key, ids[index]) }, leadingIcon = Icons.Outlined.VisibilityOff)
                    }
                }
            }
        }
    }
}
