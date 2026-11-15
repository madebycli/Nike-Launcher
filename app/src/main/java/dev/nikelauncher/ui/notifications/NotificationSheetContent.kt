// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.notifications

import androidx.compose.animation.*
import androidx.compose.animation.core.snap
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shadow
import androidx.compose.runtime.saveable.rememberSaveable
import dev.nikelauncher.media.NotificationPreview
import dev.nikelauncher.ui.kit.motion.AppMotion
import dev.nikelauncher.ui.kit.motion.backPeek
import dev.nikelauncher.ui.kit.motion.rememberBackPeek
import androidx.compose.foundation.layout.Box
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
    val peek = rememberBackPeek(enabled = selected != null) { selected = null }
    val pageShape = MaterialTheme.shapes.extraLarge
    // Clearing the last notification closes the sheet at once. The rows it had are kept on screen while it
    // slides away, so the sheet does not shrink to an empty page first. One opened empty stays open.
    // A copy, so a list the caller keeps changing in place cannot empty what is held for the slide-out.
    val items = notifications.toList()
    var hadAny by remember { mutableStateOf(items.isNotEmpty()) }
    var lastShown by remember { mutableStateOf(items) }
    LaunchedEffect(items) { if (items.isNotEmpty()) lastShown = items }
    LaunchedEffect(items.isEmpty()) {
        if (items.isNotEmpty()) hadAny = true
        else if (hadAny) onClose()
    }
    val shown = if (items.isEmpty() && hadAny) lastShown else items
    val effects = AppMotion.effects<Float>()
    val offset = AppMotion.offset()
    CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodyMedium.copy(shadow = Shadow.None)) {
    AnimatedContent(
        targetState = selected,
        modifier = Modifier.fillMaxWidth(),
        transitionSpec = {
            val forward = targetState != null
            (slideInHorizontally(offset) { if (forward) it / 4 else -it / 4 } + fadeIn(effects)) togetherWith
                (slideOutHorizontally(offset) { if (forward) -it / 4 else it / 4 } + fadeOut(effects)) using
                // The sheet around this animates its own height. A second size spring here would restart that
                // one every frame and show as a second push and a late settle, so the page size is taken at once.
                SizeTransform(clip = false) { _, _ -> snap() }
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
            Box(Modifier.backPeek(peek, pageShape, MaterialTheme.colorScheme.surfaceContainerLow)) {
                NotificationDetail(preview, controller, onBack = { selected = null }, onClose = onClose)
            }
        }
    }
    }
}
