// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.nikelauncher.R
import dev.nikelauncher.data.AppInfo
import kotlinx.coroutines.launch

/**
 * Rename an app or swap its icon, in a sheet. Shared by the home screen and the A-Z list.
 *
 * The card shows the entry as the list will draw it, and updates as you type; tapping its icon
 * goes to the icon picker. Every way out lets the sheet slide away first, so the next screen
 * follows it instead of the sheet vanishing from under the finger.
 */
@Composable
fun EditAppDialog(
    currentName: String,
    onConfirmName: (String?) -> Unit,
    /** Receives whatever is typed, because picking an icon navigates away from this sheet. */
    onChangeIcon: (String) -> Unit,
    onDismiss: () -> Unit,
    app: AppInfo? = null,
) {
    var text by rememberSaveable(app?.key, currentName) { mutableStateOf(currentName) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var leaving by remember { mutableStateOf(false) }
    fun closeThen(action: () -> Unit) {
        if (leaving) return
        leaving = true
        scope.launch {
            try {
                sheetState.hide()
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                leaving = false
                throw cancelled
            }
            action()
        }
    }
    ModalBottomSheet(
        onDismissRequest = { if (!leaving) onDismiss() },
        sheetState = sheetState,
    ) {
        Column(Modifier.fillMaxWidth().heightIn(max = (LocalConfiguration.current.screenHeightDp * .83f).dp)) {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.app_edit_title),
                    Modifier.weight(1f).semantics { heading() },
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                IconButton(onClick = { closeThen(onDismiss) }) {
                    Icon(Icons.Filled.Close, stringResource(R.string.action_cancel))
                }
            }
            Column(
                Modifier.weight(1f, fill = false).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        val iconLabel = stringResource(R.string.action_change_icon)
                        Box(
                            Modifier.size(56.dp)
                                .clickable(onClickLabel = iconLabel) { closeThen { onChangeIcon(text) } }
                                .semantics { contentDescription = iconLabel },
                            contentAlignment = Alignment.Center,
                        ) {
                            // The icon is the point of this card, so it shows even when the list hides icons.
                            CompositionLocalProvider(
                                LocalIconConfig provides LocalIconConfig.current.copy(showIcons = true, notificationBadges = false),
                            ) {
                                if (app != null) AppIcon(app, 44) else Icon(Icons.Filled.Apps, null, Modifier.size(44.dp))
                            }
                        }
                        val nameLabel = stringResource(R.string.home_display_name_label)
                        TextField(
                            value = text,
                            onValueChange = { text = it },
                            modifier = Modifier.weight(1f).semantics { contentDescription = nameLabel },
                            textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            singleLine = true,
                            placeholder = { Text(app?.label ?: currentName) },
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { closeThen { onConfirmName(text) } }),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                            ),
                        )
                    }
                }
                TextButton(onClick = { closeThen { onConfirmName(null) } }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Icon(Icons.Filled.RestartAlt, null, Modifier.size(20.dp))
                    Text(stringResource(R.string.app_edit_reset_name), Modifier.padding(start = 8.dp))
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(onClick = { closeThen { onConfirmName(text) } }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }
}
