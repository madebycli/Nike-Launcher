// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.kit.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow

/**
 * A group of settings rows drawn with Material 3 Expressive's [SegmentedListItem]: the first and
 * last rows get the outer corners, the ones between are tightly separated by the stock gap.
 *
 * Titles and values wrap, so a translation that runs 40 % longer grows its row instead of being cut.
 */
@Composable
fun SettingsGroup(
    modifier: Modifier = Modifier,
    content: SettingsGroupScope.() -> Unit,
) {
    val rows = SettingsGroupScopeImpl().apply(content).rows
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        rows.forEachIndexed { index, row ->
            SettingsRowContent(row, index, rows.size)
        }
    }
}

@Composable
private fun SettingsRowContent(row: SettingsRow, index: Int, count: Int) {
    val shapes = ListItemDefaults.segmentedShapes(index, count)
    val colors = ListItemDefaults.segmentedColors()
    when (row) {
        is SettingsRow.Item -> SegmentedListItem(
            onClick = row.onClick,
            shapes = shapes,
            colors = colors,
            enabled = row.enabled,
            leadingContent = row.icon?.let { icon -> { Icon(icon, contentDescription = null) } },
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
            leadingContent = row.icon?.let { icon -> { Icon(icon, contentDescription = null) } },
            supportingContent = row.value?.let { value -> { Text(value) } },
            trailingContent = {
                Switch(checked = row.checked, onCheckedChange = null, enabled = row.enabled)
            },
            content = { Text(row.title) },
        )

        is SettingsRow.Block -> SegmentedListItem(
            shapes = shapes,
            colors = colors,
            leadingContent = row.icon?.let { icon -> { Icon(icon, contentDescription = null) } },
            supportingContent = {
                Column {
                    row.value?.let { Text(it, overflow = TextOverflow.Visible) }
                    row.control()
                }
            },
            content = { Text(row.title) },
        )
    }
}
