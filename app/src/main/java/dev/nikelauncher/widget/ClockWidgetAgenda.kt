// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.widget

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import dev.nikelauncher.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.Date

sealed interface CalendarLoad {
    data class Ready(val events: List<CalendarEvent>) : CalendarLoad

    data object PermissionRequired : CalendarLoad

    data object Unavailable : CalendarLoad
}

data class CalendarChoice(val id: Long, val name: String, val color: Int)

object ClockWidgetAgenda {
    data class Entry(val text: String, val title: String? = null, val time: String? = null)

    fun hasCalendarPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    /** Current profile only. No enterprise-provider or hidden-profile access is added. */
    fun load(
        context: Context,
        now: Instant = Instant.now(),
        days: Int = 30,
        excluded: Set<String> = emptySet(),
    ): CalendarLoad {
        if (!hasCalendarPermission(context)) return CalendarLoad.PermissionRequired
        val zone = ZoneId.systemDefault()
        val until =
            now.atZone(zone)
                .toLocalDate()
                .plusDays(days.coerceIn(1, 90).toLong())
                .atStartOfDay(zone)
                .toInstant()
        // UTC all-day instances may have ended in the provider before their local day ends.
        val uri =
            CalendarContract.Instances.CONTENT_URI.buildUpon()
                .appendPath(now.minus(1, ChronoUnit.DAYS).toEpochMilli().toString())
                .appendPath(until.toEpochMilli().toString())
                .build()
        return try {
            val projection =
                arrayOf(
                    "event_id",
                    "calendar_id",
                    "title",
                    "begin",
                    "end",
                    "allDay",
                    "displayColor",
                    "eventLocation",
                    "selfAttendeeStatus",
                    "accessLevel",
                )
            val cursor =
                context.contentResolver.query(
                    uri,
                    projection,
                    "visible = 1 AND (selfAttendeeStatus IS NULL OR selfAttendeeStatus != ?) AND (eventStatus IS NULL OR eventStatus != ?)",
                    arrayOf("2", "2"),
                    "begin ASC, event_id ASC",
                ) ?: return CalendarLoad.Unavailable
            cursor.use {
                val events = mutableListOf<CalendarEvent>()
                while (it.moveToNext()) {
                    val calendarId = it.getLong(1)
                    if (calendarId.toString() in excluded) continue
                    val begin = Instant.ofEpochMilli(it.getLong(3))
                    val end = Instant.ofEpochMilli(it.getLong(4))
                    if (end <= begin) continue
                    val title =
                        it.getString(2)?.trim().orEmpty().ifBlank {
                            context.getString(
                                if (it.getInt(9) == 2) R.string.clock_event_busy
                                else R.string.widget_clock_event_untitled
                            )
                        }
                    val event =
                        CalendarEvent(
                            it.getLong(0),
                            calendarId,
                            title,
                            begin,
                            end,
                            it.getInt(5) != 0,
                            it.getInt(6),
                            it.getString(7).orEmpty(),
                            it.getInt(8) == 4,
                        )
                    if (event.endAt(zone) > now) events += event
                }
                CalendarLoad.Ready(events.distinctBy(CalendarEvent::key))
            }
        } catch (_: SecurityException) {
            CalendarLoad.PermissionRequired
        } catch (_: RuntimeException) {
            CalendarLoad.Unavailable
        }
    }

    fun calendars(context: Context): List<CalendarChoice> {
        if (!hasCalendarPermission(context)) return emptyList()
        return runCatching {
                context.contentResolver
                    .query(
                        CalendarContract.Calendars.CONTENT_URI,
                        arrayOf("_id", "calendar_displayName", "calendar_color"),
                        "visible = 1",
                        null,
                        "calendar_displayName COLLATE LOCALIZED ASC",
                    )
                    ?.use { cursor ->
                        buildList {
                            while (cursor.moveToNext()) add(
                                CalendarChoice(
                                    cursor.getLong(0),
                                    cursor.getString(1).orEmpty(),
                                    cursor.getInt(2),
                                )
                            )
                        }
                    }
                    .orEmpty()
            }
            .getOrDefault(emptyList())
    }

    fun events(
        context: Context,
        limit: Int,
        lookaheadHours: Int,
        relative: Boolean,
        excluded: Set<String> = emptySet(),
        hourFormat: ClockWidgetConfig.HourFormat = ClockWidgetConfig.HourFormat.SYSTEM,
    ): List<Entry> {
        val now = Instant.now()
        val days = (lookaheadHours.coerceIn(1, 168) + 23) / 24 + 1
        val events = (load(context, now, days, excluded) as? CalendarLoad.Ready)?.events.orEmpty()
        val selected = AgendaRules.homeEvents(events, now, ZoneId.systemDefault(), lookaheadHours)
        val count = limit.coerceIn(1, 3)
        return selected.take(count).mapIndexed { index, event ->
            val remaining =
                if (index == minOf(count, selected.size) - 1)
                    (selected.size - count).coerceAtLeast(0)
                else 0
            val time = AgendaText.homeWhen(context, event, now, relative, hourFormat) +
                if (remaining > 0) " +$remaining" else ""
            Entry("${event.title} · $time", event.title, time)
        }
    }

    fun nextAlarm(context: Context): Entry? =
        runCatching {
                val next =
                    context.getSystemService(AlarmManager::class.java)?.nextAlarmClock
                        ?: return null
                Entry(
                    context.getString(
                        R.string.widget_clock_alarm_at,
                        android.text.format.DateFormat.getTimeFormat(context)
                            .format(Date(next.triggerTime)),
                    )
                )
            }
            .getOrNull()

    internal fun eventDate(begin: Long, allDay: Boolean, local: ZoneId): LocalDate =
        Instant.ofEpochMilli(begin).atZone(if (allDay) ZoneOffset.UTC else local).toLocalDate()

    internal fun daysAway(begin: Long, allDay: Boolean, now: Long, local: ZoneId): Long =
        ChronoUnit.DAYS.between(
            Instant.ofEpochMilli(now).atZone(local).toLocalDate(),
            eventDate(begin, allDay, local),
        )
}
