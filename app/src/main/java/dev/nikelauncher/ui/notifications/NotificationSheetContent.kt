// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.notifications

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.runtime.saveable.rememberSaveable
import dev.nikelauncher.media.NotificationPreview
import dev.nikelauncher.ui.kit.motion.AppMotion

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
    val effects = AppMotion.effects<Float>()
    val offset = AppMotion.offset()
    AnimatedContent(
        targetState = selected,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            val forward = targetState != null
            (slideInHorizontally(offset) { if (forward) it / 4 else -it / 4 } + fadeIn(effects)) togetherWith
                (slideOutHorizontally(offset) { if (forward) -it / 4 else it / 4 } + fadeOut(effects))
        },
        label = "notificationPage",
    ) { key ->
        val preview = notifications.firstOrNull { it.key == key }
        if (preview == null) {
            NotificationOverview(
                notifications = notifications,
                onSelect = { selected = it.key },
                onDismiss = { controller.dismiss(it) },
                onClearAll = { notifications.forEach { controller.dismiss(it) } },
                onClose = onClose,
            )
        } else {
            NotificationDetail(preview, controller, onBack = { selected = null }, onClose = onClose)
        }
    }
}
