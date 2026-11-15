// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.corner

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.nikelauncher.R
import dev.nikelauncher.data.CornerAction
import dev.nikelauncher.ui.kit.settings.SettingsGroup
import dev.nikelauncher.ui.kit.settings.item
import kotlinx.coroutines.launch

/** What a tap on the button offers when it is set to open the menu. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CornerActionSheet(
    appLabel: String?,
    onDismiss: () -> Unit,
    onAction: (CornerAction) -> Unit,
    onConfigure: () -> Unit,
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    fun closeThen(action: () -> Unit) {
        scope.launch {
            state.hide()
            onDismiss()
            action()
        }
    }
    val actions = listOf(CornerAction.SEARCH, CornerAction.CALENDAR, CornerAction.WEATHER, CornerAction.NOTIFICATIONS) +
        (if (appLabel != null) listOf(CornerAction.APP, CornerAction.SHORTCUTS) else emptyList()) +
        CornerAction.SETTINGS
    val titles = actions.map { if (it == CornerAction.APP) appLabel.orEmpty() else stringResource(it.label) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            SettingsGroup {
                actions.forEachIndexed { index, action ->
                    item(titles[index], { closeThen { onAction(action) } }, icon = action.icon)
                }
            }
            TextButton(onClick = { closeThen(onConfigure) }) { Text(stringResource(R.string.corner_configure)) }
        }
    }
}
