// SPDX-License-Identifier: GPL-3.0-or-later
/* Adapted from Orion Store, RookieEnough and contributors, GPL-3.0, by way of iris-android
 * (components/orion/OrionPrimitives.kt). Source: Orion e561e91, ui/Settings.kt (Page).
 */
package dev.nikelauncher.ui.kit.orion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon

/**
 * The tinted hero a page opens on: a large shape turning in the corner, the icon on a smaller one, the title in
 * display type. It scrolls away with the page; [top] holds what sits above the icon (the back button).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OrionPageHero(
    icon: ImageVector,
    title: String,
    desc: String,
    accent: Color,
    shape: RoundedPolygon,
    modifier: Modifier = Modifier,
    background: Color = MaterialTheme.colorScheme.surface,
    top: @Composable () -> Unit = {},
) {
    val (bg, fg) = orionTonal(accent)
    Box(modifier.fillMaxWidth().clipToBounds().background(Brush.verticalGradient(listOf(bg, background)))) {
        OrionSpinner(shape, fg.copy(.08f), 260.dp, Modifier.align(Alignment.TopEnd).offset(70.dp, (-30).dp), periodMs = 40_000)
        Column(Modifier.padding(bottom = 12.dp)) {
            top()
            Box(Modifier.padding(start = 24.dp, top = 8.dp).size(72.dp), contentAlignment = Alignment.Center) {
                OrionSpinner(shape, fg.copy(.16f), 72.dp, periodMs = 18_000)
                Icon(icon, null, Modifier.size(34.dp), tint = fg)
            }
            Text(
                title,
                Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp),
                style = MaterialTheme.typography.displaySmallEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (desc.isNotEmpty()) {
                Text(
                    desc,
                    Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
