// SPDX-License-Identifier: GPL-3.0-or-later
// Adapted from AniSync (GPL-3.0-or-later), presentation/settings/components/ColorPickerSheet.kt.
package dev.nikelauncher.ui.kit.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import dev.nikelauncher.R

/** Colours offered beside the wallpaper's own, enough to pick a text colour without typing a hex. */
private val Swatches = listOf(
    0xFFFFFFFF, 0xFFE3E8EC, 0xFF9AA5B1, 0xFF10161C,
    0xFFEF5350, 0xFFFFA726, 0xFFFFEE58, 0xFF66BB6A,
    0xFF26C6DA, 0xFF42A5F5, 0xFF7E57C2, 0xFFEC407A,
).map { Color(it) }

/**
 * Picks an opaque colour by swatch, hue or hex code. [wallpaperColors] are offered first: a colour
 * that already belongs to the wallpaper is most of what anyone wants here.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPickerSheet(
    initial: Color,
    wallpaperColors: List<Color>,
    onPick: (Color) -> Unit,
    onDismiss: () -> Unit,
) {
    var argb by rememberSaveable { mutableIntStateOf(initial.toArgb()) }
    var hue by rememberSaveable { mutableFloatStateOf(hueOf(initial)) }
    var hex by rememberSaveable { mutableStateOf(initial.toHex()) }
    val selected = Color(argb)

    fun choose(color: Color) {
        argb = color.toArgb()
        hue = hueOf(color)
        hex = color.toHex()
    }

    val rainbow = remember {
        Brush.horizontalGradient((0..360 step 30).map { Color.hsl(it.toFloat(), 0.7f, 0.5f) })
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp).padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.text_color_pick), style = MaterialTheme.typography.headlineSmall)

            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                (wallpaperColors + Swatches).forEach { color ->
                    Swatch(color, selected = color.toArgb() == argb) { choose(color) }
                }
            }

            Box(Modifier.fillMaxWidth().height(40.dp).clip(MaterialTheme.shapes.extraLarge).background(rainbow)) {
                Slider(
                    value = hue,
                    onValueChange = {
                        val color = Color.hsl(it, 0.7f, 0.5f)
                        argb = color.toArgb()
                        hex = color.toHex()
                        hue = it
                    },
                    valueRange = 0f..360f,
                    colors = SliderDefaults.colors(
                        thumbColor = selected,
                        activeTrackColor = Color.Transparent,
                        inactiveTrackColor = Color.Transparent,
                    ),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).align(Alignment.Center),
                )
            }

            TextField(
                value = hex,
                onValueChange = { text ->
                    hex = text.take(7)
                    parseHex(hex)?.let { argb = it.toArgb(); hue = hueOf(it) }
                },
                singleLine = true,
                isError = parseHex(hex) == null,
                label = { Text(stringResource(R.string.color_hex)) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
                Button(onClick = { onPick(selected); onDismiss() }, enabled = parseHex(hex) != null) {
                    Text(stringResource(R.string.action_done))
                }
            }
        }
    }
}

@Composable
private fun Swatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = color,
        border = androidx.compose.foundation.BorderStroke(
            width = if (selected) 3.dp else 1.dp,
            color = MaterialTheme.colorScheme.outline,
        ),
        modifier = Modifier.size(40.dp),
    ) {}
}

private fun Color.toHex(): String = "#%06X".format(toArgb() and 0xFFFFFF)

private fun parseHex(text: String): Color? {
    val digits = text.trim().removePrefix("#")
    if (digits.length != 6) return null
    return digits.toLongOrNull(16)?.let { Color(0xFF000000 or it) }
}

private fun hueOf(color: Color): Float {
    val max = maxOf(color.red, color.green, color.blue)
    val delta = max - minOf(color.red, color.green, color.blue)
    if (delta == 0f) return 0f
    val hue = when (max) {
        color.red -> 60f * (((color.green - color.blue) / delta) % 6f)
        color.green -> 60f * (((color.blue - color.red) / delta) + 2f)
        else -> 60f * (((color.red - color.green) / delta) + 4f)
    }
    return if (hue < 0) hue + 360f else hue
}
