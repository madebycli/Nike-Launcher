// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.widget

import dev.nikelauncher.data.backup.BackupValues
import android.content.Context

/**
 * What one clock widget has been set to show.
 *
 * Kept per widget id in a file of its own rather than in the launcher's settings: two clocks
 * on the same home screen are two widgets and can be set differently, and a widget's settings
 * belong to the widget — they are reached by its own menu and go with it when it is removed.
 *
 * Plain SharedPreferences because a widget provider is a broadcast receiver: it is asked to
 * draw and must answer now, with no scope to suspend in.
 */
data class ClockWidgetConfig(
    val hourFormat: HourFormat,
    val datePattern: String?,
    val timeSizeSp: Int,
    val dateSizeSp: Int,
    val showBattery: Boolean,
    val batterySizeSp: Int,
    val showWeather: Boolean,
    val weatherSizeSp: Int,
    val weatherFahrenheit: Boolean,
    /** Package the weather row opens, or null for a row that is not a tap target. */
    val weatherPackage: String?,
    val showNotifications: Boolean,
    val notificationsSizeSp: Int,
    val showAgenda: Boolean,
    val agendaSizeSp: Int,
    /** How many upcoming events to show, at most. */
    val agendaCount: Int,
    /** How far ahead to look, in hours. */
    val agendaLookaheadHours: Int,
    /** Count down inside the last hour; the visible native widget updates at each boundary. */
    val agendaRelative: Boolean,
    val showAlarm: Boolean,
    val textColor: Int,
    val timeOpacity: Int,
    val dateOpacity: Int,
    val batteryOpacity: Int,
    val weatherOpacity: Int,
    val notificationsOpacity: Int,
    val agendaOpacity: Int,
    val style: ClockStyle = ClockStyle.CLASSIC,
    val excludedCalendars: Set<String> = emptySet(),
    /** Internal spacing; independent from the home layout's widget top/bottom padding. */
    val clockDataGapDp: Int = 8,
    val textWidthPercent: Int = 100,
    /** Optional app choices; null keeps Android's normal action resolution. */
    val clockPackage: String? = null,
    val calendarPackage: String? = null,
) : java.io.Serializable {
    enum class HourFormat { SYSTEM, TWELVE, TWENTY_FOUR }

    /** The color as one row should draw it, since each row is faded on its own. */
    fun colorAt(opacity: Int): Int = opacityOf(textColor, opacity)

    companion object {
        private const val FILE = "clock_widget"
        private const val DEFAULT_SIZE_SP = 60
        private const val DEFAULT_DATE_SIZE_SP = 16
        private const val DEFAULT_BATTERY_SIZE_SP = 16
        private const val DEFAULT_ROW_SIZE_SP = 16
        private const val DEFAULT_COLOR = 0xFFFFFFFF.toInt()
        private const val DEFAULT_OPACITY = 100

        /** Weekday, day, month — short enough for a narrow widget and clear in any language. */
        const val DEFAULT_DATE = "EEE, d MMM"

        /**
         * Whether a pattern is one TextClock can be handed.
         *
         * Checked before it is ever stored. A widget is drawn inside another app's process, so
         * a pattern that throws does not fail here where someone could see why — it fails over
         * there, as a widget that will not draw, with nothing to say which setting did it.
         */
        fun isValidDatePattern(pattern: String): Boolean {
            if (pattern.isBlank() || pattern.length > MAX_DATE_PATTERN) return false
            return runCatching {
                android.text.format.DateFormat.format(pattern, java.util.Date()).toString()
            }.getOrNull()?.isNotEmpty() == true
        }

        /** Long enough for any real format and short enough not to be a essay. */
        const val MAX_DATE_PATTERN = 40

        /** Offered in the widget's own settings; the pattern is what TextClock is given. */
        val DATE_CHOICES = listOf(
            "EEE, d MMM",
            "EEEE, d MMMM",
            "d MMM yyyy",
            "dd.MM.yyyy",
            "MM/dd/yyyy",
        )

        /** Small steps, since the right size depends on the height the widget was given. */
        val SIZE_RANGE = 16..160
        val DATE_SIZE_RANGE = 10..72
        val CLOCK_DATA_GAP_RANGE = 0..96
        val TEXT_WIDTH_RANGE = 25..100

        /** Stops short of nothing at all: an invisible row looks like a broken widget. */
        val OPACITY_RANGE = 10..100

        /** More than three and the widget is an agenda rather than a clock. */
        val AGENDA_COUNT_RANGE = 1..3

        /** Preserve the existing one-hour to one-week control; the full agenda is independent. */
        val AGENDA_HOURS_RANGE = 1..168

        /** Shared with the settings preview, so what is shown there is what is drawn. */
        fun opacityOf(color: Int, opacity: Int): Int {
            val alpha = (opacity.coerceIn(OPACITY_RANGE) * 255 / 100) shl 24
            return alpha or (color and 0x00FFFFFF)
        }

        fun read(context: Context, widgetId: Int): ClockWidgetConfig {
            val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            val hour = runCatching {
                HourFormat.valueOf(prefs.getString(key("hour", widgetId), null) ?: HourFormat.SYSTEM.name)
            }.getOrDefault(HourFormat.SYSTEM)
            // An absent key is the default; the empty string is the deliberate choice of no
            // date at all, which is why one is not stored as the other.
            val stored = prefs.getString(key("date", widgetId), null) ?: DEFAULT_DATE
            return ClockWidgetConfig(
                style = ClockStyle.entries.firstOrNull { it.name == prefs.getString(key("style", widgetId), null) }
                    ?: if (prefs.contains(key("size", widgetId))) ClockStyle.CLASSIC else ClockStyle.SPLIT,
                excludedCalendars = prefs.getStringSet(key("excludedCalendars", widgetId), emptySet()).orEmpty().toSet(),
                hourFormat = hour,
                datePattern = stored.takeIf { it.isNotEmpty() }?.let { if (isValidDatePattern(it)) it else DEFAULT_DATE },
                timeSizeSp = prefs.getInt(key("size", widgetId), DEFAULT_SIZE_SP).coerceIn(SIZE_RANGE),
                clockDataGapDp = prefs.getInt(key("clockDataGap", widgetId), 8).coerceIn(CLOCK_DATA_GAP_RANGE),
                textWidthPercent = prefs.getInt(key("textWidth", widgetId), 100).coerceIn(TEXT_WIDTH_RANGE),
                dateSizeSp = prefs.getInt(key("dateSize", widgetId), DEFAULT_DATE_SIZE_SP).coerceIn(DATE_SIZE_RANGE),
                showBattery = prefs.getBoolean(key("battery", widgetId), false),
                showWeather = prefs.getBoolean(key("weather", widgetId), false),
                weatherSizeSp = prefs.getInt(key("weatherSize", widgetId), DEFAULT_ROW_SIZE_SP).coerceIn(DATE_SIZE_RANGE),
                // Asked of the locale only the first time, so changing it later does not
                // silently undo a choice made by hand.
                weatherFahrenheit = prefs.getBoolean(
                    key("weatherF", widgetId),
                    ClockWidgetWeather.fahrenheitByDefault(),
                ),
                // Asked for rather than guessed: Android has a category for the calendar app and
                // the music app and no such thing for weather, and the broadcast does not say
                // who sent it. A list of package names we happened to think of would be wrong
                // for whoever is not on it.
                weatherPackage = prefs.getString(key("weatherPkg", widgetId), null),
                clockPackage = prefs.getString(key("clockPkg", widgetId), null),
                calendarPackage = prefs.getString(key("calendarPkg", widgetId), null),
                showNotifications = prefs.getBoolean(key("notifications", widgetId), false),
                showAgenda = prefs.getBoolean(key("agenda", widgetId), false),
                agendaSizeSp = prefs.getInt(key("agendaSize", widgetId), DEFAULT_ROW_SIZE_SP).coerceIn(DATE_SIZE_RANGE),
                agendaCount = prefs.getInt(key("agendaCount", widgetId), 1).coerceIn(AGENDA_COUNT_RANGE),
                agendaLookaheadHours = prefs.getInt(
                    key("agendaHours", widgetId),
                    if (prefs.contains(key("size", widgetId))) 24 else 12,
                ).coerceIn(AGENDA_HOURS_RANGE),
                agendaRelative = prefs.getBoolean(key("agendaRelative", widgetId), true),
                showAlarm = prefs.getBoolean(key("alarm", widgetId), false),
                notificationsSizeSp = prefs.getInt(key("notificationsSize", widgetId), DEFAULT_ROW_SIZE_SP).coerceIn(DATE_SIZE_RANGE),
                batterySizeSp = prefs.getInt(key("batterySize", widgetId), DEFAULT_BATTERY_SIZE_SP).coerceIn(DATE_SIZE_RANGE),
                textColor = prefs.getInt(key("color", widgetId), DEFAULT_COLOR),
                timeOpacity = prefs.getInt(key("timeOpacity", widgetId), DEFAULT_OPACITY),
                dateOpacity = prefs.getInt(key("dateOpacity", widgetId), DEFAULT_OPACITY),
                batteryOpacity = prefs.getInt(key("batteryOpacity", widgetId), DEFAULT_OPACITY),
                weatherOpacity = prefs.getInt(key("weatherOpacity", widgetId), DEFAULT_OPACITY),
                notificationsOpacity = prefs.getInt(key("notificationsOpacity", widgetId), DEFAULT_OPACITY),
                agendaOpacity = prefs.getInt(key("agendaOpacity", widgetId), DEFAULT_OPACITY),
            )
        }

        fun hasConfiguration(context: Context, widgetId: Int): Boolean =
            context.getSharedPreferences(FILE, Context.MODE_PRIVATE).contains(key("hour", widgetId))

        fun write(context: Context, widgetId: Int, config: ClockWidgetConfig) {
            if (!config.showAgenda || read(context, widgetId).excludedCalendars != config.excludedCalendars)
                CalendarSnapshotCache.shared.forget(widgetId)
            restoreBackup(context, widgetId, storedValues(config), synchronous = false)
        }

        private fun storedValues(config: ClockWidgetConfig): Map<String, Any> = linkedMapOf<String, Any?>(
            "style" to config.style.name,
            "excludedCalendars" to config.excludedCalendars,
            "hour" to config.hourFormat.name,
            "date" to config.datePattern.orEmpty(),
            "size" to config.timeSizeSp,
            "clockDataGap" to config.clockDataGapDp.coerceIn(CLOCK_DATA_GAP_RANGE),
            "textWidth" to config.textWidthPercent.coerceIn(TEXT_WIDTH_RANGE),
            "dateSize" to config.dateSizeSp,
            "battery" to config.showBattery,
            "weather" to config.showWeather,
            "weatherSize" to config.weatherSizeSp,
            "weatherF" to config.weatherFahrenheit,
            "weatherPkg" to config.weatherPackage,
            "clockPkg" to config.clockPackage,
            "calendarPkg" to config.calendarPackage,
            "notifications" to config.showNotifications,
            "agenda" to config.showAgenda,
            "agendaSize" to config.agendaSizeSp,
            "agendaCount" to config.agendaCount,
            "agendaHours" to config.agendaLookaheadHours,
            "agendaRelative" to config.agendaRelative,
            "alarm" to config.showAlarm,
            "notificationsSize" to config.notificationsSizeSp,
            "batterySize" to config.batterySizeSp,
            "color" to config.textColor,
            "timeOpacity" to config.timeOpacity,
            "dateOpacity" to config.dateOpacity,
            "batteryOpacity" to config.batteryOpacity,
            "weatherOpacity" to config.weatherOpacity,
            "notificationsOpacity" to config.notificationsOpacity,
            "agendaOpacity" to config.agendaOpacity,
        ).filterValues { it != null }.mapValues { it.value!! }

        /** Shares the normal-write field map, so new clock options join backups automatically. */
        internal fun backupValues(context: Context, widgetId: Int): Map<String, Any> =
            storedValues(read(context, widgetId))

        internal fun validateBackup(context: Context, values: org.json.JSONObject): Map<String, Any> {
            val types = storedValues(read(context, -1)).mapValues { (_, value) ->
                when (value) {
                    is String -> "string"
                    is Boolean -> "boolean"
                    is Int -> "int"
                    is Set<*> -> "stringSet"
                    else -> error("Unsupported clock option")
                }
            } + mapOf("weatherPkg" to "string", "clockPkg" to "string", "calendarPkg" to "string")
            val result = dev.nikelauncher.data.backup.BackupValues.decode(values, types)
            (result["date"] as? String)?.let { require(it.isEmpty() || isValidDatePattern(it)) }
            return result
        }

        internal fun restoreBackup(
            context: Context,
            widgetId: Int,
            values: Map<String, Any>,
            synchronous: Boolean = true,
            replaceExisting: Boolean = false,
        ) {
            CalendarSnapshotCache.shared.forget(widgetId)
            val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            // A newly allocated Android widget ID may have belonged to a deleted widget before.
            // Older backups do not contain fields introduced later, so merely overwriting the
            // supplied fields would inherit those stale values instead of their normal defaults.
            // Remove this ID's entire old snapshot in the same transaction as the restored one.
            if (replaceExisting) prefs.all.keys.filter { it.endsWith("_$widgetId") }.forEach(editor::remove)
            values.forEach { (name, value) ->
                when (value) {
                    is String -> editor.putString(key(name, widgetId), value)
                    is Boolean -> editor.putBoolean(key(name, widgetId), value)
                    is Int -> editor.putInt(key(name, widgetId), value)
                    is Set<*> -> editor.putStringSet(key(name, widgetId), value.filterIsInstance<String>().toSet())
                    else -> error("Invalid clock option")
                }
            }
            listOf("weatherPkg", "clockPkg", "calendarPkg").filterNot(values::containsKey)
                .forEach { editor.remove(key(it, widgetId)) }
            if (synchronous) check(editor.commit()) else editor.apply()
        }

        /** Dropped when the widget is, so a later widget cannot inherit its id's settings. */
        fun forget(context: Context, widgetId: Int) {
            CalendarSnapshotCache.shared.forget(widgetId)
            context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
                .remove(key("style", widgetId))
                .remove(key("excludedCalendars", widgetId))
                .remove(key("hour", widgetId))
                .remove(key("date", widgetId))
                .remove(key("size", widgetId))
                .remove(key("clockDataGap", widgetId))
                .remove(key("textWidth", widgetId))
                .remove(key("dateSize", widgetId))
                .remove(key("battery", widgetId))
                .remove(key("batterySize", widgetId))
                .remove(key("color", widgetId))
                .remove(key("timeOpacity", widgetId))
                .remove(key("dateOpacity", widgetId))
                .remove(key("batteryOpacity", widgetId))
                .remove(key("weather", widgetId))
                .remove(key("weatherSize", widgetId))
                .remove(key("weatherF", widgetId))
                .remove(key("weatherPkg", widgetId))
                .remove(key("clockPkg", widgetId))
                .remove(key("calendarPkg", widgetId))
                .remove(key("weatherOpacity", widgetId))
                .remove(key("notifications", widgetId))
                .remove(key("notificationsSize", widgetId))
                .remove(key("notificationsOpacity", widgetId))
                .remove(key("agenda", widgetId))
                .remove(key("agendaSize", widgetId))
                .remove(key("agendaCount", widgetId))
                .remove(key("agendaHours", widgetId))
                .remove(key("agendaRelative", widgetId))
                .remove(key("agendaOpacity", widgetId))
                .remove(key("alarm", widgetId))
                .apply()
        }

        private fun key(name: String, widgetId: Int) = name + "_" + widgetId
    }
}
