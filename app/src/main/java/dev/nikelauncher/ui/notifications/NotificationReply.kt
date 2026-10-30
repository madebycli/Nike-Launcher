// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.notifications

import android.app.RemoteInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.input.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.nikelauncher.R
import dev.nikelauncher.media.NotificationPreview
import dev.nikelauncher.media.PreviewReplyTarget

private data class ReplyOption(val target: PreviewReplyTarget, val input: RemoteInput, val title: String)

/** A draft stays attached to its action and result key when the provider replaces the notification. */
@Composable
internal fun NotificationReply(preview: NotificationPreview, controller: NotificationController, report: (Boolean) -> Unit, modifier: Modifier = Modifier, leadingActions: @Composable () -> Unit = {}) {
    val options = remember(preview.actions) {
        preview.actions.flatMap { action -> action.inputs.filter { it.allowFreeFormInput || !it.choices.isNullOrEmpty() }
            .map { ReplyOption(PreviewReplyTarget(action.intent, it.resultKey), it, action.title) } }
            .sortedByDescending { it.input.allowFreeFormInput }
    }
    val field = remember(preview.key) { TextFieldState() }
    var chosen by remember(preview.key) { mutableStateOf(options.firstOrNull()?.target) }
    LaunchedEffect(options) { if (field.text.isBlank()) chosen = options.firstOrNull()?.target }
    val option = options.firstOrNull { it.target == chosen }
    // Keep a draft visible even if its target disappears; only an explicit choice may retarget it.
    if (options.isEmpty() && field.text.isBlank()) {
        Row(modifier.fillMaxWidth()) { leadingActions() }
        return
    }
    val input = option?.input
    val canSend = input != null && field.text.isNotBlank() && field.text.length <= MAX_REPLY &&
        (input.allowFreeFormInput || input.choices.orEmpty().any { it.toString() == field.text.toString() })
    val send: (String) -> Unit = { value ->
        val target = option?.target
        if (target != null && value.isNotBlank()) {
            val ok = controller.reply(preview, target, value)
            if (ok) field.edit { replace(0, length, "") }
            report(ok)
        }
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (options.size > 1 || option == null) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { choice ->
                    FilterChip(selected = choice.target == chosen, onClick = { chosen = choice.target },
                        label = { Text(choice.title.ifBlank { stringResource(R.string.notification_reply) }) })
                }
            }
        }
        input?.choices?.takeIf { it.isNotEmpty() }?.let { choices ->
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                choices.forEach { choice -> AssistChip(onClick = { send(choice.toString()) }, label = { Text(choice.toString()) }) }
            }
        }
        if (input?.allowFreeFormInput == false) Row { leadingActions() }
        if (input?.allowFreeFormInput != false) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                leadingActions()
                TextField(state = field, inputTransformation = InputTransformation.maxLength(MAX_REPLY),
                    modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.extraLarge,
                    placeholder = { Text(input?.label?.toString()?.takeIf { it.isNotBlank() } ?: stringResource(R.string.notification_reply)) },
                    lineLimits = TextFieldLineLimits.MultiLine(maxHeightInLines = 4))
                FilledIconButton(onClick = { send(field.text.toString()) }, enabled = canSend) {
                    Icon(Icons.AutoMirrored.Rounded.Send, stringResource(R.string.notification_send))
                }
            }
        }
    }
}

private const val MAX_REPLY = 4096
