// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.kit.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.nikelauncher.R

/**
 * A Material slider with a step button either side, for the values where dragging to an exact
 * number is fiddly. Put it in a [settingsBlock]; the block's value line shows the current number.
 */
@Composable
fun SettingsSlider(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    step: Float = 1f,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = { onValueChange((value - step).coerceIn(range)) },
            enabled = value > range.start,
        ) {
            Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.settings_less))
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = { onValueChange((value + step).coerceIn(range)) },
            enabled = value < range.endInclusive,
        ) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.settings_more))
        }
    }
}
