// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.widget

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nikelauncher.R
import dev.nikelauncher.media.NotificationCountBus
import dev.nikelauncher.ui.theme.NikeTheme
import java.time.ZoneId

/** The own widget uses the same Android long-press wrapper as external widgets. */
class ClockHostView(context: Context) : AbstractComposeView(context) {
    var widgetId by mutableIntStateOf(0)
    var active by mutableStateOf(true)
    var interactive by mutableStateOf(true)

    /** The height the slot was configured to, which the clock is centred in and never moves from. */
    var configuredHeightDp by mutableIntStateOf(0)

    /** Told how tall the slot has to be to hold everything without moving the clock. */
    var onNeededHeightDp: ((Int) -> Unit)? = null

    @Composable
    override fun Content() {
        NikeTheme {
            ClockWidgetContent(widgetId, active, interactive, configuredHeightDp, onNeededHeightDp)
        }
    }
}

@Composable
fun ClockWidgetContent(
    widgetId: Int,
    active: Boolean = true,
    interactive: Boolean = true,
    configuredHeightDp: Int = 0,
    onNeededHeightDp: ((Int) -> Unit)? = null,
) {
    val state = rememberClockLiveState(widgetId, active)
    var sheet by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(active, interactive) { if (!active || !interactive) sheet = null }
    ClockHeader(
        state,
        Modifier.fillMaxSize(),
        interactive,
        onAgenda = { sheet = "agenda" },
        onWeather = { sheet = "weather" },
        configuredHeightDp = configuredHeightDp,
        onNeededHeightDp = onNeededHeightDp,
    )
    sheet?.let { mode -> ClockOverviewSheet(widgetId, mode, onDismiss = { sheet = null }) }
}

