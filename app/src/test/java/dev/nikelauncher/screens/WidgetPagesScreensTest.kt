// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.screens

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import dev.nikelauncher.R
import dev.nikelauncher.data.*
import dev.nikelauncher.ui.home.pages.PageGestureExclusions
import dev.nikelauncher.ui.home.pages.WidgetGridPage
import dev.nikelauncher.ui.theme.NikeTheme
import dev.nikelauncher.widget.ClockWidgetProvider
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "de-w411dp-h923dp-420dpi")
class WidgetPagesScreensTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    @Before fun optIn() {
        Assume.assumeTrue(System.getenv("NIKE_SCREENS") != null)
        pixel9Display(rule.activity)
    }
    @Test fun realGridAndResizeDialog() {
        val context = rule.activity
        val manager = AppWidgetManager.getInstance(context)
        val provider = AppWidgetProviderInfo().apply { this.provider = ClockWidgetProvider.componentName(context); label = "Clock" }
        ReflectionHelpers.setField(provider, "providerInfo", ActivityInfo().apply {
            applicationInfo = context.applicationInfo; packageName = context.packageName
            name = ClockWidgetProvider::class.java.name; nonLocalizedLabel = "Clock"
        })
        shadowOf(manager).addInstalledProvider(provider)
        shadowOf(manager).setAllowedToBindAppWidgets(true)
        manager.bindAppWidgetIdIfAllowed(81, provider.provider)
        var widgets by mutableStateOf(listOf(GridWidget(81)))
        rule.setContent {
            NikeTheme {
                val exclusions = remember { PageGestureExclusions(rule.activity.window.decorView) }
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.inverseSurface) {
                    WidgetGridPage(WidgetPageSide.LEFT, widgets, editing = true, active = true, gestureExclusions = exclusions,
                        onEdit = {}, onDone = {}, onAdd = {}, onRemove = { id -> widgets = widgets.filterNot { it.id == id } },
                        onResize = { resized -> widgets = widgets.map { if (it.id == resized.id) resized else it } },
                        onMove = { id, index -> widgets = moveGridWidget(widgets, id, index) }, onWidgetSettings = {})
                }
            }
        }
        rule.shoot("widget-page-edit")
        rule.onNodeWithContentDescription("Clock").performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.shoot("widget-page-resize")
        rule.onNodeWithText(context.getString(R.string.widget_pages_narrow)).performClick()
        rule.onAllNodesWithText(context.getString(R.string.widget_pages_done)).onLast().performClick()
        Assert.assertEquals(1, widgets.single().columns)
        rule.shoot("widget-page-narrow")
    }
}
