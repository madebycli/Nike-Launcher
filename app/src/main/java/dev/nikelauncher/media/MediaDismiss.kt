// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.media

import android.media.session.MediaSession
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/** Stock Material swipe state owns cancellation and rebound; a new session starts at rest. */
@Composable
internal fun MediaDismiss(token: MediaSession.Token, onDismiss: () -> Unit, modifier: Modifier = Modifier,
    content: @Composable () -> Unit) {
    key(token) {
        val callback by rememberUpdatedState(onDismiss)
        val threshold = with(LocalDensity.current) { 120.dp.toPx() }
        val state = rememberSwipeToDismissBoxState(positionalThreshold = { threshold })
        SwipeToDismissBox(state, modifier = modifier, backgroundContent = {},
            enableDismissFromEndToStart = false, onDismiss = { callback() }) { content() }
    }
}
