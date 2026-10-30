// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.applist.rows

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.nikelauncher.data.HomeAlignment
import dev.nikelauncher.data.IconSide
import dev.nikelauncher.data.ShortcutSwipe

/** Smallest comfortable row, so a tap beside a small icon still lands on its app. */
internal val MIN_ROW_HEIGHT = 48.dp

/** A folder's apps sit in from it, so where the folder ends is plain. */
internal val FOLDER_INDENT = 24.dp

/** How the rows of the app list are drawn. */
@Immutable
internal class ListRowStyle(
    val contentColor: Color,
    val alignment: HomeAlignment,
    val iconSide: IconSide,
    val iconSizeDp: Int,
    val labelSizeSp: Int,
    val shortcutSwipe: ShortcutSwipe,
    /** False when the favorites are computed, so starring would write to a list nobody sees. */
    val favoritesEditable: Boolean,
)

internal fun HomeAlignment.rowArrangement() = when (this) {
    HomeAlignment.LEFT -> Arrangement.Start
    HomeAlignment.CENTER -> Arrangement.Center
    HomeAlignment.RIGHT -> Arrangement.End
}

internal fun HomeAlignment.textAlign() = when (this) {
    HomeAlignment.LEFT -> TextAlign.Start
    HomeAlignment.CENTER -> TextAlign.Center
    HomeAlignment.RIGHT -> TextAlign.End
}

internal fun HomeAlignment.contentAlignment() = when (this) {
    HomeAlignment.LEFT -> Alignment.CenterStart
    HomeAlignment.CENTER -> Alignment.Center
    HomeAlignment.RIGHT -> Alignment.CenterEnd
}
