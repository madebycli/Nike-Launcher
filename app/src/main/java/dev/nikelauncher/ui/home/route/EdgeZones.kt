// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.route

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import dev.nikelauncher.data.EdgeSide
import dev.nikelauncher.ui.applist.EdgeTouchZone
import dev.nikelauncher.ui.applist.ScrubBand
import dev.nikelauncher.ui.applist.ScrubState
import dev.nikelauncher.ui.home.HomeSettings

/**
 * The invisible strips at the screen edge that open the app list. They sit on top of everything,
 * so one unbroken touch opens the list and then scrubs it as the finger moves.
 */
@Composable
internal fun BoxScope.EdgeZones(
    settings: HomeSettings,
    scrub: ScrubState,
    letters: List<Char>,
    band: ScrubBand,
    insetLeftDp: Int,
    insetRightDp: Int,
    cornerButtonBounds: Rect?,
    listOpen: Boolean,
    controller: AppListController,
    onDoubleTapLock: (() -> Unit)?,
) {
    val view = LocalView.current
    val density = LocalDensity.current.density
    val sides = remember(settings.edgeSide) {
        when (settings.edgeSide) {
            EdgeSide.LEFT -> listOf(EdgeSide.LEFT)
            EdgeSide.RIGHT -> listOf(EdgeSide.RIGHT)
            EdgeSide.BOTH -> listOf(EdgeSide.LEFT, EdgeSide.RIGHT)
        }
    }
    sides.forEach { side ->
        val insetDp = if (side == EdgeSide.LEFT) insetLeftDp else insetRightDp
        // The strip runs the full height, and the corner button can sit under it; so it stops
        // above the button whenever the two share any width.
        val keepOut = cornerButtonBounds?.takeIf { !listOpen }?.let { b ->
            val reach = (insetDp + settings.edgeZoneWidthDp) * density
            val overlaps = if (side == EdgeSide.LEFT) b.left < reach else b.right > view.width - reach
            if (overlaps) ((view.height - b.top) / density).coerceAtLeast(0f).dp else null
        } ?: 0.dp
        EdgeTouchZone(
            side = side,
            widthDp = settings.edgeZoneWidthDp.dp,
            insetDp = insetDp.dp,
            bottomKeepOut = keepOut,
            letters = letters,
            band = band,
            bandOnly = settings.edgeZoneBandOnly,
            holdScroll = settings.holdScroll,
            hapticsEnabled = settings.hapticsEnabled,
            state = scrub,
            listOpen = listOpen,
            onDismiss = { controller.close() },
            onOpen = { controller.openFully() },
            onDoubleTap = onDoubleTapLock,
            // Aligned to the top when it is confined, because the offset that puts it against
            // the band is measured from there.
            modifier = Modifier.align(
                when {
                    settings.edgeZoneBandOnly && side == EdgeSide.LEFT -> Alignment.TopStart
                    settings.edgeZoneBandOnly -> Alignment.TopEnd
                    side == EdgeSide.LEFT -> Alignment.CenterStart
                    else -> Alignment.CenterEnd
                },
            ),
        )
    }
}
