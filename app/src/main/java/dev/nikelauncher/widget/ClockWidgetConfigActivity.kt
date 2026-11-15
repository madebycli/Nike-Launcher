// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.nikelauncher.ui.theme.NikeTheme
import dev.nikelauncher.widget.config.ClockConfigScreen
import dev.nikelauncher.NikeApp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * The clock widget's own settings, reached from the widget's menu.
 *
 * Declared as the provider's configure activity, so the system offers it both when the widget
 * is first added and whenever its settings are asked for again. It is cancelled by default:
 * a configure activity that finishes without RESULT_OK on a first add tells the system not to
 * keep the widget, which is what should happen if someone backs out of adding one.
 */
class ClockWidgetConfigActivity : ComponentActivity() {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        widgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        setResult(Activity.RESULT_CANCELED, resultIntent())

        lifecycleScope.launch {
            val stored = ClockWidgetConfig.read(this@ClockWidgetConfigActivity, widgetId)
            val initial = if (ClockWidgetConfig.hasConfiguration(this@ClockWidgetConfigActivity, widgetId)) stored
                else (application as NikeApp).prefs.widgetApps.choices.first().applyTo(stored)
            showConfig(initial)
        }
    }

    private fun showConfig(initial: ClockWidgetConfig) {
        setContent {
            NikeTheme {
                ClockConfigScreen(
                    widgetId = widgetId,
                    initial = initial,
                    onSave = ::save,
                    onCancel = ::finish,
                )
            }
        }
    }

    private fun save(config: ClockWidgetConfig) {
        ClockWidgetConfig.write(this, widgetId, config)
        // Redrawn here rather than waiting for the system to ask: nothing else would, since
        // this widget has no update interval of its own.
        ClockWidgetProvider.render(this, AppWidgetManager.getInstance(this), widgetId)
        // Weather arrives by broadcast, and the receiver for it exists only while something
        // wants it. This is the moment that can have changed.
        ClockWidgetProvider.syncWeatherReceiver(this)
        setResult(Activity.RESULT_OK, resultIntent())
        finish()
    }

    private fun resultIntent() = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
}
