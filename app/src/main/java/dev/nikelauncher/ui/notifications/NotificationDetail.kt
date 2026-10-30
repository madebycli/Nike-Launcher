// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.notifications

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.nikelauncher.R
import dev.nikelauncher.media.NotificationPreview
import kotlinx.coroutines.delay

/** One notification in full: its picture, the conversation, its own actions, and a way to answer. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun NotificationDetail(
    preview: NotificationPreview,
    controller: NotificationController,
    onBack: () -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    var failed by remember { mutableStateOf(false) }
    // The failure line is a moment's feedback, not a state to keep.
    LaunchedEffect(failed) { if (failed) { delay(4000); failed = false } }
    val report: (Boolean) -> Unit = { ok -> if (!ok) failed = true }
    val title = preview.title.ifBlank { appLabel(context, preview.packageName) }
    val picture = notificationBitmap(preview.picture)
    // Actions that are only a button; the ones that take text are the reply field below.
    val buttons = preview.actions.filter { it.inputs.isEmpty() && it.title.isNotBlank() }

    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.action_back)) }
            SenderAvatar(title, preview.conversationIcon ?: preview.smallIcon, 32.dp)
            Text(title, Modifier.weight(1f).padding(horizontal = 12.dp), style = MaterialTheme.typography.titleMedium, maxLines = 1)
            IconButton(onClick = { if (controller.dismiss(preview)) onBack() else failed = true }) {
                Icon(Icons.Outlined.Close, stringResource(R.string.notification_dismiss))
            }
        }
        Column(
            Modifier.weight(1f, fill = false).heightIn(min = 120.dp).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            picture?.let {
                Image(it, null, Modifier.fillMaxWidth().heightIn(max = 240.dp), contentScale = ContentScale.Fit)
            }
            if (preview.messages.isNotEmpty()) NotificationMessages(preview.key, preview.messages) else if (preview.text.isNotBlank()) {
                Text(preview.text, style = MaterialTheme.typography.bodyLarge)
            }
            if (failed) {
                Text(stringResource(R.string.notification_failed), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
        NotificationReply(preview, controller, report, Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        FlowRow(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            buttons.forEach { action ->
                OutlinedButton(onClick = { report(controller.run(preview, action.intent)) }) { Text(action.title) }
            }
            val destination = preview.contentIntent
            Button(onClick = { if (destination != null) { report(controller.open(preview, destination)); onClose() } else report(controller.launchApp(preview.packageName)) }) {
                Text(stringResource(R.string.notification_open))
            }
        }
    }
}
