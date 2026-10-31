// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.media

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.nikelauncher.R

/** Compact stock Material card, constrained to the existing reserved media field. */
@Composable
internal fun ExpressiveMediaCard(current: NowPlaying, heightDp: Int, controls: Boolean) {
    Card(modifier = Modifier.fillMaxWidth().height(heightDp.dp), shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            val art = current.art
            if (art != null) Image(art.asImageBitmap(), null, Modifier.size((heightDp * .62f).dp)
                .clip(MaterialTheme.shapes.small), contentScale = ContentScale.Crop)
            else Icon(Icons.Filled.MusicNote, null, Modifier.size((heightDp * .5f).dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(current.title.ifBlank { stringResource(R.string.now_playing_unknown_title) },
                    style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(current.artist, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (controls) {
                IconButton(onClick = { current.controller.transportControls.skipToPrevious() }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Filled.SkipPrevious, stringResource(R.string.now_playing_previous))
                }
                FilledTonalIconButton(onClick = {
                    if (current.isPlaying) current.controller.transportControls.pause() else current.controller.transportControls.play()
                }, modifier = Modifier.size(40.dp)) {
                    Icon(if (current.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        stringResource(R.string.now_playing_play_pause))
                }
                IconButton(onClick = { current.controller.transportControls.skipToNext() }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Filled.SkipNext, stringResource(R.string.now_playing_next))
                }
            }
        }
    }
}
