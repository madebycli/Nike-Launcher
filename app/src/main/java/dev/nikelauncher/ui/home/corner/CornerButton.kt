// SPDX-License-Identifier: GPL-3.0-or-later
// Adapted from Victoria Grid (GPL-3.0-or-later), ui/home/CornerActionButton.kt.
package dev.nikelauncher.ui.home.corner

import android.appwidget.AppWidgetManager
import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import dev.nikelauncher.data.CornerPosition
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.nikelauncher.NikeApp
import dev.nikelauncher.R
import dev.nikelauncher.data.CornerAction
import dev.nikelauncher.data.EntryKind
import dev.nikelauncher.data.HomeAlignment
import dev.nikelauncher.service.SystemUi
import dev.nikelauncher.ui.common.AppShortcutMenu
import dev.nikelauncher.ui.common.toast
import dev.nikelauncher.ui.home.CornerActions
import dev.nikelauncher.ui.notifications.NotificationSheet
import dev.nikelauncher.widget.ClockOverviewSheet
import dev.nikelauncher.widget.ClockWidgetConfig
import dev.nikelauncher.widget.ClockWidgetProvider

/**
 * The button in the corner nearest the thumb. A tap and a swipe up each do what they are set to:
 * open the menu, the search, an app, or one of the sheets; a long press opens its settings.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun BoxScope.CornerButton(
    corner: CornerActions,
    alignment: HomeAlignment,
    iconSizeDp: Int,
    contentColor: Color,
) {
    val context = LocalContext.current
    val app = context.applicationContext as NikeApp
    val loaded by app.prefs.corner.settings.collectAsState(null)
    val settings = loaded ?: return
    DisposableEffect(settings.enabled) { onDispose { corner.onBounds(null) } }

    var configure by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var overview by remember { mutableStateOf<String?>(null) }
    var notifications by remember { mutableStateOf(false) }
    var shortcuts by remember { mutableStateOf(false) }
    val cornerApp = corner.app

    fun perform(action: CornerAction) {
        when (action) {
            CornerAction.MENU -> menu = true
            CornerAction.SEARCH -> corner.onOpenSearch()
            CornerAction.APP -> if (cornerApp != null) corner.onOpenApp(cornerApp) else configure = true
            CornerAction.SHORTCUTS -> if (cornerApp != null && cornerApp.kind == EntryKind.APP) shortcuts = true else configure = true
            CornerAction.CALENDAR -> overview = "agenda"
            CornerAction.WEATHER -> overview = "weather"
            CornerAction.NOTIFICATIONS -> notifications = true
            CornerAction.SETTINGS -> corner.onOpenSettings()
            CornerAction.ASSISTANT -> runCatching { context.startActivity(Intent(Intent.ACTION_ASSIST).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                .onFailure { context.toast(R.string.clock_action_unavailable) }
            CornerAction.SHADE -> if (!SystemUi.expandNotificationShade(context)) context.toast(R.string.toast_enable_accessibility_shade)
            CornerAction.LOCK -> if (!SystemUi.lockScreen()) context.toast(R.string.toast_enable_accessibility_lock)
        }
    }
    val latestSwipe by rememberUpdatedState(settings.swipe)
    val latestPerform by rememberUpdatedState<(CornerAction) -> Unit>(::perform)
    val configureLabel = stringResource(R.string.corner_configure)
    val swipeLabel = stringResource(R.string.corner_swipe_up)

    if (settings.enabled) BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val insets = WindowInsets.safeDrawing
        val travel = cornerTravel(constraints.maxWidth, constraints.maxHeight, with(density) { settings.sizeDp.dp.roundToPx() },
            with(density) { 20.dp.roundToPx() }, insets.getTop(density), insets.getBottom(density) + with(density) { 24.dp.roundToPx() })
        val location = settings.position?.let(travel::location)
        CornerButtonFace(
            shape = settings.shape,
            sizeDp = settings.sizeDp,
            action = settings.tap,
            app = cornerApp,
            overWallpaper = contentColor,
            appearance = settings.appearance,
            modifier = (if (location != null) Modifier.offset { IntOffset(location.first, location.second) }
                else Modifier.align(if (alignment == HomeAlignment.RIGHT) Alignment.BottomStart else Alignment.BottomEnd)
                    .navigationBarsPadding().padding(horizontal = 20.dp, vertical = 24.dp))
                // Reported so the swipe edge can stop short of it, or a tap on it opens the list instead.
                .onGloballyPositioned { corner.onBounds(it.boundsInWindow()) }
                .semantics {
                    customActions = listOf(
                        CustomAccessibilityAction(configureLabel) { configure = true; true },
                        CustomAccessibilityAction(swipeLabel) { perform(settings.swipe); true },
                    )
                }
                .combinedClickable(
                    indication = ripple(),
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    onClick = { perform(settings.tap) },
                    onLongClick = { configure = true },
                    onLongClickLabel = configureLabel,
                )
                .pointerInput(Unit) {
                    var travel = 0f
                    detectVerticalDragGestures(
                        onDragStart = { travel = 0f },
                        onDragEnd = { if (travel < -24.dp.toPx()) latestPerform(latestSwipe) },
                    ) { change, dy ->
                        change.consume()
                        travel += dy
                    }
                },
        )
    }

    if (configure) {
        CornerSettingsSheet(cornerApp?.label, contentColor, onPickApp = { configure = false; corner.onPickApp() }, onPlace = { configure = false; corner.onPlace() }, onDismiss = { configure = false })
    }
    if (menu) {
        CornerActionSheet(cornerApp?.label, onDismiss = { menu = false }, onAction = ::perform, onConfigure = { configure = true })
    }
    overview?.let { mode ->
        val widgetIds by app.prefs.home.widgetIds.collectAsState(emptyList())
        // The overview reads the first clock on the home screen, or the defaults when there is none.
        val clockId = remember(widgetIds) {
            val manager = AppWidgetManager.getInstance(context)
            widgetIds.firstOrNull { manager.getAppWidgetInfo(it)?.provider == ClockWidgetProvider.componentName(context) } ?: -1
        }
        ClockOverviewSheet(
            clockId,
            mode,
            { overview = null },
            configOverride = ClockWidgetConfig.read(context, clockId).let { if (mode == "weather") it.copy(showWeather = true) else it },
        )
    }
    if (shortcuts && cornerApp != null) AppShortcutMenu(cornerApp, true) { shortcuts = false }
    if (notifications) NotificationSheet(onDismiss = { notifications = false })
}
