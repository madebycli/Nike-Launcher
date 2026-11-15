// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SegmentedListItem
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
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.nikelauncher.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The agenda in Orion's vocabulary: each day is a section title (the date, then how far away it is), its events one
 * segmented group with the calendar's colour leading each row. Today always shows, with a way to add an event to it.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        for (date in dates) {
            item("day:$date", contentType = "date") {
                Row(
                    Modifier.testTag("calendar-day-$date").fillMaxWidth()
                        .padding(start = 8.dp, top = 16.dp, bottom = 6.dp)
                        .semantics(mergeDescendants = true) { heading() },
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(AgendaText.date(context, date), style = MaterialTheme.typography.titleSmallEmphasized,
                        color = MaterialTheme.colorScheme.primary)
                    Text(
                        AgendaText.relative(
                            context,
                            AgendaRules.relative(date.atStartOfDay(zone).toInstant(), state.now, zone, true),
                        ),
                        Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            val entries = segments[date].orEmpty()
            entries.forEachIndexed { index, segment ->
                item(segment.key, contentType = "event") {
                    CalendarEventRow(segment, index, entries.size, state.now, state.config) { onOpenEvent(segment.event) }
                }
            }
            if (date == today && entries.isEmpty()) {
                item("empty:$date", contentType = "empty") {
                    Text(
                        stringResource(R.string.clock_no_events),
                        Modifier.padding(start = 8.dp, bottom = 4.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (date == today) {
                item("insert:$date", contentType = "insert") {
                    FilledTonalButton(
                        onClick = { onNewEvent(date) },
                        modifier = Modifier.padding(top = 8.dp).testTag("calendar-new-event"),
                        shapes = ButtonDefaults.shapes(),
                    ) {
                        Icon(Icons.Outlined.Add, null, Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                        Text(stringResource(R.string.clock_new_event))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CalendarEventRow(
    segment: CalendarSegment,
    index: Int,
    count: Int,
    now: Instant,
    config: ClockWidgetConfig,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val event = segment.event
    val suffix = if (segment.count > 1) " (${segment.index + 1}/${segment.count})" else ""
    val detail = if (event.allDay) stringResource(R.string.clock_all_day)
        else AgendaText.details(context, segment, now, config.hourFormat)
    val supporting = listOf(detail, event.location).filter { it.isNotBlank() }
    val openLabel = stringResource(R.string.calendar_open_event)
    SegmentedListItem(
        onClick = onClick,
        shapes = ListItemDefaults.segmentedShapes(index, count),
        colors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.testTag("calendar-event-${segment.key}")
            .semantics { onClick(label = openLabel) { onClick(); true } },
        leadingContent = {
            Box(
                Modifier.size(width = 6.dp, height = 36.dp)
                    .testTag("calendar-color-${segment.key}")
                    .background(Color(event.color), MaterialTheme.shapes.extraSmall),
            )
        },
        supportingContent = if (supporting.isNotEmpty()) ({
            Column { supporting.forEach { Text(it, maxLines = 2, overflow = TextOverflow.Ellipsis) } }
        }) else null,
        content = { Text(event.title + suffix, maxLines = 2, overflow = TextOverflow.Ellipsis) },
    )
}
