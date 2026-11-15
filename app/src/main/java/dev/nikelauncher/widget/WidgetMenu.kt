// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.widget

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import dev.nikelauncher.R
import dev.nikelauncher.ui.kit.menu.TouchMenu

/** The shared expressive menu gives widget actions the same grouped shapes and motion as app rows. */
@Composable
internal fun WidgetMenu(expanded: Boolean, offset: DpOffset, id: Int, actions: WidgetSlotActions,
    onEditLayout: () -> Unit, onDismiss: () -> Unit) {
    TouchMenu(expanded, offset, onDismiss) {
        if (id > 0) {
            item(stringResource(R.string.action_app_info), { onDismiss(); actions.onAppInfo(id) }, Icons.Filled.Info)
            item(stringResource(R.string.action_edit_layout), { onDismiss(); onEditLayout() }, Icons.Filled.Edit)
            item(stringResource(R.string.widget_change_settings), { onDismiss(); actions.onWidgetSettings(id) }, Icons.Filled.Tune)
            gap()
            item(stringResource(R.string.widget_add_another), { onDismiss(); actions.onAddWidget() }, Icons.Filled.Add)
            item(stringResource(R.string.widget_remove_this), { onDismiss(); actions.onRemoveWidget(id) }, Icons.Filled.Delete, destructive = true)
        } else {
            item(stringResource(R.string.widget_add), { onDismiss(); actions.onAddWidget() }, Icons.Filled.Add)
            item(stringResource(R.string.action_edit_layout), { onDismiss(); onEditLayout() }, Icons.Filled.Edit)
        }
        gap()
        item(stringResource(R.string.action_open_settings), { onDismiss(); actions.onOpenSettings() }, Icons.Filled.Settings)
    }
}
