// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import dev.nikelauncher.widget.ClockWidgetConfig
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Initial app choices for new clocks; existing per-widget choices remain authoritative. */
data class WidgetApps(val clock: String? = null, val calendar: String? = null, val weather: String? = null) {
    fun applyTo(config: ClockWidgetConfig) = config.copy(clockPackage = clock, calendarPackage = calendar, weatherPackage = weather)
}

enum class WidgetAppSlot { CLOCK, CALENDAR, WEATHER }

class WidgetAppPrefs internal constructor(private val store: DataStore<Preferences>) {
    val choices = store.data.map { WidgetApps(it[CLOCK], it[CALENDAR], it[WEATHER]) }.distinctUntilChanged()
    suspend fun set(slot: WidgetAppSlot, packageName: String?) {
        val key = when (slot) { WidgetAppSlot.CLOCK -> CLOCK; WidgetAppSlot.CALENDAR -> CALENDAR; WidgetAppSlot.WEATHER -> WEATHER }
        store.edit { if (packageName.isNullOrBlank()) it.remove(key) else it[key] = packageName }
    }
    private companion object {
        val CLOCK = stringPreferencesKey("default_widget_clock_app")
        val CALENDAR = stringPreferencesKey("default_widget_calendar_app")
        val WEATHER = stringPreferencesKey("default_widget_weather_app")
    }
}
