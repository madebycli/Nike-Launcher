// SPDX-License-Identifier: GPL-3.0-or-later
// Adapted from AniSync (GPL-3.0-or-later), presentation/components/menu/Menu.kt.
package dev.nikelauncher.ui.kit.menu

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorPosition
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

/**
 * Material 3 Expressive dropdown menu. Builds on `DropdownMenuPopup` /
 * `DropdownMenuGroup` / `DropdownMenuItem` so items get segmented shapes, group
 * container colors, partial-width dividers, and refined motion automatically.
 *
 * Author content with the [MenuScope] DSL: [MenuScope.item],
 * [MenuScope.divider], [MenuScope.gap]. `gap()` splits subsequent items
 * into a new visual group with 8dp of empty space between groups.
 *
 * Destructive items use the error color triplet on text + icon; selected items
 * pick up the secondaryContainer highlight from the standard expressive tokens.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Menu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    content: @Composable MenuScope.() -> Unit
) {
    val scope = MenuScopeImpl()
    scope.content()
    val groups = scope.groups()
    if (groups.isEmpty()) return

    DropdownMenuPopup(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        popupPositionProvider = MenuDefaults.rememberDropdownMenuPopupPositionProvider(
            MenuAnchorPosition.Below,
            offset
        )
    ) {
        MenuGroups(groups, scope.scrollingGroup())
    }
}

/**
 * Text-field-anchored variant of [Menu]. Selecting an option flips the
 * anchor's text. Items are authored via the same [MenuScope] DSL.
 *
 * Form-field menus skip [DropdownMenuGroup] segmentation since the text-field
 * anchor implies a single grouped surface; gaps render as empty padding rather
 * than separate Surface groups.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MenuField(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    selectedLabel: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    fieldSupportingText: String? = null,
    enabled: Boolean = true,
    content: @Composable MenuScope.() -> Unit
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) onExpandedChange(it) },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = label?.let { { Text(it) } },
            supportingText = fieldSupportingText?.let { supporting ->
                {
                    Text(
                        text = supporting,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled)
        )

        val scope = MenuScopeImpl()
        scope.content()
        val groups = scope.groups()
        if (groups.isEmpty()) return@ExposedDropdownMenuBox

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            // ExposedDropdownMenu owns its surface, so groups are flattened into one column with
            // spacers between them instead of nested surfaces.
            groups.forEachIndexed { index, group ->
                MenuRows(group)
                if (index < groups.size - 1) Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ColumnScope.MenuGroups(groups: List<List<MenuEntry>>, scrolling: ScrollingGroup?) {
    val groupCount = groups.size
    groups.forEachIndexed { gIndex, group ->
        val shapes = MenuDefaults.groupShape(gIndex, groupCount)
        if (scrolling != null && group === scrolling.entries) {
            // The scroll sits inside the group's own surface, as in Material's scrolling menu. Around
            // the surfaces it would clip their shadows flat at its top and bottom edges.
            Box(Modifier.weight(1f, fill = false)) {
                DropdownMenuGroup(shapes = shapes, containerColor = MenuDefaults.groupStandardContainerColor) {
                    ScrollingRows(scrolling.maxVisibleItems) { MenuRows(group) }
                }
            }
        } else {
            DropdownMenuGroup(shapes = shapes, containerColor = MenuDefaults.groupStandardContainerColor) {
                MenuRows(group)
            }
        }
        if (gIndex < groupCount - 1) Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun MenuRows(group: List<MenuEntry>) {
    val itemCount = group.count { it is MenuEntry.Item }
    var itemIndex = 0
    group.forEach { entry ->
        when (entry) {
            is MenuEntry.Item -> {
                MenuItemRow(entry, itemIndex, itemCount)
                itemIndex++
            }
            MenuEntry.Divider -> HorizontalDivider(
                modifier = Modifier.padding(MenuDefaults.HorizontalDividerPadding)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MenuItemRow(entry: MenuEntry.Item, index: Int, count: Int) {
    val errorColor = MaterialTheme.colorScheme.error
    val colors = if (entry.destructive) {
        MenuDefaults.selectableItemColors(
            textColor = errorColor,
            leadingIconColor = errorColor,
            trailingContentColor = errorColor
        )
    } else {
        MenuDefaults.selectableItemColors()
    }
    SelectableDropdownMenuItem(
        selected = entry.selected,
        onClick = entry.onClick,
        text = { Text(entry.text, maxLines = entry.maxLines, overflow = TextOverflow.Ellipsis) },
        shapes = MenuDefaults.itemShape(index, count),
        leadingIcon = when {
            entry.leadingIcon != null -> {
                { Icon(imageVector = entry.leadingIcon, contentDescription = null) }
            }
            entry.leadingImage != null -> {
                { Image(bitmap = entry.leadingImage, contentDescription = null, modifier = Modifier.size(24.dp)) }
            }
            else -> null
        },
        trailingContent = entry.trailingLabel?.let { trailing ->
            {
                Text(
                    text = trailing,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        enabled = entry.enabled,
        colors = colors,
        supportingText = entry.supportingText?.let { supporting ->
            { Text(supporting, maxLines = entry.maxLines, overflow = TextOverflow.Ellipsis) }
        }
    )
}