@Composable
fun ClockHeader(
    state: ClockLiveState,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
    onAgenda: () -> Unit = {},
    onWeather: () -> Unit = {},
    /** The slot's configured height, for the clock to be centred in; 0 means whatever it is given. */
    configuredHeightDp: Int = 0,
    /** Told how tall the slot has to be, so what arrives below the clock can never push it. */
    onNeededHeightDp: ((Int) -> Unit)? = null,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val c = state.config
    val color = Color(c.textColor)
    val count by NotificationCountBus.counts.collectAsState()
    val zone = ZoneId.systemDefault()
    val events =
        if (c.showAgenda)
            AgendaRules.homeEvents(
                (state.calendar as? CalendarLoad.Ready)?.events.orEmpty(),
                state.now,
                zone,
                c.agendaLookaheadHours,
            )
        else emptyList()
    // Rows that come and go — the agenda, the next alarm, the notification count — hang below
    // the clock and date, which stay where they are. Centering the lot in the slot, as this used
    // to, moved the clock by half of whatever arrived.
    val rowSp = listOfNotNull(
        c.dateSizeSp.takeIf { c.datePattern != null },
        c.batterySizeSp.takeIf { c.showBattery },
        c.weatherSizeSp.takeIf { c.showWeather },
    ).maxOrNull()
    AnchoredClockLayout(
        modifier = modifier.padding(horizontal = 4.dp),
        configuredHeightPx = with(density) { configuredHeightDp.dp.roundToPx() },
        onNeededHeightPx = onNeededHeightDp?.let { report ->
            { px -> report(with(density) { px.toDp().value.toInt() + 1 }) }
        },
        anchor = {
            Column {
                ClockFace(
                    c,
                    state.now,
                    Modifier.clickable(enabled = interactive) { ClockActions.clock(context, c) },
                )
                if (c.style != ClockStyle.NONE)
                    Spacer(Modifier.height(c.clockDataGapDp.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth(c.textWidthPercent.coerceIn(ClockWidgetConfig.TEXT_WIDTH_RANGE) / 100f)
                        // Held at the height of the tallest thing that can be in it, so the
                        // battery or weather appearing beside the date does not grow the row.
                        .then(
                            if (rowSp != null)
                                Modifier.heightIn(min = with(density) { (rowSp * 1.3f).sp.toDp() })
                            else Modifier
                        ),
                ) {
                    c.datePattern?.let { pattern ->
                        val date =
                            android.text.format.DateFormat.format(pattern, java.util.Date.from(state.now))
                                .toString()
                        Text(
                            date,
                            Modifier.weight(1f, fill = false)
                                .alignByBaseline()
                                .clickable(enabled = interactive, onClick = onAgenda),
                            color = Color(c.colorAt(c.dateOpacity)),
                            fontFamily = dev.nikelauncher.ui.theme.WidgetTextFamily,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                            fontSize = c.dateSizeSp.sp,
                            lineHeight = (c.dateSizeSp * 1.3f).sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    state.battery?.let {
                        Text(
                            it,
                            Modifier.alignByBaseline().clickable(enabled = interactive) {
                                ClockActions.battery(context)
                            },
                            color = Color(c.colorAt(c.batteryOpacity)),
                            fontFamily = dev.nikelauncher.ui.theme.WidgetTextFamily,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                            fontSize = c.batterySizeSp.sp,
                            lineHeight = (c.batterySizeSp * 1.3f).sp,
                            maxLines = 1,
                        )
                    }
                    if (c.showWeather) {
                        Text(
                            state.weatherReading?.let {
                                ClockWidgetWeather.degrees(it.tempK, c.weatherFahrenheit)
                            } ?: "—",
                            Modifier.alignByBaseline()
                                .clickable(enabled = interactive, onClick = onWeather),
                            color = Color(c.colorAt(c.weatherOpacity)),
                            fontFamily = dev.nikelauncher.ui.theme.WidgetTextFamily,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                            fontSize = c.weatherSizeSp.sp,
                            lineHeight = (c.weatherSizeSp * 1.3f).sp,
                            maxLines = 1,
                        )
                    }
                }
            }
        },
        dynamic = {
            Column(Modifier.fillMaxWidth(c.textWidthPercent.coerceIn(ClockWidgetConfig.TEXT_WIDTH_RANGE) / 100f)) {
                if (c.showAgenda) {
                    val shown = events.take(c.agendaCount)
                    shown.forEachIndexed { index, event ->
                        val extra =
                            if (index == shown.lastIndex && events.size > shown.size)
                                " +${events.size-shown.size}"
                            else ""
                        AgendaSummaryRow(
                            event.title,
                            AgendaText.homeWhen(context, event, state.now, c.agendaRelative, c.hourFormat) + extra,
                            c,
                            Modifier.padding(top = 4.dp).fillMaxWidth()
                                .clickable(enabled = interactive, onClick = onAgenda),
                        )
                    }
                    if (state.calendar is CalendarLoad.PermissionRequired) {
                        Text(
                            stringResource(R.string.clock_allow_calendar),
                            Modifier.clickable(enabled = interactive, onClick = onAgenda),
                            color = color,
                            fontFamily = dev.nikelauncher.ui.theme.WidgetTextFamily,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                            fontSize = c.agendaSizeSp.sp,
                            lineHeight = (c.agendaSizeSp * 1.3f).sp,
                        )
                    }
                    if (state.calendar is CalendarLoad.Unavailable) {
                        Text(
                            stringResource(R.string.clock_calendar_unavailable),
                            Modifier.clickable(enabled = interactive, onClick = onAgenda),
                            color = color,
                            fontFamily = dev.nikelauncher.ui.theme.WidgetTextFamily,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                            fontSize = c.agendaSizeSp.sp,
                            lineHeight = (c.agendaSizeSp * 1.3f).sp,
                        )
                    }
                }
                if (c.showAlarm)
                    ClockWidgetAgenda.nextAlarm(context)?.let { alarm ->
                        Text(
                            alarm.text,
                            Modifier.clickable(enabled = interactive) { ClockActions.clock(context, c) },
                            color = Color(c.colorAt(c.agendaOpacity)),
                            fontFamily = dev.nikelauncher.ui.theme.WidgetTextFamily,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                            fontSize = c.agendaSizeSp.sp,
                            lineHeight = (c.agendaSizeSp * 1.3f).sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                val notifications = count.values.sum()
                if (c.showNotifications && notifications > 0)
                    Text(
                        context.resources.getQuantityString(
                            R.plurals.widget_clock_notification_count,
                            notifications,
                            notifications,
                        ),
                        color = Color(c.colorAt(c.notificationsOpacity)),
                        fontFamily = dev.nikelauncher.ui.theme.WidgetTextFamily,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        fontSize = c.notificationsSizeSp.sp,
                        lineHeight = (c.notificationsSizeSp * 1.3f).sp,
                    )
            }
        },
    )
}

/** Measure the unweighted time first; only the title gives up space. */
@Composable
internal fun AgendaSummaryRow(title: String, time: String, c: ClockWidgetConfig, modifier: Modifier = Modifier) {
    val style = androidx.compose.ui.text.TextStyle(
        color = Color(c.colorAt(c.agendaOpacity)),
        fontFamily = dev.nikelauncher.ui.theme.WidgetTextFamily,
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        fontSize = c.agendaSizeSp.sp,
        lineHeight = (c.agendaSizeSp * 1.3f).sp,
    )
    Row(modifier) {
        Text(title, Modifier.weight(1f, fill = false).alignByBaseline(), style = style,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        // Extremely narrow slots may wrap this part rather than lose the clock time.
        Text(stringResource(R.string.clock_time_suffix, time), Modifier.alignByBaseline(), style = style)
    }
}
