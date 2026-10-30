// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.rows

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.nikelauncher.data.HomeAlignment
import dev.nikelauncher.data.IconSide
import dev.nikelauncher.ui.common.LocalIconConfig

/**
 * Lays an icon and its label against whichever edge the user picked.
 *
 * Left and right hang the label off a weight so it fills the row and pushes the icon to the far
 * side. Centring cannot: a weighted label would still span the row and leave the pair pinned
 * apart, so nothing is weighted and the row's own Center arrangement packs them together.
 */
@Composable
internal fun RowScope.AlignedIconLabel(
    alignment: HomeAlignment,
    iconSide: IconSide,
    showLabel: Boolean,
    /** Width of [icon], so a centred label can be balanced against it. */
    iconWidth: Dp,
    icon: @Composable () -> Unit,
    label: @Composable RowScope.(Modifier) -> Unit,
) {
    val showIcons = LocalIconConfig.current.showIcons
    // No icon drawn means no gap to leave for one.
    val gap = if (showIcons) 16.dp else 0.dp
    val centered = alignment == HomeAlignment.CENTER
    // fill = false keeps the icon beside its label: a plain weight would make the label span the
    // whole row and pin the icon to the opposite edge. The label takes only the width it needs,
    // still yielding when a long name would overflow.
    val labelModifier = Modifier.weight(1f, fill = false)
    // Centring measures the pair, so the text would sit off the middle by half an icon; balancing
    // the icon on the label's far side puts the text itself on the centre line.
    val balance: @Composable () -> Unit = {
        if (centered && showLabel && showIcons) Spacer(Modifier.width(iconWidth + gap))
    }

    if (iconSide == IconSide.RIGHT) {
        balance()
        if (showLabel) {
            label(labelModifier)
            Spacer(Modifier.width(gap))
        }
        icon()
    } else {
        icon()
        if (showLabel) {
            Spacer(Modifier.width(gap))
            label(labelModifier)
        }
        balance()
    }
}
