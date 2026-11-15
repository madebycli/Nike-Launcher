// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.edit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.nikelauncher.R
import dev.nikelauncher.data.HomeBlock
import dev.nikelauncher.ui.home.HomeSettings
import dev.nikelauncher.ui.kit.settings.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeLayoutSheet(settings: HomeSettings, available: Set<HomeBlock>, selected: Set<HomeBlock>,
    edge: (HomeBlock) -> Int, onToggle: (HomeBlock) -> Unit, onGap: (Int) -> Unit,
    onRows: (Int) -> Unit, onEdge: (Int) -> Unit, onDismiss: () -> Unit) {
    val targets = selected.ifEmpty { available }
    val edgeValue = targets.map(edge).distinct().singleOrNull()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.edit_layout_settings), Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("home-layout-close")) { Icon(Icons.Filled.Done, stringResource(R.string.action_done)) }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                available.forEach { block ->
                    FilterChip(selected = block in selected, onClick = { onToggle(block) }, label = { Text(stringResource(block.label())) })
                }
            }
            SettingsGroup(buildList {
                if (targets.size > 1) add(settingsBlock(stringResource(R.string.edit_block_spacing), value = stringResource(R.string.edit_value_dp, settings.blockGapDp)) {
                    SettingsSlider(settings.blockGapDp.toFloat(), 0f..64f, { onGap(it.toInt()) })
                })
                add(settingsBlock(stringResource(R.string.settings_favorite_spacing), value = stringResource(R.string.edit_value_dp, settings.itemSpacingDp)) {
                    SettingsSlider(settings.itemSpacingDp.toFloat(), 0f..48f, { onRows(it.toInt()) })
                })
                add(settingsBlock(stringResource(R.string.edit_edge_distance), value = edgeValue?.let { stringResource(R.string.edit_value_dp, it) } ?: stringResource(R.string.edit_mixed)) {
                    SettingsSlider((edgeValue ?: targets.minOf(edge)).toFloat(), 0f..120f, { onEdge(it.toInt()) })
                })
            })
        }
    }
}
