// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nikelauncher.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Original agenda layout, extracted from ClockOverviewSheet before the calendar redesign. */
@Composable
internal fun CalendarOverview(
    state: ClockLiveState,
    onOpenEvent: (CalendarEvent) -> Unit,
    onNewEvent: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val zone = ZoneId.systemDefault()
    val today = state.now.atZone(zone).toLocalDate()
    val events = (state.calendar as? CalendarLoad.Ready)?.events.orEmpty()
    val segments = remember(events, today, zone) {
        AgendaRules.segments(events, today, 30, zone).groupBy { it.date }
    }
    val dates = remember(segments, today) { (segments.keys + today).sorted() }
    val scroll = rememberLazyListState()
    LazyColumn(
        modifier.fillMaxSize().testTag("calendar-agenda-list"),
        state = scroll,
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        for (date in dates) {
            item("day:$date", contentType = "date") {
                Row(
                    Modifier.testTag("calendar-day-$date")
                        .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
                        .semantics { heading() },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(AgendaText.date(context, date), fontSize = 16.sp)
                    Text(
                        AgendaText.relative(
                            context,
                            AgendaRules.relative(date.atStartOfDay(zone).toInstant(), state.now, zone, true),
                        ),
                        Modifier.padding(start = 8.dp),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            val entries = segments[date].orEmpty()
            for (segment in entries) {
                item(segment.key, contentType = "event") {
                    CalendarEventRow(
                        segment, state.now, state.config,
                        onClick = { onOpenEvent(segment.event) },
                    )
                }
            }
            if (date == today && entries.isEmpty()) {
                item("empty:$date", contentType = "empty") {
                    Text(
                        stringResource(R.string.clock_no_events),
                        Modifier.padding(start = 52.dp, end = 16.dp, bottom = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                    )
                }
            }
            if (date == today) {
                item("insert:$date", contentType = "insert") {
                    NewCalendarEventRow { onNewEvent(date) }
                }
            }
        }
    }
}

@Composable
private fun NewCalendarEventRow(onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().testTag("calendar-new-event")
            .clickable(onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Add, null, Modifier.size(24.dp))
        Text(
            stringResource(R.string.clock_new_event),
            Modifier.padding(start = 12.dp),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun CalendarEventRow(
    segment: CalendarSegment,
    now: Instant,
    config: ClockWidgetConfig,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val event = segment.event
    val openLabel = stringResource(R.string.calendar_open_event)
    Row(
        Modifier.fillMaxWidth().testTag("calendar-event-${segment.key}")
            .clickable(onClickLabel = openLabel, onClick = onClick)
            .height(IntrinsicSize.Min)
            .heightIn(min = 48.dp)
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
    ) {
        Box(Modifier.width(24.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
            Box(
                Modifier.width(6.dp).fillMaxHeight()
                    .testTag("calendar-color-${segment.key}")
                    .background(Color(event.color), MaterialTheme.shapes.extraSmall),
            )
        }
        Column(
            Modifier.padding(start = 12.dp).weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            val suffix = if (segment.count > 1) " (${segment.index + 1}/${segment.count})" else ""
            Text(event.title + suffix, fontSize = 16.sp)
            val detail = if (event.allDay) stringResource(R.string.clock_all_day)
                else AgendaText.details(context, segment, now, config.hourFormat)
            if (detail.isNotBlank())
                Text(detail, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (event.location.isNotBlank())
                Text(event.location, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
