// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.notifications

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import dev.nikelauncher.R
import dev.nikelauncher.ui.kit.orion.OrionMessageEntrance
import dev.nikelauncher.media.NotificationPreview
import kotlinx.coroutines.flow.drop

/** A conversation follows its newest message until the reader scrolls away. */
@Composable
internal fun NotificationHistory(preview: NotificationPreview, outgoing: List<OutgoingReply>, modifier: Modifier, onOpen: () -> Unit) {
    val scroll = rememberScrollState()
    var pinned by remember(preview.key) { mutableStateOf(preview.messages.isNotEmpty()) }
    var programmatic by remember(preview.key) { mutableStateOf(false) }
    LaunchedEffect(scroll) {
        snapshotFlow { scroll.isScrollInProgress }.drop(1).collect { moving ->
            if (!moving && !programmatic) pinned = scroll.value >= scroll.maxValue - 24
        }
    }
    // Sending something is always followed down to it, wherever the reader had scrolled.
    LaunchedEffect(outgoing.size) { if (outgoing.isNotEmpty()) pinned = true }
    LaunchedEffect(scroll.maxValue, preview.messages.size, outgoing.size) {
        if (pinned) {
            programmatic = true
            scroll.scrollTo(scroll.maxValue)
            programmatic = false
        }
    }
    Column(modifier.fillMaxWidth().heightIn(min = 120.dp).verticalScroll(scroll).padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        notificationBitmap(preview.picture, 1024)?.let {
            Image(it, null, Modifier.fillMaxWidth().aspectRatio(it.width.toFloat() / it.height).heightIn(max = 300.dp).testTag("notification-picture"), contentScale = ContentScale.Fit)
        }
        if (preview.messages.isNotEmpty()) NotificationMessages(preview.key, preview.messages, onOpen)
        else if (preview.text.isNotBlank()) Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Text(preview.text, Modifier.fillMaxWidth().padding(20.dp), style = MaterialTheme.typography.bodyMedium)
        }
        outgoing.forEachIndexed { index, reply ->
            key(reply.id) { OutgoingBubble(reply.text, last = index == outgoing.lastIndex) }
        }
    }
}

/** A reply the app has taken but not yet shown back: the reader's own bubble, springing up, with a quiet "sent" under the last. */
@Composable
private fun OutgoingBubble(text: String, last: Boolean) {
    OrionMessageEntrance(fromSelf = true, animate = true, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(start = 48.dp)) {
                Text(text, Modifier.padding(horizontal = 16.dp, vertical = 10.dp), style = MaterialTheme.typography.bodyMedium)
            }
            if (last) Text(stringResource(R.string.notification_sent), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
