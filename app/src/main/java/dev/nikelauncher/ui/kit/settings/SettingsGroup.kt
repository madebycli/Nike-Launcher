// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.kit.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectableGroup
import dev.nikelauncher.ui.kit.onboarding.IconBlob
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A group of settings rows drawn with Material 3 Expressive's [SegmentedListItem]: the first and
 * last rows get the outer corners, the ones between are tightly separated by the stock gap.
 *
 * Titles and values wrap, so a translation that runs 40 % longer grows its row instead of being cut.
 */
@Composable
fun SettingsGroup(rows: List<SettingsRow>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        rows.forEachIndexed { index, row -> SettingsRowContent(row, index, rows.size) }
    }
}

/** The rows of a [SettingsGroup] written in place, for groups that are not searched. */
@Composable
fun SettingsGroup(modifier: Modifier = Modifier, content: SettingsRowCollector.() -> Unit) {
    SettingsGroup(SettingsRowCollector().apply(content).rows, modifier)
}

class SettingsRowCollector internal constructor() {
    internal val rows = mutableListOf<SettingsRow>()

    fun add(row: SettingsRow) {
        rows += row
    }
}

@Composable
private fun SettingsRowContent(row: SettingsRow, index: Int, count: Int) {
    val shapes = ListItemDefaults.segmentedShapes(index, count)
    // One plate colour for every kind of row. Left to themselves, a switch row that is on is
    // drawn in the secondary container and a plain row in the page colour, which makes a group
    // read as unrelated slabs.
    val plate = MaterialTheme.colorScheme.surfaceContainer
    val colors = ListItemDefaults.segmentedColors(
        containerColor = plate,
        selectedContainerColor = plate,
        contentColor = MaterialTheme.colorScheme.onSurface,
        selectedContentColor = MaterialTheme.colorScheme.onSurface,
        supportingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        selectedSupportingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    when (row) {
        is SettingsRow.Item -> SegmentedListItem(
            onClick = row.onClick,
            shapes = shapes,
            colors = colors,
            enabled = row.enabled,
            leadingContent = row.icon?.let { icon -> { IconBlob(icon) } },
            supportingContent = row.value?.let { value -> { Text(value) } },
            trailingContent = row.trailing,
            content = { Text(row.title) },
        )

        is SettingsRow.Switch -> SegmentedListItem(
            checked = row.checked,
            onCheckedChange = row.onCheckedChange,
            shapes = shapes,
            colors = colors,
            enabled = row.enabled,
            leadingContent = row.icon?.let { icon -> { IconBlob(icon) } },
            supportingContent = row.value?.let { value -> { Text(value) } },
            trailingContent = {
                Switch(checked = row.checked, onCheckedChange = null, enabled = row.enabled)
            },
            content = { Text(row.title) },
        )

        is SettingsRow.Radio -> SegmentedListItem(
            selected = row.selected,
            onClick = row.onClick,
            shapes = shapes,
            colors = colors,
            supportingContent = row.value?.let { value -> { Text(value) } },
            trailingContent = { RadioButton(selected = row.selected, onClick = null) },
            content = { Text(row.title) },
        )

        is SettingsRow.Block -> SegmentedListItem(
            shapes = shapes,
            colors = colors,
            leadingContent = row.icon?.let { icon -> { IconBlob(icon) } },
            supportingContent = {
                Column {
                    row.value?.let { Text(it) }
                    row.control()
                }
            },
            content = { Text(row.title) },
        )
    }
}
