// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.edit

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Badge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.nikelauncher.R
import dev.nikelauncher.data.EdgeSide
import dev.nikelauncher.ui.applist.ScrubBand
import dev.nikelauncher.ui.home.HomeSettings

/** Paint-only annotations: Canvas and Material badges have no pointer handlers. */
@Composable
internal fun HomeGestureZones(settings: HomeSettings, band: ScrubBand, insetLeftDp: Int, insetRightDp: Int, corner: Rect?, names: Map<String, String>) {
    val density = LocalDensity.current
    val colors = MaterialTheme.colorScheme
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val width = with(density) { maxWidth.toPx() }
        val height = with(density) { maxHeight.toPx() }
        val sides = when (settings.edgeSide) {
            EdgeSide.LEFT -> listOf(true)
            EdgeSide.RIGHT -> listOf(false)
            EdgeSide.BOTH -> listOf(true, false)
        }
        val edges = sides.map { fromLeft ->
            edgeGestureBounds(width, height, fromLeft, with(density) { settings.edgeZoneWidthDp.dp.toPx() },
                with(density) { (if (fromLeft) insetLeftDp else insetRightDp).dp.toPx() },
                settings.edgeZoneBandOnly, band.topPx, band.heightPx,
                corner?.let { GestureZoneBounds(it.left, it.top, it.right, it.bottom) })
        }
        val quick = quickLaunchBounds(width, height, settings.quickLaunchRegion).takeIf { settings.quickLaunchEnabled }
        Canvas(Modifier.fillMaxSize()) {
            // The vertical gesture starts anywhere on home; these are direction markers, not hit areas.
            drawRect(colors.primary.copy(alpha = .04f))
            edges.forEach { bounds ->
                val origin = Offset(bounds.left, bounds.top)
                val extent = Size(bounds.width, bounds.height)
                drawRect(colors.primary.copy(alpha = .22f), origin, extent)
                drawRect(colors.primary, origin, extent, style = Stroke(1.dp.toPx()))
                val top = maxOf(bounds.top, band.topPx)
                val bottom = minOf(bounds.bottom, band.bottomPx)
                if (bottom > top) drawRect(colors.primary.copy(alpha = .3f), Offset(bounds.left, top), Size(bounds.width, bottom - top))
            }
            quick?.let { bounds ->
                drawRect(colors.tertiary.copy(alpha = .14f), Offset(0f, bounds.top), Size(bounds.width, bounds.height))
                drawRect(colors.tertiary, Offset(0f, bounds.top), Size(bounds.width, bounds.height), style = Stroke(1.dp.toPx()))
            }
        }
        ZoneLabel(stringResource(R.string.gesture_zone_down), Modifier.offset(maxWidth * .35f, 56.dp))
        if (settings.swipeUpOpensAppList) {
            ZoneLabel(stringResource(R.string.gesture_zone_up), Modifier.offset(maxWidth * .35f, maxHeight * .62f))
        }
        edges.forEachIndexed { index, bounds ->
            val fromLeft = sides[index]
            val x = if (fromLeft) with(density) { bounds.right.toDp() } + 4.dp else maxWidth - 84.dp
            ZoneLabel(stringResource(R.string.gesture_zone_edge), Modifier.offset(x, with(density) { ((bounds.top + bounds.bottom) / 2).toDp() }))
        }
        quick?.let { bounds ->
            Column(Modifier.offset(y = with(density) { bounds.top.toDp() }).fillMaxWidth().padding(8.dp)) {
                ZoneLabel(stringResource(R.string.gesture_zone_quick))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    settings.quickLaunchLeft?.let { ZoneLabel(stringResource(R.string.gesture_zone_left, names[it.key] ?: it.label), Modifier.weight(1f)) }
                    settings.quickLaunchRight?.let { ZoneLabel(stringResource(R.string.gesture_zone_right, names[it.key] ?: it.label), Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun ZoneLabel(text: String, modifier: Modifier = Modifier) {
    Badge(modifier, containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
        Text(text, Modifier.padding(4.dp), style = MaterialTheme.typography.labelMedium)
    }
}
