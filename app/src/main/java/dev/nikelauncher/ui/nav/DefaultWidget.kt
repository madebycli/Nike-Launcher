// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.nav

import android.appwidget.AppWidgetManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalContext
import dev.nikelauncher.NikeApp
import dev.nikelauncher.widget.ClockWidgetProvider

/**
 * A brand new launcher gets its own clock, so the home screen is not simply empty on first sight.
 * Only ever on an install that has never been set up (the marker is written once, before anything
 * the user does) and only while there is no widget at all, so nobody's arrangement is added to and
 * a widget they removed does not come back.
 */
@Composable
fun EnsureDefaultWidget(app: NikeApp, layoutDefaultsVersion: Int?, widgetIds: List<Int>) {
    val context = LocalContext.current
    // Binding is refused until this launcher holds the home role, which on a first run is granted
    // on a system screen part way through the setup. Asking again each time the app comes back is
    // what puts the clock there once it has been granted.
    var resumed by remember { mutableIntStateOf(0) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) resumed++ }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(layoutDefaultsVersion, widgetIds, resumed) {
        if (layoutDefaultsVersion != 1 || widgetIds.isNotEmpty()) return@LaunchedEffect
        val manager = AppWidgetManager.getInstance(context)
        val id = app.widgetHost.allocateAppWidgetId()
        val bound = runCatching { manager.bindAppWidgetIdIfAllowed(id, ClockWidgetProvider.componentName(context)) }.getOrDefault(false)
        if (!bound) {
            // Binding is refused unless this launcher holds the home role, which it may not yet on
            // the very first run. The id is given back rather than left stranded.
            runCatching { app.widgetHost.deleteAppWidgetId(id) }
            return@LaunchedEffect
        }
        ClockWidgetProvider.render(context, manager, id)
        app.prefs.home.addWidgetId(id)
    }
}
