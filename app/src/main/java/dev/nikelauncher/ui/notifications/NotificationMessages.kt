// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.notifications

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.nikelauncher.media.PreviewMessage

/** A conversation as bubbles: the owner's on the trailing side, everyone else's on the leading side. */
@Composable
internal fun NotificationMessages(messages: List<PreviewMessage>) {
    val context = LocalContext.current
    val now = remember(messages) { System.currentTimeMillis() }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        messages.forEach { message ->
            val own = message.fromSelf
            Row(Modifier.fillMaxWidth(), horizontalArrangement = if (own) Arrangement.End else Arrangement.Start, verticalAlignment = Alignment.Bottom) {
                if (!own) SenderAvatar(message.sender, message.senderIcon, 28.dp, Modifier.padding(end = 8.dp))
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = if (own) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.widthIn(max = 280.dp),
                ) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        if (!own && message.sender.isNotBlank()) {
                            Text(message.sender, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        attachmentBitmap(message.attachment)?.let {
                            Image(it, null, Modifier.heightIn(max = 160.dp), contentScale = ContentScale.Fit)
                        }
                        if (message.text.isNotBlank()) Text(message.text, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            messageStamp(context, message.time, now),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
