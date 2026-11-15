// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.route

import android.graphics.Rect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import dev.nikelauncher.data.EdgeSide
import dev.nikelauncher.ui.applist.ScrubBand

/** Android only honors this much gesture exclusion per side, so it is spent on the strip. */
private const val GESTURE_EXCLUSION_CAP_DP = 200

/**
 * Tells the system to keep its back gesture out of the pixels the A-Z strip's edge zone uses.
 *
 * Spent on the scrub band rather than spread over the whole screen height, and starting where the
 * strip now starts: excluding from the very edge while the strip is held inward would leave the
 * setting doing nothing.
 */
@Composable
internal fun EdgeGestureExclusion(
    edgeSide: EdgeSide,
    widthDp: Int,
    insetLeftDp: Int,
    insetRightDp: Int,
    band: ScrubBand,
) {
    val view = LocalView.current
    LaunchedEffect(edgeSide, widthDp, insetLeftDp, insetRightDp, view, band) {
        view.post {
            val density = view.resources.displayMetrics.density
            val widthPx = (widthDp * density).toInt()
            val insetLeftPx = (insetLeftDp * density).toInt()
            val insetRightPx = (insetRightDp * density).toInt()
            val h = view.height
            val w = view.width
            if (h <= 0 || w <= 0) return@post
            val capPx = (GESTURE_EXCLUSION_CAP_DP * density).toInt()
            val top = band.topPx.toInt().coerceIn(0, h)
            val bottom = band.bottomPx.toInt().coerceIn(top, h)
            // The band is usually taller than the cap, and whatever is left over stays the
            // system's: a touch there waits on the back-gesture detector before it reaches us.
            // Centring what we are given splits that remainder between the two ends.
            val height = bottom - top
            val exTop = if (height > capPx) top + (height - capPx) / 2 else top
            val exBottom = if (height > capPx) exTop + capPx else bottom
            val rects = buildList {
                if (edgeSide != EdgeSide.RIGHT) add(Rect(insetLeftPx, exTop, insetLeftPx + widthPx, exBottom))
                if (edgeSide != EdgeSide.LEFT) add(Rect(w - widthPx - insetRightPx, exTop, w - insetRightPx, exBottom))
            }
            ViewCompat.setSystemGestureExclusionRects(view, rects)
        }
    }
    // Settings and the other screens must get the system back gesture back.
    DisposableEffect(view) { onDispose { ViewCompat.setSystemGestureExclusionRects(view, emptyList()) } }
}
