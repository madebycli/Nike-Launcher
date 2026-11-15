// SPDX-License-Identifier: GPL-3.0-or-later
/* Composer layout adapted from Orion Store, RookieEnough and contributors, GPL-3.0, e561e91, ui/AiChat.kt (AiInput). */
package dev.nikelauncher.ui.notifications

import android.app.RemoteInput
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.nikelauncher.ui.kit.orion.orionReveal
import dev.nikelauncher.R
import dev.nikelauncher.media.NotificationPreview
import dev.nikelauncher.media.PreviewReplyTarget
import dev.nikelauncher.ui.kit.motion.AppMotion

private data class ReplyOption(val target: PreviewReplyTarget, val input: RemoteInput, val title: String)

/**
 * The reply composer: one pill holding the text and the notification's other actions, and a send button that only
 * exists while there is something to send. A sent reply is handed to [onSent] (to be shown at once); a reply the app
 * refused stays in the field. A draft stays attached to its action and result key when the app replaces the notification.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
internal fun NotificationReply(
    preview: NotificationPreview,
    controller: NotificationController,
    report: (Boolean) -> Unit,
    onSent: (String) -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
) {
    val options = remember(preview.actions) {
        preview.actions.flatMap { action -> action.inputs.filter { it.allowFreeFormInput || !it.choices.isNullOrEmpty() }
            .map { ReplyOption(PreviewReplyTarget(action.intent, it.resultKey), it, action.title) } }
            .sortedByDescending { it.input.allowFreeFormInput }
    }
    val field = remember(preview.key) { TextFieldState() }
    var chosen by remember(preview.key) { mutableStateOf(options.firstOrNull()?.target) }
    LaunchedEffect(options) { if (field.text.isBlank()) chosen = options.firstOrNull()?.target }
    val option = options.firstOrNull { it.target == chosen }
    val haptic = LocalHapticFeedback.current
    // Keep a draft visible even if its target disappears; only an explicit choice may retarget it.
    if (options.isEmpty() && field.text.isBlank()) {
        Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { actions() }
        return
    }
    val input = option?.input
    val canSend = input != null && field.text.isNotBlank() && field.text.length <= MAX_REPLY &&
        (input.allowFreeFormInput || input.choices.orEmpty().any { it.toString() == field.text.toString() })
    val send: (String) -> Unit = { value ->
        val target = option?.target
        if (target != null && value.isNotBlank()) {
            val ok = controller.reply(preview, target, value)
            if (ok) {
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                onSent(value)
                field.edit { replace(0, length, "") }
            }
            report(ok)
        }
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (option == null || (options.size > 1 && input?.allowFreeFormInput != true)) {
            // Which of the app's answer routes to use: Orion's choice buttons (Expressive toggle buttons).
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                options.forEach { choice ->
                    ToggleButton(checked = choice.target == chosen, onCheckedChange = { chosen = choice.target },
                        shapes = ToggleButtonDefaults.shapesFor(ToggleButtonDefaults.MinHeight)) {
                        Text(choice.title.ifBlank { stringResource(R.string.notification_reply) }, maxLines = 1)
                    }
                }
            }
        }
        input?.choices?.takeIf { it.isNotEmpty() }?.let { choices ->
            // The app's own quick answers: tonal pills that spring in one after another and send on a tap. They are an
            // alternative to typing, so they step aside while something is being typed.
            AnimatedVisibility(
                visible = field.text.isEmpty(),
                enter = expandVertically(AppMotion.spatial()) + fadeIn(AppMotion.effects()),
                exit = shrinkVertically(AppMotion.spatial()) + fadeOut(AppMotion.fastEffects()),
            ) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    choices.forEachIndexed { index, choice ->
                        QuickReply(choice.toString(), index) { send(choice.toString()) }
                    }
                }
            }
        }
        if (input?.allowFreeFormInput == false) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { actions() }
        if (input?.allowFreeFormInput != false) {
            val cs = MaterialTheme.colorScheme
            val spatial = AppMotion.fastSpatial<Float>()
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Row(
                    Modifier.weight(1f).heightIn(min = 56.dp).clip(MaterialTheme.shapes.extraLarge).background(cs.surfaceContainerHigh),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val placeholder = input?.label?.toString()?.takeIf { it.isNotBlank() } ?: stringResource(R.string.notification_reply)
                    BasicTextField(
                        state = field,
                        modifier = Modifier.weight(1f).padding(start = 20.dp, end = 4.dp, top = 16.dp, bottom = 16.dp),
                        inputTransformation = InputTransformation.maxLength(MAX_REPLY),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = cs.onSurface),
                        cursorBrush = SolidColor(cs.primary),
                        lineLimits = TextFieldLineLimits.MultiLine(maxHeightInLines = 5),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Send),
                        onKeyboardAction = { if (canSend) send(field.text.toString()) },
                        decorator = { inner ->
                            Box {
                                if (field.text.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant, maxLines = 1)
                                inner()
                            }
                        },
                    )
                    actions()
                }
                AnimatedVisibility(
                    visible = field.text.isNotBlank(),
                    enter = expandHorizontally(AppMotion.fastSpatial(), expandFrom = Alignment.Start) + scaleIn(spatial, .4f) + fadeIn(AppMotion.fastEffects()),
                    exit = shrinkHorizontally(AppMotion.fastSpatial(), shrinkTowards = Alignment.Start) + scaleOut(spatial, .4f) + fadeOut(AppMotion.fastEffects()),
                ) {
                    FilledIconButton(
                        onClick = { send(field.text.toString()) },
                        enabled = canSend,
                        modifier = Modifier.padding(start = 8.dp).size(56.dp),
                        shapes = IconButtonDefaults.shapes(),
                    ) {
                        Icon(Icons.Filled.ArrowUpward, stringResource(R.string.notification_send))
                    }
                }
            }
        }
    }
}

/** One quick answer: a tonal Expressive button whose shape squeezes on press, springing in after the ones before it. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun QuickReply(text: String, index: Int, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.heightIn(min = ButtonDefaults.MinHeight).orionReveal(index),
        shapes = ButtonDefaults.shapes(),
        // The composer's own surface with the answer in the accent: one family with the field, not a row of slabs.
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.primary,
        ),
        contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MinHeight),
    ) {
        Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private const val MAX_REPLY = 4096
