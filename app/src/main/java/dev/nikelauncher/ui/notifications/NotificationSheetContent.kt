// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.notifications

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shadow
import androidx.compose.runtime.saveable.rememberSaveable
import dev.nikelauncher.media.NotificationPreview
import dev.nikelauncher.ui.kit.motion.AppMotion
import kotlinx.coroutines.delay

private const val EMPTY_CLOSE_DELAY_MS = 700L

/** Presentation and page selection, shared by the live sheet and invented-data render tests. */
@Composable
internal fun NotificationSheetContent(
    notifications: List<NotificationPreview>,
    initialKey: String?,
    controller: NotificationController,
    onClose: () -> Unit,
) {
    var selected by rememberSaveable { mutableStateOf(initialKey) }
    val current = notifications.firstOrNull { it.key == selected }
    LaunchedEffect(current == null) { if (current == null) selected = null }
    BackHandler(selected != null) { selected = null }
    // Clearing the last notification closes the sheet at once. The rows it had are kept on screen while it
    // slides away, so the sheet does not shrink to an empty page first. One opened empty stays open.
    var hadAny by remember { mutableStateOf(notifications.isNotEmpty()) }
    var lastShown by remember { mutableStateOf(notifications) }
    LaunchedEffect(notifications) { if (notifications.isNotEmpty()) lastShown = notifications }
    LaunchedEffect(notifications.isEmpty()) {
        if (notifications.isNotEmpty()) hadAny = true
        else if (hadAny) onClose()
    }
    val shown = if (notifications.isEmpty() && hadAny) lastShown else notifications
    val effects = AppMotion.effects<Float>()
    val offset = AppMotion.offset()
    val size = AppMotion.size()
    CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodyMedium.copy(shadow = Shadow.None)) {
    AnimatedContent(
        targetState = selected,
        modifier = Modifier.fillMaxWidth(),
        transitionSpec = {
            val forward = targetState != null
            (slideInHorizontally(offset) { if (forward) it / 4 else -it / 4 } + fadeIn(effects)) togetherWith
                (slideOutHorizontally(offset) { if (forward) -it / 4 else it / 4 } + fadeOut(effects)) using
                SizeTransform(clip = false) { _, _ -> size }
        },
        label = "notificationPage",
    ) { key ->
        val preview = shown.firstOrNull { it.key == key }
        if (preview == null) {
            NotificationOverview(
                notifications = shown,
                onSelect = { selected = it.key },
                onDismiss = { controller.dismiss(it) },
                onClearAll = { notifications.toList().forEach { controller.dismiss(it) } },
            )
        } else {
            NotificationDetail(preview, controller, onBack = { selected = null }, onClose = onClose)
        }
    }
    }
}
