// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.widget

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/** A concrete occurrence. UTC all-day dates are converted only at the display boundary. */
data class CalendarEvent(
    val eventId: Long,
    val calendarId: Long,
    val title: String,
    val begin: Instant,
    val end: Instant,
    val allDay: Boolean,
    val color: Int,
    val location: String = "",
    val tentative: Boolean = false,
) {
    val key: String
        get() = "$calendarId:$eventId:${begin.toEpochMilli()}"

    fun firstDate(zone: ZoneId): LocalDate =
        begin.atZone(if (allDay) ZoneOffset.UTC else zone).toLocalDate()

    fun startAt(zone: ZoneId): Instant =
        if (allDay) firstDate(zone).atStartOfDay(zone).toInstant() else begin

    fun endAt(zone: ZoneId): Instant =
        if (allDay) {
            end.atZone(ZoneOffset.UTC).toLocalDate().atStartOfDay(zone).toInstant()
        } else end
}

data class CalendarSegment(
    val event: CalendarEvent,
    val date: LocalDate,
    val index: Int,
    val count: Int,
) {
    val key: String
        get() = "${event.key}:$date"

    val showStart: Boolean
        get() = !event.allDay && index == 0

    val showEnd: Boolean
        get() = !event.allDay && index == count - 1
}

enum class RelativeUnit {
    NOW,
    MINUTES,
    HOURS,
    DAYS,
    TODAY,
    TOMORROW,
}

data class RelativeLabel(val unit: RelativeUnit, val count: Long = 0, val future: Boolean = false)

/** Pure rules, shared by the live widget, agenda and RemoteViews. */
object AgendaRules {
    fun eligible(
        event: CalendarEvent,
        now: Instant,
        zone: ZoneId,
        lookaheadHours: Int = 12,
    ): Boolean {
        val hours = if (event.allDay) 6L else lookaheadHours.coerceIn(1, 168).toLong()
        return !now.isBefore(event.startAt(zone).minus(Duration.ofHours(hours))) &&
            now.isBefore(event.endAt(zone))
    }

    fun homeEvents(
        events: List<CalendarEvent>,
        now: Instant,
        zone: ZoneId,
        lookaheadHours: Int = 12,
    ): List<CalendarEvent> {
        val today = now.atZone(zone).toLocalDate()
        return events
            .asSequence()
            .filter { eligible(it, now, zone, lookaheadHours) }
            .sortedWith(
                compareBy<CalendarEvent>(
                    { it.firstDate(zone) != today },
                    { it.endAt(zone).minusNanos(1).atZone(zone).toLocalDate() != today },
                    { it.firstDate(zone) },
                    { it.allDay },
                    { it.startAt(zone) },
                    { it.endAt(zone) },
                    { it.key },
                )
            )
            .toList()
    }

    fun relative(
        at: Instant,
        now: Instant,
        zone: ZoneId,
        dateOnly: Boolean = false,
    ): RelativeLabel {
        val days =
            ChronoUnit.DAYS.between(now.atZone(zone).toLocalDate(), at.atZone(zone).toLocalDate())
        val future = at.isAfter(now)
        val distance = Duration.between(now, at).abs()
        if (dateOnly && days == 0L) return RelativeLabel(RelativeUnit.TODAY)
        if (dateOnly && days == 1L) return RelativeLabel(RelativeUnit.TOMORROW)
        return when {
            dateOnly -> RelativeLabel(RelativeUnit.DAYS, kotlin.math.abs(days), days > 0)
            distance < Duration.ofMinutes(1) -> RelativeLabel(RelativeUnit.NOW)
            distance < Duration.ofHours(1) ->
                RelativeLabel(RelativeUnit.MINUTES, distance.toMinutes(), future)
            distance < Duration.ofDays(1) ->
                RelativeLabel(RelativeUnit.HOURS, distance.toHours(), future)
            days == 1L && future -> RelativeLabel(RelativeUnit.TOMORROW)
            else -> RelativeLabel(RelativeUnit.DAYS, kotlin.math.abs(days).coerceAtLeast(1), future)
        }
    }

    fun segments(
        events: List<CalendarEvent>,
        from: LocalDate,
        days: Int,
        zone: ZoneId,
    ): List<CalendarSegment> {
        val until = from.plusDays(days.coerceIn(1, 90).toLong())
        return events
            .flatMap { event ->
                val first = event.firstDate(zone)
                val last =
                    event.endAt(zone).minusNanos(1).atZone(zone).toLocalDate().coerceAtLeast(first)
                val count = ChronoUnit.DAYS.between(first, last).toInt() + 1
                val visibleFirst = maxOf(first, from)
                val visibleLast = minOf(last, until.minusDays(1))
                if (visibleFirst > visibleLast) emptyList()
                else {
                    generateSequence(visibleFirst) { it.plusDays(1) }
                        .takeWhile { it <= visibleLast }
                        .map {
                            CalendarSegment(
                                event,
                                it,
                                ChronoUnit.DAYS.between(first, it).toInt(),
                                count,
                            )
                        }
                        .toList()
                }
            }
            .sortedWith(
                compareBy<CalendarSegment>(
                    { it.date },
                    { !it.event.allDay },
                    { it.event.startAt(zone) },
                    { it.event.endAt(zone) },
                    { it.event.key },
                )
            )
    }

    fun nextUpdate(
        now: Instant,
        events: List<CalendarEvent>,
        zone: ZoneId,
        lookaheadHours: Int = 12,
    ): Instant {
        var next = now.truncatedTo(ChronoUnit.MINUTES).plusSeconds(60)
        for (event in events) {
            val start = event.startAt(zone)
            for (at in
                listOf(
                    start.minus(
                        Duration.ofHours(
                            if (event.allDay) 6L else lookaheadHours.coerceIn(1, 168).toLong()
                        )
                    ),
                    start.minusSeconds(60),
                    start.plusMillis(1),
                    event.endAt(zone),
                )) {
                if (at > now && at < next) next = at
            }
            // A provider can include seconds. Countdowns then change between wall-clock minutes.
            val delta = Duration.between(now, start).toMillis()
            if (delta in 1 until 3_600_000) {
                val boundary = now.plusMillis(delta % 60_000 + 1)
                if (boundary < next) next = boundary
            }
        }
        return next
    }
}
