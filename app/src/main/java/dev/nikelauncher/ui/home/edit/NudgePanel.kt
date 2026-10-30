// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.edit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.nikelauncher.R
import dev.nikelauncher.ui.kit.motion.AppMotion
import kotlinx.coroutines.delay

/** What the nudge panel shows and does for the selected block. */
internal class NudgeState(
    val offsetDp: Int,
    val edgeDp: Int,
    val onMove: (dxDp: Int, dyDp: Int) -> Unit,
    val onEdge: (deltaDp: Int) -> Unit,
)

private const val FIRST_REPEAT_MS = 350L
private const val REPEAT_MS = 45L

/**
 * Exact placement for the selected block, in a panel of its own: one dp a press, more while a
 * button is held. Dragging is for the rough place; this is for the last few dp, and for the
 * distance from the screen edge, which a drag cannot set.
 */
@Composable
internal fun NudgePanel(visible: Boolean, state: NudgeState?, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible && state != null,
        modifier = modifier,
        enter = slideInVertically(AppMotion.offset()) { it } + fadeIn(AppMotion.effects()),
        exit = slideOutVertically(AppMotion.offset()) { it } + fadeOut(AppMotion.effects()),
    ) {
        // Kept while it animates out, so the values do not blank before the panel is gone.
        val shown = remember { arrayOfNulls<NudgeState>(1) }
        state?.let { shown[0] = it }
        val s = shown[0] ?: return@AnimatedVisibility
        Surface(
            modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 3.dp,
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NudgeButton(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.edit_move_left)) { s.onMove(-1, 0) }
                    NudgeButton(Icons.Filled.ArrowUpward, stringResource(R.string.edit_move_up)) { s.onMove(0, -1) }
                    NudgeButton(Icons.Filled.ArrowDownward, stringResource(R.string.edit_move_down)) { s.onMove(0, 1) }
                    NudgeButton(Icons.AutoMirrored.Filled.ArrowForward, stringResource(R.string.edit_move_right)) { s.onMove(1, 0) }
                    Text(
                        stringResource(R.string.edit_value_dp, s.offsetDp),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.edit_edge_distance),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    RepeatingIconButton(Icons.Filled.Remove, stringResource(R.string.settings_less)) { s.onEdge(-1) }
                    Text(
                        stringResource(R.string.edit_value_dp, s.edgeDp),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    RepeatingIconButton(Icons.Filled.Add, stringResource(R.string.settings_more)) { s.onEdge(1) }
                }
            }
        }
    }
}

@Composable
private fun NudgeButton(icon: ImageVector, description: String, onStep: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    RepeatWhilePressed(source, onStep)
    FilledTonalIconButton(onClick = {}, interactionSource = source) { Icon(icon, contentDescription = description) }
}

@Composable
private fun RepeatingIconButton(icon: ImageVector, description: String, onStep: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    RepeatWhilePressed(source, onStep)
    IconButton(onClick = {}, interactionSource = source) { Icon(icon, contentDescription = description) }
}

/** One step when the press lands, then a run of them for as long as it is held. */
@Composable
private fun RepeatWhilePressed(source: MutableInteractionSource, onStep: () -> Unit) {
    val pressed by source.collectIsPressedAsState()
    val step by rememberUpdatedState(onStep)
    LaunchedEffect(pressed) {
        if (!pressed) return@LaunchedEffect
        step()
        delay(FIRST_REPEAT_MS)
        while (true) {
            step()
            delay(REPEAT_MS)
        }
    }
}
