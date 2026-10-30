// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.nikelauncher.R
import dev.nikelauncher.media.NotificationPreview
import dev.nikelauncher.media.PreviewReplyTarget

/** The text field of the notification's own reply action, with its suggested answers as chips. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun NotificationReply(
    preview: NotificationPreview,
    controller: NotificationController,
    report: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The target is the action and input themselves, not a position in a list that may change.
    val targets = preview.actions.filter { a -> a.inputs.any { it.allowFreeFormInput || !it.choices.isNullOrEmpty() } }
    if (targets.isEmpty()) return
    // An app can offer several places to answer, such as two accounts; the first is chosen until another is.
    var chosen by remember(preview.key) { mutableStateOf(0) }
    val action = targets[chosen.coerceIn(0, targets.lastIndex)]
    val input = action.inputs.firstOrNull { it.allowFreeFormInput } ?: action.inputs.first()
    val target = PreviewReplyTarget(action.intent, input.resultKey)
    var text by remember(preview.key) { mutableStateOf("") }
    val send: (String) -> Unit = { value ->
        if (value.isNotBlank()) {
            val ok = controller.reply(preview, target, value)
            if (ok) text = ""
            report(ok)
        }
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (targets.size > 1) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                targets.forEachIndexed { index, target ->
                    FilterChip(selected = index == chosen, onClick = { chosen = index }, label = { Text(target.title.ifBlank { stringResource(R.string.notification_reply) }) })
                }
            }
        }
        input.choices?.takeIf { it.isNotEmpty() }?.let { choices ->
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                choices.forEach { choice -> AssistChip(onClick = { send(choice.toString()) }, label = { Text(choice.toString()) }) }
            }
        }
        if (input.allowFreeFormInput) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(MAX_REPLY) },
                    modifier = Modifier.weight(1f),
                    label = { Text(input.label?.toString()?.takeIf { it.isNotBlank() } ?: stringResource(R.string.notification_reply)) },
                    maxLines = 4,
                )
                IconButton(onClick = { send(text) }, enabled = text.isNotBlank()) {
                    Icon(Icons.AutoMirrored.Outlined.Send, stringResource(R.string.notification_send))
                }
            }
        }
    }
}

private const val MAX_REPLY = 4096
