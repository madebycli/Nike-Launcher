// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.common

import android.content.pm.ShortcutInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import dev.nikelauncher.NikeApp
import dev.nikelauncher.data.AppInfo
import dev.nikelauncher.data.ShortcutSwipe
import dev.nikelauncher.ui.kit.menu.Menu
import dev.nikelauncher.ui.kit.menu.MenuScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Most an app is allowed to contribute, so a menu stays a menu rather than a list. */
private const val MAX_SHORTCUTS = 5

/** One shortcut an app publishes, ready to be a menu row. */
class ShortcutEntry(val label: String, val icon: ImageBitmap?, val start: () -> Unit)

/**
 * The shortcuts an app publishes, read when [expanded] turns true rather than with the app list:
 * it is a call into the system per app, and an app list is hundreds of apps of which one is ever
 * asked about. Empty for an app that publishes none.
 */
@Composable
fun rememberAppShortcuts(app: AppInfo, expanded: Boolean): List<ShortcutEntry> {
    val context = LocalContext.current
    val nikeApp = context.applicationContext as NikeApp
    val density = LocalConfiguration.current.densityDpi
    var shortcuts by remember(app.key) { mutableStateOf<List<ShortcutInfo>>(emptyList()) }

    LaunchedEffect(app.key, expanded) {
        if (!expanded) return@LaunchedEffect
        shortcuts = withContext(Dispatchers.IO) { nikeApp.appRepository.appShortcuts(app).take(MAX_SHORTCUTS) }
    }
    return shortcuts.mapNotNull { shortcut ->
        val label = (shortcut.shortLabel ?: shortcut.longLabel)?.toString().orEmpty()
        if (label.isBlank()) return@mapNotNull null
        val icon = remember(shortcut.id, density) {
            runCatching {
                nikeApp.appRepository.shortcutIcon(shortcut, density)?.toBitmap(ICON_PX, ICON_PX)?.asImageBitmap()
            }.getOrNull()
        }
        ShortcutEntry(label, icon) { nikeApp.appRepository.startAppShortcut(shortcut) }
    }
}

/** Adds an app's shortcuts to a menu; adds nothing when it publishes none. */
@Composable
fun MenuScope.appShortcutItems(shortcuts: List<ShortcutEntry>, onStarted: () -> Unit) {
    shortcuts.forEach { entry ->
        item(text = entry.label, leadingImage = entry.icon, onClick = { entry.start(); onStarted() })
    }
}

/** Rasterised at menu-icon size; the drawable itself is whatever density the publisher had. */
private const val ICON_PX = 72

/**
 * Swipe a row sideways to reach what the app publishes.
 *
 * A separate gesture from the long press on purpose: shortcuts are used daily and the menu
 * behind a long press is set-up-and-done, so they do not belong in the same place.
 *
 * One direction, chosen in settings, so the other stays free to mean something later. It fires
 * once per drag rather than per pixel, and only past a distance no tap wanders.
 *
 * Nothing happens on a row whose app publishes none. There is no way to know that before
 * asking, and asking every row up front is a call into the system per app.
 */
fun Modifier.swipeForShortcuts(
    mode: ShortcutSwipe,
    enabled: Boolean,
    onSwipe: () -> Unit,
): Modifier =
    if (mode == ShortcutSwipe.OFF || !enabled) this else this.pointerInput(mode) {
        var travelled = 0f
        var fired = false
        detectHorizontalDragGestures(
            onDragStart = { travelled = 0f; fired = false },
            onDragEnd = { },
            onDragCancel = { },
        ) { change, amount ->
            travelled += amount
            val far = when (mode) {
                ShortcutSwipe.RIGHT -> travelled > SWIPE_THRESHOLD_PX
                ShortcutSwipe.LEFT -> travelled < -SWIPE_THRESHOLD_PX
                ShortcutSwipe.OFF -> false
            }
            if (!fired && far) {
                fired = true
                change.consume()
                onSwipe()
            }
        }
    }

/** Far enough not to fire on a tap that wandered, short enough to feel like a flick. */
private const val SWIPE_THRESHOLD_PX = 90f

/**
 * The shortcuts on their own, for the swipe. Renders nothing when the app publishes none, so
 * a swipe on such a row opens an empty popup rather than a stray one.
 *
 * Anchored to the row and not to the finger, unlike the long-press menu. That one is opened
 * from somewhere on a row that may be as tall as a widget, so it has to find the finger; this
 * one is always a swipe across a row one line high, and following the finger only meant it
 * landed somewhere slightly different every time. Against the row it is in the same place at
 * the same size on every use, which is what makes it something you reach for rather than read.
 */
@Composable
fun AppShortcutMenu(app: AppInfo, expanded: Boolean, onDismiss: () -> Unit) {
    val shortcuts = rememberAppShortcuts(app, expanded)
    Menu(expanded = expanded && shortcuts.isNotEmpty(), onDismissRequest = onDismiss) {
        appShortcutItems(shortcuts, onDismiss)
    }
}
