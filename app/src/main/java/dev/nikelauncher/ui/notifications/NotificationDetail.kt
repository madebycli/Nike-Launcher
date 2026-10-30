// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.notifications

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.nikelauncher.R
import dev.nikelauncher.media.NotificationPreview
import kotlinx.coroutines.delay

/** The header and fixed composer surround a scrollable notification or conversation. */
@Composable
internal fun NotificationDetail(preview: NotificationPreview, controller: NotificationController, onBack: () -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(failed) { if (failed) { delay(4000); failed = false } }
    val report: (Boolean) -> Unit = { ok -> if (!ok) failed = true }
    val title = preview.title.ifBlank { appLabel(context, preview.packageName) }
    val open = {
        val destination = preview.contentIntent
        val opened = if (destination != null) controller.open(preview, destination) else controller.launchApp(preview.packageName)
        if (opened) onClose() else failed = true
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.action_back)) }
            SenderAvatar(title, preview.conversationIcon ?: preview.smallIcon, 32.dp)
            Text(title, Modifier.weight(1f).padding(horizontal = 12.dp), style = MaterialTheme.typography.titleMedium,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, stringResource(R.string.action_close)) }
        }
        NotificationHistory(preview, Modifier.weight(1f), open)
        if (failed) Text(stringResource(R.string.notification_failed), Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        NotificationReply(preview, controller, report, Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
            NotificationActions(preview, controller, open,
                onDismiss = { if (controller.dismiss(preview)) onBack() else failed = true }, report = report)
        }
    }
}
