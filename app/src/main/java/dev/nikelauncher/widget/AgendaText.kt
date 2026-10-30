// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.widget

import android.content.Context
import android.text.format.DateFormat
import dev.nikelauncher.R
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object AgendaText {
    fun clock(
        context: Context,
        at: Instant,
        format: ClockWidgetConfig.HourFormat = ClockWidgetConfig.HourFormat.SYSTEM,
    ): String {
        val twentyFour =
            when (format) {
                ClockWidgetConfig.HourFormat.SYSTEM -> DateFormat.is24HourFormat(context)
                ClockWidgetConfig.HourFormat.TWENTY_FOUR -> true
                ClockWidgetConfig.HourFormat.TWELVE -> false
            }
        val locale = context.resources.configuration.locales[0]
        val pattern = DateFormat.getBestDateTimePattern(locale, if (twentyFour) "HHmm" else "hm")
        return DateTimeFormatter.ofPattern(pattern, locale)
            .format(at.atZone(ZoneId.systemDefault()))
    }

    fun date(context: Context, date: LocalDate): String {
        val locale = context.resources.configuration.locales[0]
        return DateTimeFormatter.ofPattern(
                DateFormat.getBestDateTimePattern(locale, "EEEMMMd"),
                locale,
            )
            .format(date)
    }

    fun relative(context: Context, label: RelativeLabel): String {
        val count = label.count.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        return when (label.unit) {
            RelativeUnit.NOW -> context.getString(R.string.clock_now)
            RelativeUnit.TODAY -> context.getString(R.string.clock_today)
            RelativeUnit.TOMORROW -> context.getString(R.string.clock_tomorrow)
            RelativeUnit.MINUTES ->
                context.resources.getQuantityString(
                    if (label.future) R.plurals.clock_in_minutes else R.plurals.clock_minutes,
                    count,
                    count,
                )
            RelativeUnit.HOURS ->
                context.resources.getQuantityString(
                    if (label.future) R.plurals.clock_in_hours else R.plurals.clock_hours,
                    count,
                    count,
                )
            RelativeUnit.DAYS ->
                context.resources.getQuantityString(
                    if (label.future) R.plurals.clock_in_days else R.plurals.clock_days,
                    count,
                    count,
                )
        }
    }

    fun homeWhen(
        context: Context,
        event: CalendarEvent,
        now: Instant,
        useRelative: Boolean,
        format: ClockWidgetConfig.HourFormat = ClockWidgetConfig.HourFormat.SYSTEM,
    ): String {
        val zone = ZoneId.systemDefault()
        val start = event.startAt(zone)
        val end = event.endAt(zone)
        if (event.allDay)
            return relative(context, AgendaRules.relative(start, now, zone, dateOnly = true))
        if (now <= start)
            return if (useRelative && Duration.between(now, start) < Duration.ofHours(1)) {
                relative(context, AgendaRules.relative(start, now, zone))
            } else {
                val eventDate = start.atZone(zone).toLocalDate()
                val today = now.atZone(zone).toLocalDate()
                val time = clock(context, start, format)
                when (eventDate) {
                    today -> time
                    today.plusDays(1) -> "${context.getString(R.string.clock_tomorrow)} $time"
                    else -> "${date(context, eventDate)} $time"
                }
            }
        val today = now.atZone(zone).toLocalDate()
        val first = event.firstDate(zone)
        val last = end.minusNanos(1).atZone(zone).toLocalDate()
        return when {
            first == last -> "${clock(context, start, format)}–${clock(context, end, format)}"
            today == first -> clock(context, start, format)
            today == last -> context.getString(R.string.clock_until, clock(context, end, format))
            else -> relative(context, AgendaRules.relative(start, now, zone, dateOnly = true))
        }
    }

    fun home(
        context: Context,
        event: CalendarEvent,
        now: Instant,
        useRelative: Boolean,
        format: ClockWidgetConfig.HourFormat = ClockWidgetConfig.HourFormat.SYSTEM,
    ): String = "${event.title} · ${homeWhen(context, event, now, useRelative, format)}"

    fun details(
        context: Context,
        segment: CalendarSegment,
        now: Instant,
        format: ClockWidgetConfig.HourFormat = ClockWidgetConfig.HourFormat.SYSTEM,
    ): String {
        val event = segment.event
        val parts = mutableListOf<String>()
        when {
            segment.showStart && segment.showEnd ->
                parts +=
                    "${clock(context, event.begin, format)}–${clock(context, event.end, format)}"
            segment.showStart -> parts += clock(context, event.begin, format)
            segment.showEnd ->
                parts += context.getString(R.string.clock_until, clock(context, event.end, format))
        }
        if (segment.showStart)
            parts +=
                relative(context, AgendaRules.relative(event.begin, now, ZoneId.systemDefault()))
        return parts.joinToString(" · ")
    }
}
