// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.rows

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import dev.nikelauncher.R
import dev.nikelauncher.data.FolderWidget
import dev.nikelauncher.ui.kit.menu.TouchMenu
import dev.nikelauncher.ui.kit.settings.SettingsSlider
import dev.nikelauncher.widget.WidgetPage
import dev.nikelauncher.widget.WidgetSlotActions

/**
 * One widget inside an open folder, with its own menu. The widget is the same hosted view the
 * home screen's widget area draws, so a widget that fails is contained the same way.
 */
@Composable
internal fun FolderWidgetBlock(
    folderId: String,
    widget: FolderWidget,
    actions: WidgetSlotActions,
    controlsInset: Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var menuOffset by remember { mutableStateOf(DpOffset.Zero) }
    var resizing by remember { mutableStateOf(false) }
    // Followed while the slider moves, and stored once the change is done.
    var liveHeight by remember(widget.heightDp) { mutableIntStateOf(widget.heightDp) }
    val density = LocalDensity.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        Box(Modifier.fillMaxWidth().height(liveHeight.dp)) {
            WidgetPage(widget.id) { x, y ->
                menuOffset = with(density) { DpOffset(x.toDp(), y.toDp()) }
                menuExpanded = true
            }
            TouchMenu(expanded = menuExpanded, offset = menuOffset, onDismissRequest = { menuExpanded = false }) {
                item(stringResource(R.string.handle_widget_height), { menuExpanded = false; resizing = true }, leadingIcon = Icons.Filled.Height)
                item(stringResource(R.string.widget_change_settings), { menuExpanded = false; actions.onWidgetSettings(widget.id) }, leadingIcon = Icons.Filled.Tune)
                item(stringResource(R.string.action_app_info), { menuExpanded = false; actions.onAppInfo(widget.id) }, leadingIcon = Icons.Filled.Info)
                item(stringResource(R.string.widget_remove_this), { menuExpanded = false; actions.onRemoveFolderWidget(folderId, widget.id) }, leadingIcon = Icons.Filled.Delete, destructive = true)
            }
        }
        if (resizing) {
            Row(modifier = controlsInset, verticalAlignment = Alignment.CenterVertically) {
                SettingsSlider(liveHeight.toFloat(), 80f..900f, { liveHeight = it.toInt() }, Modifier.weight(1f))
                TextButton(onClick = {
                    resizing = false
                    if (liveHeight != widget.heightDp) actions.onResizeFolderWidget(folderId, widget.id, liveHeight)
                }) { Text(stringResource(R.string.action_done)) }
            }
        }
    }
}
