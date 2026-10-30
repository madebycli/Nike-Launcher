// SPDX-License-Identifier: GPL-3.0-or-later
// Structure taken from Victoria Grid's clock widget settings: a home page that opens Clock,
// Calendar, Weather and Appearance, a live preview that stays put, and a gallery of styles.
package dev.nikelauncher.widget.config

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import dev.nikelauncher.R
import dev.nikelauncher.ui.kit.motion.AppMotion
import dev.nikelauncher.widget.ClockOverviewSheet
import dev.nikelauncher.widget.ClockStyleGallery
import dev.nikelauncher.widget.ClockWidgetAgenda
import dev.nikelauncher.widget.ClockWidgetConfig
import dev.nikelauncher.widget.rememberClockLiveState

internal typealias ConfigUpdate = ((ClockWidgetConfig) -> ClockWidgetConfig) -> Unit

/** The pages of the clock settings; Home is the one that leads to the others. */
internal enum class ConfigPage(@StringRes val title: Int) {
    HOME(R.string.widget_settings_title),
    CLOCK(R.string.widget_settings_clock),
    STYLES(R.string.clock_style),
    CALENDAR(R.string.widget_settings_calendar),
    WEATHER(R.string.widget_settings_weather),
    APPEARANCE(R.string.widget_settings_appearance),
}

/**
 * The clock widget's settings. Nothing is written until Done; closing throws the changes away.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockConfigScreen(
    widgetId: Int,
    initial: ClockWidgetConfig,
    onSave: (ClockWidgetConfig) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    var config by rememberSaveable { mutableStateOf(initial) }
    var lastDatePattern by rememberSaveable { mutableStateOf(initial.datePattern ?: ClockWidgetConfig.DEFAULT_DATE) }
    var page by rememberSaveable { mutableStateOf(ConfigPage.HOME) }
    var dialog by remember { mutableStateOf<ClockConfigDialog?>(null) }
    var overview by remember { mutableStateOf<String?>(null) }
    // Asked for here rather than at install, and re-read on the way back so the page stops
    // offering what has just been granted.
    var calendarGranted by remember { mutableStateOf(ClockWidgetAgenda.hasCalendarPermission(context)) }
    val askCalendar = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        calendarGranted = granted
        // Refused, so nothing would ever appear; better to leave it off than to show a widget
        // that silently has nothing in it.
        config = config.copy(showAgenda = granted)
    }
    val update: ConfigUpdate = { change ->
        config = change(config)
        config.datePattern?.let { lastDatePattern = it }
    }
    val state = rememberClockLiveState(widgetId, override = config)
    val holder = rememberSaveableStateHolder()
    val effectsSpec = AppMotion.effects<Float>()

    fun back() {
        page = if (page == ConfigPage.STYLES) ConfigPage.CLOCK else ConfigPage.HOME
    }
    BackHandler(page != ConfigPage.HOME) { back() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(page.title), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    val home = page == ConfigPage.HOME
                    IconButton(onClick = { if (home) onCancel() else back() }) {
                        Icon(
                            if (home) Icons.Filled.Close else Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(if (home) R.string.clock_close else R.string.action_back),
                        )
                    }
                },
                actions = {
                    if (page != ConfigPage.STYLES) {
                        IconButton(onClick = { page = ConfigPage.STYLES }) {
                            Icon(Icons.Filled.Style, stringResource(R.string.clock_style))
                        }
                    }
                    TextButton(onClick = { onSave(config) }) { Text(stringResource(R.string.widget_settings_done)) }
                },
            )
        },
    ) { insets ->
        AnimatedContent(
            targetState = page,
            modifier = Modifier.padding(insets).fillMaxSize(),
            transitionSpec = { fadeIn(effectsSpec) togetherWith fadeOut(effectsSpec) },
            label = "clockConfigPage",
        ) { current ->
            holder.SaveableStateProvider(current.name) {
                if (current == ConfigPage.STYLES) {
                    ClockStyleGallery(config, onSelect = { style -> update { it.copy(style = style) } })
                } else {
                    PinnedPreview(state) {
                        when (current) {
                            ConfigPage.HOME -> HomePage(config, update) { page = it }
                            ConfigPage.CLOCK -> ClockPage(config, update, { page = ConfigPage.STYLES }) { dialog = it }
                            ConfigPage.CALENDAR -> CalendarPage(
                                config, lastDatePattern, calendarGranted, update, { dialog = it }, { overview = "agenda" },
                            ) { askCalendar.launch(Manifest.permission.READ_CALENDAR) }
                            ConfigPage.WEATHER -> WeatherPage(config, state.weatherReading != null, update, { dialog = it }) { overview = "weather" }
                            ConfigPage.APPEARANCE -> AppearancePage(config, update) { dialog = it }
                            ConfigPage.STYLES -> Unit
                        }
                    }
                }
            }
        }
    }

    ClockConfigDialogs(dialog, config, update) { dialog = null }
    overview?.let { mode -> ClockOverviewSheet(widgetId, mode, onDismiss = { overview = null }, configOverride = config) }
}
