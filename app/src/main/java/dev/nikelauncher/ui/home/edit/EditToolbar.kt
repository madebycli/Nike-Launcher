// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.edit

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.nikelauncher.R
import dev.nikelauncher.ui.kit.motion.AppMotion

/** The actions edit mode offers for the selected block. Null means the block does not have it. */
internal class EditToolbarActions(
    val onDone: () -> Unit,
    val onReset: (() -> Unit)?,
    val onSmaller: (() -> Unit)?,
    val onLarger: (() -> Unit)?,
    val onBandRange: (() -> Unit)?,
    val onSettings: () -> Unit,
    val onSelectAll: () -> Unit,
    val onAddWidget: (() -> Unit)?,
)

/**
 * Edit mode's controls: a Material floating toolbar docked at the bottom, with Done as its action.
 * It overlays the canvas without changing the measured fields.
 */
@Composable
internal fun EditToolbar(visible: Boolean, actions: EditToolbarActions, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(AppMotion.offset()) { it } + fadeIn(AppMotion.effects()),
        exit = slideOutVertically(AppMotion.offset()) { it } + fadeOut(AppMotion.effects()),
    ) {
        HorizontalFloatingToolbar(
            expanded = true,
            modifier = Modifier.navigationBarsPadding().padding(bottom = 16.dp),
            floatingActionButton = {
                FloatingToolbarDefaults.VibrantFloatingActionButton(onClick = actions.onDone) {
                    Icon(Icons.Filled.Done, contentDescription = stringResource(R.string.action_done))
                }
            },
        ) {
            ToolbarAction(Icons.Filled.Tune, stringResource(R.string.edit_layout_settings), actions.onSettings)
            ToolbarAction(Icons.Filled.SelectAll, stringResource(R.string.edit_select_all), actions.onSelectAll)
            actions.onAddWidget?.let { ToolbarAction(Icons.Filled.Add, stringResource(R.string.widget_add), it) }
            actions.onSmaller?.let { ToolbarAction(Icons.Filled.ZoomOut, stringResource(R.string.settings_less), it) }
            actions.onLarger?.let { ToolbarAction(Icons.Filled.ZoomIn, stringResource(R.string.settings_more), it) }
            actions.onBandRange?.let { ToolbarAction(Icons.Filled.Height, stringResource(R.string.handle_az_range), it) }
            actions.onReset?.let { ToolbarAction(Icons.Filled.RestartAlt, stringResource(R.string.edit_block_reset), it) }
        }
    }
}

/** An icon action whose name shows on a long press, the platform's way to name an icon without a caption. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolbarAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(label) } },
        state = rememberTooltipState(),
    ) {
        IconButton(onClick = onClick) { Icon(icon, contentDescription = label) }
    }
}
