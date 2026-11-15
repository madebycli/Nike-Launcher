// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.widget

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
import dev.nikelauncher.R
import java.time.LocalDate
import java.time.ZoneId

object ClockActions {
    fun launch(context: Context, intent: Intent) {
        runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure {
                Toast.makeText(context, R.string.clock_action_unavailable, Toast.LENGTH_SHORT)
                    .show()
            }
    }

    fun clock(context: Context, config: ClockWidgetConfig) =
        launch(context, ClockWidgetProvider.clockIntent(context, config.clockPackage))

    fun battery(context: Context) = launch(context, ClockWidgetProvider.batteryIntent(context))

    fun event(context: Context, event: CalendarEvent, config: ClockWidgetConfig) =
        launchCalendar(
            context,
            Intent(
                    Intent.ACTION_VIEW,
                    ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.eventId),
                )
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.begin.toEpochMilli())
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.end.toEpochMilli()),
            config.calendarPackage,
        )

    fun insert(context: Context, date: LocalDate, config: ClockWidgetConfig) {
        val now = java.time.ZonedDateTime.now()
        val start =
            (if (date == now.toLocalDate())
                    now.truncatedTo(java.time.temporal.ChronoUnit.HOURS).plusHours(1)
                else date.atTime(9, 0).atZone(ZoneId.systemDefault()))
                .toInstant()
                .toEpochMilli()
        launchCalendar(
            context,
            Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, start + 3_600_000),
            config.calendarPackage,
        )
    }

    /** A calendar may handle viewing but not insertion (or may have been removed). */
    private fun launchCalendar(context: Context, intent: Intent, preferredPackage: String?) {
        if (preferredPackage != null) {
            val preferred = Intent(intent).setPackage(preferredPackage)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            // PackageManager queries do not infer a content URI's MIME type themselves.
            val resolvedType = runCatching { preferred.resolveTypeIfNeeded(context.contentResolver) }.getOrNull()
            if (resolvedType != null) preferred.setDataAndType(preferred.data, resolvedType)
            val handler = runCatching { context.packageManager.resolveActivity(preferred, 0) }
                .getOrNull()?.activityInfo
            if (handler != null && handler.exported && runCatching {
                    context.startActivity(preferred)
                }.isSuccess) return
        }
        launch(context, intent)
    }

    fun weather(context: Context, config: ClockWidgetConfig) {
        config.weatherPackage
            ?.let { context.packageManager.getLaunchIntentForPackage(it) }
            ?.let { launch(context, it) }
    }
}
