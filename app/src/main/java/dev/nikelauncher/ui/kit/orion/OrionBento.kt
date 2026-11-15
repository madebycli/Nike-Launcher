// SPDX-License-Identifier: GPL-3.0-or-later
/* Adapted from Orion Store, RookieEnough and contributors, GPL-3.0, by way of iris-android
 * (components/orion/OrionPrimitives.kt). Source: Orion e561e91, ui/Settings.kt (Bento, HubHeader).
 * Nike changes: the tile takes the theme's extra-large shape.
 */
package dev.nikelauncher.ui.kit.orion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.RoundedPolygon

/** Hub tile in the accent's tonal pair: a shape-backed icon, a large turning shape in the corner, title and description below. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OrionBento(
    title: String,
    desc: String,
    icon: ImageVector,
    accent: Color,
    shape: RoundedPolygon,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    sharedKey: String? = null,
) {
    val (bg, fg) = orionTonal(accent)
    val outline = MaterialTheme.shapes.extraLarge
    Box(modifier.orionSharedBounds(sharedKey).orionSpringClick(outline, onClick = onClick).background(bg)) {
        OrionSpinner(shape, fg.copy(.1f), if (compact) 120.dp else 168.dp, Modifier.align(Alignment.BottomEnd).offset(36.dp, 36.dp))
        CompositionLocalProvider(LocalContentColor provides fg) {
            Column(Modifier.fillMaxSize().padding(18.dp)) {
                Box(Modifier.size(44.dp).background(fg.copy(.14f), shape.toShape()), contentAlignment = Alignment.Center) {
                    Icon(icon, null, Modifier.size(24.dp))
                }
                Spacer(Modifier.weight(1f))
                // Long words shrink a little before they are cut.
                Text(
                    title,
                    style = MaterialTheme.typography.titleLargeEmphasized,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    autoSize = TextAutoSize.StepBased(minFontSize = 14.sp, maxFontSize = MaterialTheme.typography.titleLargeEmphasized.fontSize),
                )
                if (!compact && desc.isNotEmpty()) {
                    Text(desc, style = MaterialTheme.typography.bodySmall, color = fg.copy(.8f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/** Hub section header with an optional trailing action. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OrionHubHeader(title: String, modifier: Modifier = Modifier, action: @Composable () -> Unit = {}) =
    Row(modifier.fillMaxWidth().padding(start = 8.dp, top = 16.dp, bottom = 2.dp).heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmallEmphasized, color = MaterialTheme.colorScheme.primary)
        action()
    }
