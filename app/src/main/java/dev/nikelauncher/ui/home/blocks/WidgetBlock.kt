// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.blocks

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.nikelauncher.widget.WidgetSlot
import dev.nikelauncher.widget.WidgetSlotActions

/**
 * The home widgets. A widget lays out its own contents and most clocks centre theirs, which nothing
 * out here can reach inside, so narrowing the slot and moving it sideways moves what it draws.
 */
@Composable
internal fun WidgetBlock(
    widgetIds: List<Int>,
    heightDp: Int,
    stacked: Boolean,
    sidePaddingDp: Int,
    offsetXDp: Int,
    homeIntentTick: Int,
    actions: WidgetSlotActions,
    onEditLayout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WidgetSlot(
        widgetIds = widgetIds,
        heightDp = heightDp,
        stacked = stacked,
        onEditLayout = onEditLayout,
        actions = actions,
        homeIntentTick = homeIntentTick,
        modifier = modifier
            .offset(x = offsetXDp.dp)
            .fillMaxWidth()
            .padding(horizontal = sidePaddingDp.dp),
    )
}
