// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.notifications

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.nikelauncher.media.NotificationPreview
import kotlinx.coroutines.delay

/** A swipe requests dismissal; an unconfirmed or refused dismissal returns to its resting state. */
@Composable
internal fun NotificationDismissBox(preview: NotificationPreview, onDismiss: (NotificationPreview) -> Boolean, content: @Composable () -> Unit) {
    val currentPreview by rememberUpdatedState(preview)
    val requestDismiss by rememberUpdatedState(onDismiss)
    val state = rememberSwipeToDismissBoxState()
    LaunchedEffect(state.currentValue) {
        if (state.currentValue != SwipeToDismissBoxValue.Settled) {
            if (requestDismiss(currentPreview)) delay(2500)
            state.reset()
        }
    }
    SwipeToDismissBox(state, modifier = Modifier.testTag("notification-dismiss-${preview.key}").semantics {
        dismiss { requestDismiss(currentPreview) }
    }, backgroundContent = {
        if (state.dismissDirection != SwipeToDismissBoxValue.Settled) {
            Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.large) {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = if (state.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd) {
                    Icon(Icons.Outlined.DeleteOutline, null)
                }
            }
        }
    }) { content() }
}
