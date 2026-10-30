// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.notifications

import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.nikelauncher.R
import dev.nikelauncher.media.PreviewMessage
import dev.nikelauncher.ui.kit.menu.Menu

/** Material surfaces and swipe dismissal; message hiding affects only this in-memory view. */
@Composable
internal fun MessageBubble(message: PreviewMessage, maxWidth: Dp, onOpen: () -> Unit, onHide: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val hide = stringResource(R.string.notification_hide_message)
    val state = rememberSwipeToDismissBoxState()
    LaunchedEffect(state.currentValue) {
        if (state.currentValue != SwipeToDismissBoxValue.Settled) onHide()
    }
    Box {
        SwipeToDismissBox(state, backgroundContent = {},
            modifier = Modifier.widthIn(max = maxWidth).semantics { dismiss { onHide(); true } }) {
            Surface(shape = MaterialTheme.shapes.large,
                color = if (message.fromSelf) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = if (message.fromSelf) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.combinedClickable(onClick = onOpen, onLongClick = { menu = true })) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    attachmentBitmap(message.attachment)?.let {
                        Image(it, stringResource(R.string.notification_photo), Modifier.heightIn(max = 160.dp), contentScale = ContentScale.Fit)
                    }
                    if (message.text.isNotBlank()) Text(message.text, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        Menu(expanded = menu, onDismissRequest = { menu = false }) {
            item(hide, { menu = false; onHide() }, leadingIcon = Icons.Outlined.VisibilityOff)
        }
    }
}
