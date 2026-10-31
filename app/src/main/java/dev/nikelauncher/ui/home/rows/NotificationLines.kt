// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.rows

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import dev.nikelauncher.data.HomeAlignment
import dev.nikelauncher.media.NotificationPreview
import dev.nikelauncher.ui.theme.WidgetTextFamily

/** Folder message excerpt; the app row owns all interaction, never a dismissible inline line. */
@Composable
internal fun NotificationLines(preview: NotificationPreview?, alignment: HomeAlignment, color: Color, labelSizeSp: Int) {
    if (preview != null) Text(preview.messages.lastOrNull()?.text ?: preview.text,
        color = color, fontFamily = WidgetTextFamily, fontSize = (labelSizeSp * 14f / 16f).sp,
        maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = alignment.textAlign())
}
