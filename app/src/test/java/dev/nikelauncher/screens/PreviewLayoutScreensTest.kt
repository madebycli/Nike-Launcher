// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.screens

import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import dev.nikelauncher.NikeApp
import dev.nikelauncher.data.AppInfo
import dev.nikelauncher.data.IconSide
import dev.nikelauncher.ui.home.setup.StyleStep
import dev.nikelauncher.ui.theme.NikeTheme
import dev.nikelauncher.widget.*
import dev.nikelauncher.widget.config.PinnedPreview
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant

/** Resize the actual setup and clock previews; viewport and sibling icon sizes must stay stable. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "en-w411dp-h923dp-notnight-420dpi")
class PreviewLayoutScreensTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    @Before fun optIn() { Assume.assumeTrue(System.getenv("NIKE_SCREENS") != null); pixel9Display(rule.activity) }
    @Test fun light() = resize("preview-fixed-light", 1f)
    @Test @Config(qualifiers = "de-w411dp-h923dp-night-420dpi") fun darkGerman() = resize("preview-fixed-dark-de-large", 1.5f)
    private fun resize(prefix: String, scale: Float) {
        val app = rule.activity.application as NikeApp
        val a = app.prefs.appearance
        installFakeApps(app)
        val apps = SAMPLE_APPS.take(3).map { AppInfo(ComponentName("fake."+it.lowercase(), "fake."+it.lowercase()+".Main"), it) }
        runBlocking { app.prefs.replaceBackupValues(emptyMap()); a.setIconSizeDp(24) }
        var clock by mutableStateOf(false)
        var config by mutableStateOf(ClockWidgetConfig.read(app, 42).copy(timeSizeSp = 32, textColor = 0xffffffff.toInt(), showBattery = true))
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                NikeTheme { Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
                    if (clock) PinnedPreview(ClockLiveState(config, Instant.parse("2026-10-30T12:00:00Z"), null, null, null, "73%")) {}
                    else Column(Modifier.fillMaxSize().padding(horizontal = 24.dp)) { StyleStep(app, apps, apps) }
                } }
            }
        }
        rule.waitForIdle()
        val viewport = rule.onNodeWithTag("setup-style-preview").getUnclippedBoundsInRoot()
        rule.shoot("$prefix-setup-small")
        runBlocking { a.setIconSizeDp(96) }
        rule.waitForIdle()
        Assert.assertEquals(viewport, rule.onNodeWithTag("setup-style-preview").getUnclippedBoundsInRoot())
        apps.take(2).forEach { rule.onNodeWithTag("setup-preview-icon-${it.key}").assertHeightIsEqualTo(96.dp) }
        rule.shoot("$prefix-setup-large")
        runBlocking { a.setIconSide(IconSide.RIGHT); a.setLabelSizeSp(32) }
        rule.waitForIdle()
        apps.take(2).forEach { rule.onNodeWithTag("setup-preview-icon-${it.key}").assertWidthIsEqualTo(96.dp).assertHeightIsEqualTo(96.dp) }
        rule.shoot("$prefix-setup-right-large")
        runBlocking { a.setIconSizeDp(56); a.setItemSpacingDp(48); a.setLabelSizeSp(32) }
        rule.waitForIdle()
        Assert.assertEquals(viewport, rule.onNodeWithTag("setup-style-preview").getUnclippedBoundsInRoot())
        apps.take(2).forEach { rule.onNodeWithTag("setup-preview-icon-${it.key}").assertHeightIsEqualTo(56.dp) }
        rule.shoot("$prefix-setup-spacing")
        rule.onNodeWithTag("setup-preview-icon-${apps[2].key}").assertDoesNotExist()
        rule.onAllNodes(hasScrollAction()).onFirst().performTouchInput { swipeUp() }
        rule.waitForIdle()
        Assert.assertTrue(rule.onNodeWithTag("setup-style-preview").getUnclippedBoundsInRoot().top < viewport.top)
        rule.shoot("$prefix-setup-scrolled")
        rule.runOnIdle { clock = true }; rule.waitForIdle()
        val clockViewport = rule.onNodeWithTag("clock-preview").getUnclippedBoundsInRoot()
        rule.shoot("$prefix-clock-white-small")
        rule.runOnIdle { config = config.copy(timeSizeSp = 160, dateSizeSp = 72, clockDataGapDp = 40) }
        rule.waitForIdle()
        Assert.assertEquals(clockViewport, rule.onNodeWithTag("clock-preview").getUnclippedBoundsInRoot())
        rule.shoot("$prefix-clock-white-large")
        rule.runOnIdle { config = config.copy(textColor = 0xff000000.toInt()) }
        rule.shoot("$prefix-clock-black")
    }
}
