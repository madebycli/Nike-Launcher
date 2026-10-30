// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.widget.config

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dev.nikelauncher.NikeApp
import dev.nikelauncher.R
import dev.nikelauncher.data.AppInfo
import dev.nikelauncher.data.EntryKind
import dev.nikelauncher.ui.kit.settings.ColorPickerSheet
import dev.nikelauncher.ui.kit.settings.SettingsPickerSheet
import dev.nikelauncher.widget.ClockWidgetConfig
import java.util.Date

/** The secondary windows the clock settings can open. */
enum class ClockConfigDialog { CUSTOM_DATE, WEATHER_APP, TEXT_COLOR }

@Composable
internal fun ClockConfigDialogs(
    dialog: ClockConfigDialog?,
    config: ClockWidgetConfig,
    update: ((ClockWidgetConfig) -> ClockWidgetConfig) -> Unit,
    dismiss: () -> Unit,
) {
    when (dialog) {
        null -> Unit
        ClockConfigDialog.CUSTOM_DATE -> CustomDateDialog(
            initial = config.datePattern ?: ClockWidgetConfig.DEFAULT_DATE,
            onConfirm = { pattern -> update { it.copy(datePattern = pattern) }; dismiss() },
            onDismiss = dismiss,
        )
        ClockConfigDialog.WEATHER_APP -> WeatherAppSheet(
            selected = config.weatherPackage,
            onPick = { pkg -> update { it.copy(weatherPackage = pkg) }; dismiss() },
            onDismiss = dismiss,
        )
        ClockConfigDialog.TEXT_COLOR -> ColorPickerSheet(
            initial = Color(config.textColor),
            wallpaperColors = MaterialTheme.colorScheme.let { listOf(it.primary, it.secondary, it.tertiary, it.primaryContainer) },
            onPick = { color -> update { it.copy(textColor = color.toArgb()) } },
            onDismiss = dismiss,
        )
    }
}

/**
 * A pattern typed by hand, so the formats nobody here thought of are reachable.
 *
 * Refuses what TextClock cannot draw, since the widget lives in another process and a bad
 * pattern there is a widget that silently will not render.
 */
@Composable
private fun CustomDateDialog(initial: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initial) }
    val valid = ClockWidgetConfig.isValidDatePattern(text)
    val preview = if (valid) {
        runCatching { DateFormat.format(text, Date()).toString() }.getOrDefault("")
    } else {
        stringResource(R.string.widget_clock_date_custom_invalid)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.widget_clock_date_custom)) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= ClockWidgetConfig.MAX_DATE_PATTERN) text = it },
                    singleLine = true,
                    isError = !valid,
                    label = { Text(stringResource(R.string.widget_clock_date_custom_label)) },
                    supportingText = { Text(preview) },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }, enabled = valid) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/**
 * Which app the weather row opens: every launchable app, since there is no category the system
 * recognises for weather and the broadcast that brings the forecast does not say who sent it.
 */
@Composable
private fun WeatherAppSheet(selected: String?, onPick: (String?) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val apps = remember {
        runCatching {
            (context.applicationContext as NikeApp).appRepository.queryAllApps()
                .filter { it.kind == EntryKind.APP }
                .sortedBy { it.label.lowercase() }
        }.getOrDefault(emptyList<AppInfo>())
    }
    val none = stringResource(R.string.settings_quick_launch_none)
    SettingsPickerSheet(
        title = stringResource(R.string.widget_clock_weather_opens),
        items = listOf<AppInfo?>(null) + apps,
        selected = apps.firstOrNull { it.componentName.packageName == selected },
        itemLabel = { it?.label ?: none },
        onSelect = { onPick(it?.componentName?.packageName) },
        onDismiss = onDismiss,
    )
}
