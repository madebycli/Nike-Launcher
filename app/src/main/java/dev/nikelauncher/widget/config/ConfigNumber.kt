// SPDX-License-Identifier: GPL-3.0-or-later
// Adapted from Victoria Grid (GPL-3.0-or-later), ui/settings/PreferenceRows.kt (PreferenceNumber).
package dev.nikelauncher.widget.config

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.then
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.nikelauncher.R
import dev.nikelauncher.ui.kit.settings.SettingsRow
import dev.nikelauncher.ui.kit.settings.SettingsSlider
import dev.nikelauncher.ui.kit.settings.settingsBlock

/**
 * A number with a slider and step buttons, and its value as a button that opens an exact entry, so
 * both the rough and the precise way of setting it are one tap away.
 */
@Composable
internal fun configNumber(
    title: String,
    value: Int,
    range: IntRange,
    valueLabel: String = value.toString(),
    onChange: (Int) -> Unit,
): SettingsRow = settingsBlock(title) {
    var editing by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        androidx.compose.foundation.layout.Box(Modifier.weight(1f)) {
            SettingsSlider(
                value.toFloat().coerceIn(range.first.toFloat(), range.last.toFloat()),
                range.first.toFloat()..range.last.toFloat(),
                { v -> onChange(v.toInt().coerceIn(range)) },
            )
        }
        FilledTonalButton(onClick = { editing = true }, modifier = Modifier.heightIn(min = 48.dp)) { Text(valueLabel) }
    }
    if (editing) ExactNumberDialog(title, value, range, onChange) { editing = false }
}

@Composable
private fun ExactNumberDialog(title: String, value: Int, range: IntRange, onChange: (Int) -> Unit, onDismiss: () -> Unit) {
    val input = rememberTextFieldState(value.toString())
    val number = input.text.toString().toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                state = input,
                inputTransformation = InputTransformation.maxLength(4).then {
                    if (asCharSequence().any { !it.isDigit() }) revertAllChanges()
                },
                lineLimits = TextFieldLineLimits.SingleLine,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText = { Text(stringResource(R.string.widget_settings_range, range.first, range.last)) },
                isError = number == null || number !in range,
                modifier = Modifier.padding(top = 4.dp),
            )
        },
        confirmButton = {
            TextButton(onClick = { number?.let(onChange); onDismiss() }, enabled = number != null && number in range) {
                Text(stringResource(R.string.widget_settings_done))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
