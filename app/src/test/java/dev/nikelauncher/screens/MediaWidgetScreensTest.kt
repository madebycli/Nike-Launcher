// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.screens

import android.content.ComponentName
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import dev.nikelauncher.NikeApp
import dev.nikelauncher.R
import dev.nikelauncher.data.prefs.MediaWidgetStyle
import kotlinx.coroutines.flow.first
import dev.nikelauncher.media.*
import dev.nikelauncher.ui.theme.NikeTheme
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Real media card and settings at the user's screen dimensions. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "en-w411dp-h923dp-420dpi")
class MediaWidgetScreensTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    @Before fun optIn() { Assume.assumeTrue(System.getenv("NIKE_SCREENS") != null); pixel9Display(rule.activity) }
    @After fun cleanup() { NowPlayingBus.update(null) }
    @Test fun english() = exercise("media-widget", 1f)
    @Test @Config(qualifiers = "de-w411dp-h923dp-night-420dpi") fun germanLarge() = exercise("media-widget-de-large", 1.5f)

    private fun exercise(prefix: String, fontScale: Float) {
        val app = rule.activity.application as NikeApp
        runBlocking { app.prefs.replaceBackupValues(emptyMap()) }
        Settings.Secure.putString(app.contentResolver, "enabled_notification_listeners", ComponentName(app, NowPlayingListenerService::class.java).flattenToString())
        val session = MediaSession(app, "fixture")
        session.setMetadata(MediaMetadata.Builder().putLong(MediaMetadata.METADATA_KEY_DURATION, 240_000).build())
        session.setPlaybackState(PlaybackState.Builder().setState(PlaybackState.STATE_PAUSED, 60_000, 0f).build())
        val media = NowPlaying("Night drive", "Sample artist", false, null, MediaController(app, session.sessionToken))
        NowPlayingBus.update(media)
        var settings by mutableStateOf(false)
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                NikeTheme { Surface(Modifier.fillMaxSize(), color = Color.Black) {
                    Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        NowPlayingWidget(heightDp = 88, contentColor = Color.White)
                        if (settings) MediaWidgetSettingsSheet { settings = false }
                    }
                } }
            }
        }
        rule.waitUntil(5000) { rule.onAllNodesWithText("Night drive").fetchSemanticsNodes().isNotEmpty() }
        rule.shoot("$prefix-classic")
        rule.runOnIdle { settings = true }
        rule.waitUntil(5000) { rule.onAllNodesWithText(app.getString(R.string.media_widget_settings)).fetchSemanticsNodes().isNotEmpty() }
        rule.shoot("$prefix-settings")
        rule.onNodeWithText(app.getString(R.string.media_widget_expressive)).performClick()
        rule.waitUntil(5000) { runBlocking { app.prefs.mediaWidget.options.first().style == MediaWidgetStyle.EXPRESSIVE } }
        rule.onNodeWithText(app.getString(R.string.media_widget_progress)).performClick()
        rule.waitUntil(5000) { runBlocking { app.prefs.mediaWidget.options.first().progress } }
        rule.waitForIdle()
        rule.runOnIdle { settings = false }
        rule.waitForIdle()
        // Robolectric does not reliably supply a MediaController session position here; the
        // calculation itself is covered by MediaProgressMathTest and needs a device session check.
        rule.shoot("$prefix-expressive-progress")
        rule.onNodeWithText("Night drive").assertIsDisplayed()
        rule.onNodeWithContentDescription(app.getString(R.string.now_playing_play_pause)).assertIsDisplayed()
        rule.runOnIdle { settings = true }
        rule.onNodeWithText(app.getString(R.string.media_widget_controls)).performClick()
        rule.waitUntil(5000) { runBlocking { !app.prefs.mediaWidget.options.first().controls } }
        rule.runOnIdle { settings = false }
        rule.waitForIdle()
        rule.onNodeWithContentDescription(app.getString(R.string.now_playing_play_pause)).assertDoesNotExist()
        rule.shoot("$prefix-no-controls")
        session.release()
    }
}
