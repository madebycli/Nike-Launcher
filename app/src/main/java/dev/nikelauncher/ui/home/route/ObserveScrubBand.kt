// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.Snapshot
import dev.nikelauncher.ui.applist.ScrubBand

/** Keep moving-band invalidations inside its consumers; a hidden list reads its band on reveal. */
@Composable
internal fun ObserveScrubBand(bands: BandEditing, active: Boolean = true, content: @Composable (ScrubBand) -> Unit) {
    val band = if (active) bands.band else Snapshot.withoutReadObservation { bands.band }
    content(band)
}
