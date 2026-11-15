// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.screens

import android.content.ComponentName
import android.media.session.MediaSession
import android.media.session.MediaController
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import dev.nikelauncher.NikeApp
import dev.nikelauncher.R
import dev.nikelauncher.data.*
import dev.nikelauncher.media.*
import dev.nikelauncher.ui.home.*
import dev.nikelauncher.ui.state.*
import dev.nikelauncher.ui.theme.NikeTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Full home content with real settings, media content and edit interactions at Pixel 9 size. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "en-w411dp-h923dp-420dpi")
class HomeLayoutScreensTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    @Before fun optIn() { Assume.assumeTrue(System.getenv("NIKE_SCREENS") != null); pixel9Display(rule.activity) }
    @Test fun english() = exercise("home-fixed", 1f)
    @Test @Config(qualifiers = "de-w411dp-h923dp-420dpi") fun germanLarge() = exercise("home-fixed-de-large", 1.5f)

    private fun exercise(prefix: String, fontScale: Float) {
        val app = rule.activity.application as NikeApp
        val prefs = app.prefs
        val names = listOf("AccuBattery", "Accrescent", "Aerium", "AI Usage")
        installFakeApps(app, names)
        val apps = names.map { AppInfo(ComponentName("fake." + it.lowercase(), "fake." + it.lowercase() + ".Main"), it) }
        val byKey = apps.associateBy { it.key }
        runBlocking {
            prefs.replaceBackupValues(emptyMap()); prefs.home.setNowPlayingEnabled(true)
            prefs.homeLayout.setPins(mapOf(HomeBlock.NOW_PLAYING to BlockPin(PinEdge.TOP, .18f), HomeBlock.FAVORITES to BlockPin(PinEdge.TOP, .44f)))
        }
        Settings.Secure.putString(app.contentResolver, "enabled_notification_listeners", ComponentName(app, NowPlayingListenerService::class.java).flattenToString())
        val session = MediaSession(app, "fixture")
        val playing = NowPlaying("Night drive", "Sample artist", true, null, MediaController(app, session.sessionToken))
        NowPlayingBus.update(null)
        var editing by mutableStateOf(false)
        var generation by mutableIntStateOf(0)
        rule.setContent {
            key(generation) {
                val library = rememberLibraryState(prefs)
                val settings = rememberHomeSettings(prefs, library, byKey).copy(contentColor = Color.White)
                val media by NowPlayingBus.state.collectAsState()
                val scope = rememberCoroutineScope()
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                    NikeTheme { Surface(Modifier.fillMaxSize(), color = Color.Black) {
                        HomeScreen(settings, HomeData(apps.map { FavoriteEntry.App(it) }, byKey, emptyMap(), emptyList(), 180,
                            media != null, library.pins, library.blockOffsets), fakeHomeActions(prefs, scope), 0, editing, { editing = it })
                    } }
                }
            }
        }
        rule.waitUntil(10000) { rule.onAllNodesWithTag("home-frame-NOW_PLAYING").fetchSemanticsNodes().isNotEmpty() }
        fun top(block: HomeBlock) = rule.onNodeWithTag("home-frame-${block.name}").fetchSemanticsNode().boundsInRoot.top
        fun height(block: HomeBlock) = rule.onNodeWithTag("home-frame-${block.name}").fetchSemanticsNode().boundsInRoot.height
        fun text(id: Int) = app.getString(id)
        rule.waitForIdle()
        val favoriteTop = top(HomeBlock.FAVORITES)
        val mediaHeight = height(HomeBlock.NOW_PLAYING)
        rule.shoot("$prefix-idle")
        rule.runOnIdle { NowPlayingBus.update(playing) }; rule.shoot("$prefix-playing")
        Assert.assertEquals(favoriteTop, top(HomeBlock.FAVORITES), 1f)
        Assert.assertEquals(mediaHeight, height(HomeBlock.NOW_PLAYING), 1f)
        val titleLeft = rule.onNodeWithText(playing.title).fetchSemanticsNode().boundsInRoot.left
        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag("home-frame-NOW_PLAYING").performTouchInput { swipe(Offset(width * .2f, centerY), Offset(width * .3f, centerY), 500) }
        rule.mainClock.advanceTimeBy(32)
        rule.onNodeWithTag("home-frame-NOW_PLAYING").performTouchInput { down(Offset(width * .3f, centerY)); moveBy(Offset(60f, 0f)); cancel() }
        rule.mainClock.advanceTimeBy(4000); rule.mainClock.autoAdvance = true; rule.waitForIdle()
        Assert.assertEquals(titleLeft, rule.onNodeWithText(playing.title).fetchSemanticsNode().boundsInRoot.left, 1f)
        Assert.assertNotNull(NowPlayingBus.state.value)
        rule.shoot("$prefix-media-rebound")
        rule.runOnIdle { NowPlayingBus.update(playing.copy(isPlaying = false)) }; rule.shoot("$prefix-paused")
        rule.onNodeWithTag("home-frame-NOW_PLAYING").performTouchInput { swipe(Offset(width * .1f, centerY), Offset(width * .9f, centerY), 400) }
        rule.waitUntil(10000) { NowPlayingBus.state.value == null }
        rule.shoot("$prefix-dismissed")
        Assert.assertEquals(favoriteTop, top(HomeBlock.FAVORITES), 1f)
        rule.onRoot().performTouchInput { swipe(Offset(width * .3f, height * .92f), Offset(width * .3f, height * .70f), 500) }
        rule.mainClock.advanceTimeBy(4000); rule.waitForIdle()
        Assert.assertEquals(favoriteTop, top(HomeBlock.FAVORITES), 1f)
        rule.runOnIdle { editing = true }; rule.shoot("$prefix-edit")
        Assert.assertEquals(favoriteTop, top(HomeBlock.FAVORITES), 1f)
        Assert.assertEquals(mediaHeight, height(HomeBlock.NOW_PLAYING), 1f)
        rule.onNodeWithTag("home-select-NOW_PLAYING").performClick()
        rule.onNodeWithTag("home-select-FAVORITES").performClick()
        val mediaTop = top(HomeBlock.NOW_PLAYING)
        repeat(12) { rule.onNodeWithContentDescription(text(R.string.edit_move_up)).performClick() }
        rule.waitForIdle()
        val dp = rule.activity.resources.displayMetrics.density
        Assert.assertEquals(mediaTop - 12 * dp, top(HomeBlock.NOW_PLAYING), 2f)
        Assert.assertEquals(favoriteTop - 12 * dp, top(HomeBlock.FAVORITES), 2f)
        rule.shoot("$prefix-multi-selection")
        rule.onNodeWithContentDescription(text(R.string.edit_layout_settings)).performClick()
        rule.shoot("$prefix-layout-settings")
        rule.onAllNodes(SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsActions.SetProgress)).onLast().performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress) { it(32f) }
        rule.onAllNodes(SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsActions.SetProgress)).onFirst().performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress) { it(28f) }
        rule.waitForIdle(); rule.shoot("$prefix-layout-spaced")
        rule.onNodeWithTag("home-layout-close").performClick()
        rule.waitUntil(10000) { rule.onAllNodesWithText(text(R.string.edit_block_spacing)).fetchSemanticsNodes().isEmpty() }
        rule.waitForIdle()
        val moved = top(HomeBlock.FAVORITES)
        rule.onNodeWithContentDescription(text(R.string.action_done)).performClick()
        rule.mainClock.advanceTimeBy(2000); rule.waitForIdle()
        Assert.assertEquals(moved, top(HomeBlock.FAVORITES), 2f)
        rule.runOnIdle { generation++ }; rule.waitForIdle()
        rule.waitUntil(10000) { kotlin.math.abs(top(HomeBlock.FAVORITES) - moved) < 2f }
        rule.shoot("$prefix-restored")
        runBlocking {
            Assert.assertEquals(28, prefs.homeLayout.blockGapDp.first())
            Assert.assertEquals(32, prefs.homeLayout.mediaSidePaddingDp.first())
            Assert.assertEquals(32, prefs.appearance.sidePaddingDp.first())
            Assert.assertEquals(20, prefs.home.widgetSidePaddingDp.first())
        }
        session.release(); NowPlayingBus.update(null)
    }
}
