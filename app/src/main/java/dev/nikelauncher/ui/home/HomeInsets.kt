// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.nikelauncher.data.EdgeSide
import dev.nikelauncher.data.HomeAlignment

/** How much of an edge an always-on A-Z strip takes, matching what the app list holds clear. */
private val STRIP_INSET = 56.dp

/** The padding that keeps home content clear of the A-Z strip and of the invisible edge zone. */
internal class HomeInsets(val rows: Modifier, val folderControls: Modifier)

private fun covers(side: EdgeSide?, which: EdgeSide) = side == which || side == EdgeSide.BOTH

private fun pad(start: androidx.compose.ui.unit.Dp, end: androidx.compose.ui.unit.Dp): Modifier =
    if (start == 0.dp && end == 0.dp) Modifier else Modifier.padding(start = start, end = end)

/**
 * An always-on A-Z strip is drawn over this screen rather than beside it, so the side it occupies
 * is held clear. The invisible edge zone has to be cleared too, drawn or not: it is live on its
 * side the whole time the home screen is up, so a favorite under it gives its taps to the zone
 * and opens the app list instead of the app.
 *
 * Only on the side the favorites lean towards: that is the only place the zone can sit over an
 * icon. Centred favorites are held clear on both sides, or clearing one would pull them off centre.
 */
@Composable
internal fun rememberHomeInsets(
    stripInsetSide: EdgeSide?,
    edgeZoneSide: EdgeSide,
    edgeZoneExtentDp: Int,
    alignment: HomeAlignment,
): HomeInsets = remember(stripInsetSide, edgeZoneSide, edgeZoneExtentDp, alignment) {
    val zone = edgeZoneExtentDp.dp
    val zoneLeft = if (covers(edgeZoneSide, EdgeSide.LEFT)) zone else 0.dp
    val zoneRight = if (covers(edgeZoneSide, EdgeSide.RIGHT)) zone else 0.dp
    val centered = alignment == HomeAlignment.CENTER
    val zoneStart = when {
        centered -> maxOf(zoneLeft, zoneRight)
        alignment != HomeAlignment.RIGHT -> zoneLeft
        else -> 0.dp
    }
    val zoneEnd = when {
        centered -> maxOf(zoneLeft, zoneRight)
        alignment != HomeAlignment.LEFT -> zoneRight
        else -> 0.dp
    }
    val start = maxOf(if (covers(stripInsetSide, EdgeSide.LEFT)) STRIP_INSET else 0.dp, zoneStart)
    val end = maxOf(if (covers(stripInsetSide, EdgeSide.RIGHT)) STRIP_INSET else 0.dp, zoneEnd)

    // Controls in a folder (Now Playing's buttons, a widget's height slider) reach the far end,
    // which is the end the inset above leaves under the zone, and there the zone takes their taps.
    val folderStart = if (covers(edgeZoneSide, EdgeSide.LEFT) && alignment == HomeAlignment.RIGHT) zone else 0.dp
    val folderEnd = if (covers(edgeZoneSide, EdgeSide.RIGHT) && alignment == HomeAlignment.LEFT) zone else 0.dp
    HomeInsets(pad(start, end), pad(folderStart, folderEnd))
}
