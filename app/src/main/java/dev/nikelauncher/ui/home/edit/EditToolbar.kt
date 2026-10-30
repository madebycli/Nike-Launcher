// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.edit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Height
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
)

/**
 * Edit mode's controls: a Material floating toolbar at the bottom edge, so it never covers the
 * blocks being arranged, with Done as its action button.
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
            actions.onSmaller?.let {
                IconButton(onClick = it) { Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.settings_less)) }
            }
            actions.onLarger?.let {
                IconButton(onClick = it) { Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.settings_more)) }
            }
            actions.onBandRange?.let {
                IconButton(onClick = it) { Icon(Icons.Filled.Height, contentDescription = stringResource(R.string.handle_az_range)) }
            }
            actions.onReset?.let {
                IconButton(onClick = it) { Icon(Icons.Filled.RestartAlt, contentDescription = stringResource(R.string.edit_block_reset)) }
            }
        }
    }
}
