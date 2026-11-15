// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.nikelauncher.data.EdgeSide
import dev.nikelauncher.ui.kit.motion.AppMotion
import kotlinx.coroutines.delay

private const val PREVIEW_MS = 1400L

/** True for [PREVIEW_MS] after every change of [tick], and never for the first value. */
@Composable
private fun flash(tick: Int): Boolean {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(tick) {
        if (tick == 0) return@LaunchedEffect
        shown = true
        delay(PREVIEW_MS)
        shown = false
    }
    return shown
}

/** How far the dim preview has faded in; the settings surface fades out by the same amount. */
@Composable
fun SettingsPreviews.dimReveal(): Float {
    val shown = flash(dimTick)
    val reveal by animateFloatAsState(if (shown) 1f else 0f, AppMotion.effects(), label = "dimReveal")
    return reveal
}

/** The dim scrim, painted where the wallpaper would be. */
@Composable
fun DimPreview(previews: SettingsPreviews, dimColor: Color, reveal: Float) {
    if (reveal <= 0f) return
    Box(Modifier.fillMaxSize().background(dimColor.copy(alpha = previews.dimAlpha * reveal)))
}

/** The edge zones and the line a letter lands on, drawn over the settings while they are adjusted. */
@Composable
fun PreviewOverlays(
    previews: SettingsPreviews,
    edgeSide: EdgeSide,
    edgeZoneWidthDp: Int,
    sectionTopPercent: Int,
    modifier: Modifier = Modifier,
) {
    val edgeAlpha by animateFloatAsState(if (flash(previews.edgeTick)) 1f else 0f, AppMotion.effects(), label = "edgePreview")
    val sectionAlpha by animateFloatAsState(if (flash(previews.sectionTick)) 1f else 0f, AppMotion.effects(), label = "sectionPreview")
    val primary = MaterialTheme.colorScheme.primary
    Box(modifier.fillMaxSize()) {
        if (sectionAlpha > 0f) {
            Box(Modifier.align(Alignment.TopStart).fillMaxWidth().fillMaxHeight(sectionTopPercent / 100f), Alignment.BottomStart) {
                Box(Modifier.fillMaxWidth().height(2.dp).background(primary.copy(alpha = 0.9f * sectionAlpha)))
            }
        }
        if (edgeAlpha > 0f) {
            val stripe = primary.copy(alpha = 0.35f * edgeAlpha)
            if (edgeSide != EdgeSide.RIGHT) {
                Box(Modifier.align(Alignment.CenterStart).width(edgeZoneWidthDp.dp).fillMaxHeight().background(stripe))
            }
            if (edgeSide != EdgeSide.LEFT) {
                Box(Modifier.align(Alignment.CenterEnd).width(edgeZoneWidthDp.dp).fillMaxHeight().background(stripe))
            }
        }
    }
}
