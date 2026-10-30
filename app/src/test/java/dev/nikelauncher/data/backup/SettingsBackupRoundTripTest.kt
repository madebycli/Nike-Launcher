// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.data.backup

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import androidx.test.core.app.ApplicationProvider
import dev.nikelauncher.NikeApp
import dev.nikelauncher.data.*
import dev.nikelauncher.data.prefs.WidgetAppSlot
import dev.nikelauncher.widget.ClockWidgetConfig
import dev.nikelauncher.widget.ClockWidgetProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import android.content.pm.ActivityInfo

/** Exercise the real exporter and restorer, including a newly allocated clock widget id. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SettingsBackupRoundTripTest {
    @Test fun `new defaults layout offsets search colors and clock options survive restore`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<NikeApp>()
        val prefs = app.prefs
        val manager = AppWidgetManager.getInstance(app)
        val provider = AppWidgetProviderInfo().apply {
            this.provider = ClockWidgetProvider.componentName(app)
            label = "Clock"
            minWidth = 100
            minHeight = 100
        }
        ReflectionHelpers.setField(provider, "providerInfo", ActivityInfo().apply {
            applicationInfo = app.applicationInfo
            packageName = app.packageName
            name = ClockWidgetProvider::class.java.name
            nonLocalizedLabel = "Clock"
        })
        shadowOf(manager).addInstalledProvider(provider)
        shadowOf(manager).setAllowedToBindAppWidgets(true)
        shadowOf(app.widgetHost).setAppWidgetIdToAllocate(101)
        manager.bindAppWidgetIdIfAllowed(77, provider.provider)
        prefs.home.addWidgetId(77)
        prefs.home.setBlockOffset(HomeBlock.FAVORITES, 17)
        prefs.homeLayout.setGap(27)
        prefs.homeLayout.setEdges(setOf(HomeBlock.NOW_PLAYING), 31)
        prefs.homeLayout.setPins(mapOf(HomeBlock.NOW_PLAYING to BlockPin(PinEdge.TOP, .2f), HomeBlock.FAVORITES to BlockPin(PinEdge.TOP, .5f)))
        prefs.home.setQuickLaunchRegion(QuickLaunchRegion(13, 29))
        prefs.widgetApps.set(WidgetAppSlot.CLOCK, "fake.clock")
        prefs.widgetApps.set(WidgetAppSlot.CALENDAR, "fake.calendar")
        prefs.widgetApps.set(WidgetAppSlot.WEATHER, "fake.weather")
        prefs.corner.setSearchColorMode(CornerColorMode.CUSTOM)
        prefs.corner.setSearchAutomaticForeground(false)
        prefs.corner.setSearchForeground(0xFF223344.toInt())
        val clock = ClockWidgetConfig.read(app, 77).copy(
            datePattern = "dd.MM.yyyy", showAgenda = true, agendaCount = 3,
            agendaLookaheadHours = 168, agendaRelative = false, clockDataGapDp = 31,
            textWidthPercent = 67, timeOpacity = 72, weatherPackage = "fake.weather",
            clockPackage = "fake.clock", calendarPackage = "fake.calendar", excludedCalendars = setOf("2"),
        )
        ClockWidgetConfig.write(app, 77, clock)
        val before = prefs.storedValues()
        val backup = prefs.exportJson(true).json
        prefs.replaceBackupValues(emptyMap())
        val result = prefs.importJson(backup)
        assertNotNull(result)
        val restored = prefs.storedValues()
        before.filterKeys { it !in setOf("widget_ids", "widget_id") }.forEach { (key, value) -> assertEquals(key, value, restored[key]) }
        val id = prefs.home.widgetIds.first().single()
        assertEquals(101, id)
        assertEquals(clock, ClockWidgetConfig.read(app, id))
        assertEquals("fake.weather", prefs.widgetApps.choices.first().weather)
        assertEquals(27, prefs.homeLayout.blockGapDp.first())
        assertEquals(31, prefs.homeLayout.mediaSidePaddingDp.first())
        assertEquals(20, prefs.appearance.sidePaddingDp.first())
        assertFalse(prefs.corner.searchAppearance.first().automaticForeground)
        assertFalse(ClockWidgetConfig.hasConfiguration(app, 987))
        assertEquals("fake.clock", prefs.widgetApps.choices.first().applyTo(ClockWidgetConfig.read(app, 987)).clockPackage)
    }
}
