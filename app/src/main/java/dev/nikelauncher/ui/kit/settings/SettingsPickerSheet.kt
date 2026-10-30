// SPDX-License-Identifier: GPL-3.0-or-later
// Adapted from AniSync (GPL-3.0-or-later), presentation/settings/components/SettingsPickerSheet.kt.
package dev.nikelauncher.ui.kit.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A single-choice bottom sheet: a heading and one radio row per option. The caller owns dismissal;
 * have [onSelect] apply the value and close the sheet.
 */
@Composable
fun <T> SettingsPickerSheet(
    title: String,
    items: List<T>,
    selected: T?,
    itemLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = modifier,
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            SettingsGroup(
                rows = items.map { item ->
                    settingsRadio(itemLabel(item), selected = item == selected, onClick = { onSelect(item) })
                },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}
