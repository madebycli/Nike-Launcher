// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.widget

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.os.Bundle
import android.os.Build
import android.util.SizeF
import android.content.Intent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.material3.TextButton
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dev.nikelauncher.NikeApp
import dev.nikelauncher.ui.common.TouchAnchoredMenu
import kotlinx.coroutines.delay
import dev.nikelauncher.R
import androidx.compose.ui.res.stringResource

/** One hosted widget. [onLongPress] carries the press position in this page's local pixels. */
@Composable
internal fun WidgetPage(
    widgetId: Int,
    interactionsEnabled: Boolean = true,
    visible: Boolean = true,
    configuredHeightDp: Int = 0,
    onNeededHeightDp: ((Int) -> Unit)? = null,
    onPressAndHold: (x: Float, y: Float) -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as NikeApp
    val appWidgetManager = remember { AppWidgetManager.getInstance(context) }
    val owner = LocalLifecycleOwner.current
    var providerRevision by remember { mutableIntStateOf(0) }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) providerRevision++
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val pending by app.prefs.pendingWidgetRestores.collectAsState(initial = emptyList())
    val providerInfo: AppWidgetProviderInfo? = remember(widgetId, providerRevision) {
        if (widgetId > 0) appWidgetManager.getAppWidgetInfo(widgetId) else null
    }
    val density = LocalDensity.current
    var slotSizeDp by remember { mutableStateOf(0 to 0) }
    var reportedSizeDp by remember(widgetId) { mutableStateOf(0 to 0) }

    if (providerInfo == null) {
        // The provider is gone — its app was uninstalled or disabled. Draw something that
        // still takes a long press, or this page is a blank gap with no way to remove it.
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .pointerInput(widgetId) {
                    detectTapGestures(onLongPress = { offset -> onPressAndHold(offset.x, offset.y) })
                },
            color = Color.Black.copy(alpha = 0.25f),
            shape = MaterialTheme.shapes.largeIncreased,
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(stringResource(R.string.widget_unavailable), color = Color.White.copy(alpha = 0.8f))
                if (pending.any { it.id == widgetId }) TextButton(onClick = {
                    context.startActivity(Intent(context, WidgetRestoreActivity::class.java))
                }) { Text(stringResource(R.string.backup_widget_setup)) }
            }
        }
        return
    }

    // A real long-press (finger held still) opens the edit menu; an ordinary tap or drag
    // still reaches the widget's own view untouched — see LongPressFrameLayout.
    val scrollTarget = remember(widgetId) { WidgetScrollTarget() }
    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                slotSizeDp = with(density) { size.width.toDp().value.toInt() to size.height.toDp().value.toInt() }
            }
            .protectWidgetScroll(scrollTarget, interactionsEnabled && visible),
        factory = { ctx ->
            // A widget draws with code from the app that provides it, and one that throws on
            // the way up would otherwise take the launcher down with it — leaving a home
            // screen that crashes on sight and no obvious way back. An empty slot is a poor
            // widget but it is still a home screen.
            // createView already binds the id to the view; calling setAppWidget after it is
            // not a second helping of the same thing — see the update block.
            val hostView = if (providerInfo.provider == ClockWidgetProvider.componentName(ctx)) {
                ClockHostView(ctx).also { clock ->
                    clock.widgetId = widgetId
                    // Editing blocks taps, not the live content used to judge spacing/size.
                    clock.active = visible
                    clock.interactive = interactionsEnabled
                    clock.configuredHeightDp = configuredHeightDp
                    clock.onNeededHeightDp = onNeededHeightDp
                    clock.setViewCompositionStrategy(androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
                }
            } else runCatching {
                app.widgetHost.createView(ctx, widgetId, providerInfo)
            }.getOrNull()
            LongPressFrameLayout(ctx).apply {
                scrollTarget.view = this
                hostView?.let { addView(it) }
                onLongPress = { x, y -> onPressAndHold(x, y) }
            }
        },
        update = { container ->
            container.importantForAccessibility = if (interactionsEnabled) {
                android.view.View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
            } else {
                android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
            }
            (container.getChildAt(0) as? ClockHostView)?.let {
                it.active = visible
                it.interactive = interactionsEnabled
                it.configuredHeightDp = configuredHeightDp
                it.onNeededHeightDp = onNeededHeightDp
            }
            val hostView = container.getChildAt(0) as? AppWidgetHostView
            // Deliberately not calling setAppWidget here any more, and this is the whole of
            // issue #80. It does not merely restate which widget this is: it resets the view
            // to the provider's initial layout, throwing away whatever the widget has since
            // drawn. A widget whose content arrives on its own schedule — a calendar agenda
            // and anything else backed by a collection — was being wiped back to its loading
            // state on every recomposition, and settled on "Can't show content" because it
            // never got to finish. A widget that draws once from a plain RemoteViews survived
            // it, which is why some worked and some did not.
            //
            // Widgets lay themselves out for the size they were *told*, not the size of the
            // view; without this they render for some other size and get clipped.
            //
            // Pass the widget's existing options rather than an empty Bundle — a blank one
            // replaces them outright, dropping the size hints (and on Android 12+ the size
            // list) that responsive widgets pick their layout from, which is how a widget
            // ends up drawing half its text. Only report real changes, since this ran on
            // every recomposition.
            val (wDp, hDp) = slotSizeDp
            if (hostView != null && wDp > 0 && hDp > 0 && slotSizeDp != reportedSizeDp) {
                reportedSizeDp = slotSizeDp
                val options = runCatching { appWidgetManager.getAppWidgetOptions(widgetId) }
                    .getOrNull() ?: Bundle()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // Responsive RemoteViews (including Breezy Weather) select layouts from
                    // this list on Android 12+, not just the legacy min/max rectangle.
                    hostView.updateAppWidgetSize(options, listOf(SizeF(wDp.toFloat(), hDp.toFloat())))
                } else {
                    @Suppress("DEPRECATION")
                    hostView.updateAppWidgetSize(options, wDp, hDp, wDp, hDp)
                }
            }
            container.onLongPress = { x, y -> onPressAndHold(x, y) }
        },
    )
}

/** Settings belong to the selected widget and remain reachable above its RemoteViews. */
@Composable
internal fun WidgetSettingsButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shadowElevation = 3.dp,
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(56.dp)) {
            Icon(Icons.Filled.Tune, stringResource(R.string.widget_change_settings))
        }
    }
}
