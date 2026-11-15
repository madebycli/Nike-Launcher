// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.media

import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.os.SystemClock
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

/** Read the session once per second only while the optional indicator is visible and playing. */
@Composable
internal fun MediaProgress(current: NowPlaying, modifier: Modifier = Modifier) {
    var progress by remember(current.controller.sessionToken) { mutableStateOf<Float?>(null) }
    var revision by remember(current.controller.sessionToken) { mutableIntStateOf(0) }
    DisposableEffect(current.controller) {
        val callback = object : MediaController.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackState?) { revision++ }
            override fun onMetadataChanged(metadata: MediaMetadata?) { revision++ }
        }
        current.controller.registerCallback(callback)
        onDispose { current.controller.unregisterCallback(callback) }
    }
    LaunchedEffect(current.controller.sessionToken, current.isPlaying, revision) {
        do {
            val state = current.controller.playbackState
            val duration = current.controller.metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0
            progress = state?.let {
                val elapsed = if (current.isPlaying && it.lastPositionUpdateTime > 0)
                    SystemClock.elapsedRealtime() - it.lastPositionUpdateTime else 0L
                mediaProgress(it.position, duration, elapsed, if (current.isPlaying) it.playbackSpeed else 0f)
            }
            if (current.isPlaying) delay(1000)
        } while (current.isPlaying)
    }
    progress?.let { value -> LinearProgressIndicator(progress = { value }, modifier = modifier) }
}
