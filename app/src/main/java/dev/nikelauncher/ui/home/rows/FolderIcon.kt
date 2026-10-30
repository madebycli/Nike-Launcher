// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.rows

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.nikelauncher.data.AppInfo
import dev.nikelauncher.ui.common.AppIcon
import dev.nikelauncher.ui.common.FolderIconImage
import dev.nikelauncher.ui.common.LocalIconConfig

/** A folder's icon: its own picture when it has one, else a 2×2 preview of its first apps. */
@Composable
internal fun FolderIcon(members: List<AppInfo>, sizeDp: Int, contentColor: Color, iconOverride: String?) {
    if (!LocalIconConfig.current.showIcons) return
    if (iconOverride != null && FolderIconImage(iconOverride, sizeDp)) return

    val preview = members.take(4)
    Surface(
        modifier = Modifier.size(sizeDp.dp),
        shape = MaterialTheme.shapes.medium,
        // Enough of a container to read as one thing over a busy wallpaper.
        color = contentColor.copy(alpha = 0.22f),
    ) {
        Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            if (preview.isEmpty()) {
                Icon(
                    Icons.Filled.Folder,
                    contentDescription = null,
                    tint = contentColor.copy(alpha = 0.7f),
                    modifier = Modifier.size((sizeDp * 0.55f).dp),
                )
            } else {
                // Sized to fill the container so the preview does not read as a smudge beside the
                // solid icons it shares a column with.
                val cell = (sizeDp * 0.44f).toInt()
                preview.chunked(2).forEach { rowApps ->
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        rowApps.forEach { AppIcon(app = it, sizeDp = cell) }
                    }
                }
            }
        }
    }
}
