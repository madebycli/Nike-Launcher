// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.rows

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import dev.nikelauncher.data.HomeAlignment
import dev.nikelauncher.data.IconSide
import dev.nikelauncher.data.ShortcutSwipe

/** How a favorites row is drawn, taken from the settings once so rows do not each take ten parameters. */
@Immutable
class RowStyle(
    val iconSizeDp: Int,
    val labelSizeSp: Int,
    val sidePaddingDp: Int,
    val contentColor: Color,
    val showLabels: Boolean,
    val alignment: HomeAlignment,
    val iconSide: IconSide,
    /** Whether only the icon and name answer a tap, rather than the whole line. */
    val compactTapArea: Boolean,
    val shortcutSwipe: ShortcutSwipe,
)
