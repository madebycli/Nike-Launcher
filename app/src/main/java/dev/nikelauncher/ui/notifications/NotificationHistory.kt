// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.notifications

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import dev.nikelauncher.media.NotificationPreview
import kotlinx.coroutines.flow.drop

/** A conversation follows its newest message until the reader scrolls away. */
@Composable
internal fun NotificationHistory(preview: NotificationPreview, modifier: Modifier, onOpen: () -> Unit) {
    val scroll = rememberScrollState()
    var pinned by remember(preview.key) { mutableStateOf(preview.messages.isNotEmpty()) }
    var programmatic by remember(preview.key) { mutableStateOf(false) }
    LaunchedEffect(scroll) {
        snapshotFlow { scroll.isScrollInProgress }.drop(1).collect { moving ->
            if (!moving && !programmatic) pinned = scroll.value >= scroll.maxValue - 24
        }
    }
    LaunchedEffect(scroll.maxValue, preview.messages.size) {
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
        else if (preview.text.isNotBlank()) Text(preview.text, style = MaterialTheme.typography.bodyLarge)
    }
}
