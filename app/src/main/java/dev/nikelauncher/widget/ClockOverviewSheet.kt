// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.widget

import android.Manifest
import android.appwidget.AppWidgetManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import kotlinx.coroutines.launch
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.nikelauncher.ui.kit.motion.AppMotion
import dev.nikelauncher.R
import dev.nikelauncher.ui.theme.NikeTheme

class ClockOverviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id =
            intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID,
            )
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        setContent {
            NikeTheme {
                ClockOverviewSheet(
                    id,
                    intent.getStringExtra("mode") ?: "agenda",
                    onDismiss = { finish() },
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ClockOverviewSheet(
    widgetId: Int,
    initialMode: String,
    onDismiss: () -> Unit,
    configOverride: ClockWidgetConfig? = null,
) {
    val context = LocalContext.current
    val mode = if (initialMode == "weather") "weather" else "agenda"
    var permissionRevision by remember { mutableIntStateOf(0) }
    var weatherPage by remember { mutableStateOf(WeatherPage.OVERVIEW) }
    val liveState =
        key(permissionRevision) {
            rememberClockLiveState(
                widgetId,
                override = configOverride,
                requireCalendar = mode == "agenda",
                loadCalendar = mode == "agenda",
                loadWeather = mode == "weather",
            )
        }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionRevision++
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val state = liveState
    val shownPage = weatherPage
    fun closeThen(action: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            onDismiss()
            action()
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        sheetMaxWidth = 640.dp,
        dragHandle = null,
        contentWindowInsets = {
            WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
        },
    ) {
        BackHandler(enabled = mode == "weather" && shownPage != WeatherPage.OVERVIEW) {
            weatherPage = WeatherPage.OVERVIEW
        }
        val density = LocalDensity.current
        val safeTop = with(density) {
            maxOf(WindowInsets.statusBars.getTop(this), WindowInsets.displayCutout.getTop(this)).toDp()
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            // Use the available viewport from the first frame. Keep the sheet below the
            // status bar/cutout without a second height cap or an inset above the handle.
            val panelHeight = (maxHeight - safeTop).coerceAtLeast(0.dp)
            Column(Modifier.fillMaxWidth().height(panelHeight).testTag("clock-overview-panel")
                .semantics { testTagsAsResourceId = true }) {
                // The stock handle brings 22dp of air on either side; the header carries its own padding.
                Box(Modifier.fillMaxWidth().height(24.dp), contentAlignment = Alignment.Center) {
                    BottomSheetDefaults.DragHandle(Modifier.wrapContentHeight(unbounded = true))
                }
                Row(
                    Modifier.fillMaxWidth().padding(start = 12.dp, end = 8.dp, top = 0.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(Modifier.width(8.dp))
                    AnimatedVisibility(
                        visible = mode == "weather" && shownPage != WeatherPage.OVERVIEW,
                        enter = expandHorizontally(AppMotion.spatial()) + fadeIn(AppMotion.effects()),
                        exit = shrinkHorizontally(AppMotion.spatial()) + fadeOut(AppMotion.effects()),
                    ) {
                        IconButton(onClick = { weatherPage = WeatherPage.OVERVIEW }) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.action_back))
                        }
                    }
                    val heading = when {
                        mode != "weather" -> R.string.clock_agenda_title
                        shownPage == WeatherPage.HOURLY -> R.string.clock_weather_hourly
                        shownPage == WeatherPage.DAILY -> R.string.clock_weather_daily
                        shownPage == WeatherPage.DAYLIGHT -> R.string.weather_day_night
                        else -> R.string.clock_weather_title
                    }
                    val headingSpec = AppMotion.effects<Float>()
                    AnimatedContent(
                        targetState = heading,
                        modifier = Modifier.weight(1f),
                        transitionSpec = {
                            (fadeIn(headingSpec) togetherWith fadeOut(headingSpec))
                                .using(null)
                        },
                        label = "clock-overview-heading",
                    ) { title ->
                        Text(stringResource(title), style = MaterialTheme.typography.titleLarge)
                    }
                    IconButton(onClick = { closeThen {} }) {
                        Icon(Icons.Outlined.Close, stringResource(R.string.clock_close))
                    }
                }
                Box(Modifier.fillMaxWidth().weight(1f)) {
                    if (mode == "weather")
                        WeatherOverview(
                            state = state,
                            page = shownPage,
                            onPage = { weatherPage = it },
                            onOpenApp = { closeThen { ClockActions.weather(context, state.config) } },
                        )
                    else when (state.calendar) {
                        null -> Text(stringResource(R.string.clock_loading), Modifier.padding(16.dp))
                        CalendarLoad.PermissionRequired ->
                            Column(Modifier.padding(16.dp)) {
                                Text(stringResource(R.string.clock_calendar_permission))
                                TextButton(onClick = { ask.launch(Manifest.permission.READ_CALENDAR) }) {
                                    Text(stringResource(R.string.clock_allow_calendar))
                                }
                            }
                        CalendarLoad.Unavailable ->
                            TextButton(
                                onClick = { permissionRevision++ },
                                modifier = Modifier.padding(16.dp),
                            ) {
                                Text(stringResource(R.string.clock_calendar_unavailable))
                            }
                        is CalendarLoad.Ready -> CalendarOverview(
                            state = state,
                            onOpenEvent = { event -> closeThen { ClockActions.event(context, event, state.config) } },
                            onNewEvent = { date -> closeThen { ClockActions.insert(context, date, state.config) } },
                        )
                    }
                }
            }
        }
    }
}
