// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.route

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.nikelauncher.data.EdgeSide
import dev.nikelauncher.ui.applist.BandEditOverlay
import dev.nikelauncher.ui.applist.EdgeScrubber
import dev.nikelauncher.ui.applist.ScrubState
import dev.nikelauncher.ui.home.HomeSettings

/** The idle alphabet and its editor subscribe to the band without invalidating the home route. */
@Composable
internal fun BoxScope.HomeBandOverlays(settings: HomeSettings, bands: BandEditing, scrub: ScrubState, letters: List<Char>,
    listVisible: Boolean, homeEditing: Boolean, viewportHeightPx: Int, onReset: () -> Unit) {
    val showIdleStrip = !listVisible && (bands.editing || (!homeEditing && settings.stripInsetSide != null))
    if (showIdleStrip) ObserveScrubBand(bands) { band ->
        EdgeScrubber(letters = letters, scrubY = { null }, pullPx = { 0f }, band = band, side = scrub.side,
            style = settings.scrubStyle, ripple = settings.rippleParams,
            modifier = Modifier.align(if (scrub.side == EdgeSide.LEFT) Alignment.CenterStart else Alignment.CenterEnd))
    }
    if (bands.editing) ObserveScrubBand(bands) { band ->
        BandEditOverlay(band = band, side = scrub.side, viewportHeightPx = viewportHeightPx, letters = letters,
            onBandChange = { bands.live = it }, onReset = onReset, onDone = bands::commit)
    }
}
