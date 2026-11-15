// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.widget

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import java.util.Locale
import java.time.Instant
import java.time.ZoneId

/**
 * The last weather another app sent us, and whether it is still worth drawing.
 *
 * Victoria has no network permission and is not getting one, so there is no fetching here: a
 * weather app that already has the forecast broadcasts it, and this is where what arrived is
 * kept. Nothing asks for a location either — the sender says where it is.
 *
 * Written to disk rather than held in memory because a widget outlives the process that drew
 * it. A launcher gets killed often, and weather that vanishes every time it does is worse than
 * no weather at all.
 */
object ClockWidgetWeather {

    private const val FILE = "clock_widget_weather"
    private const val KEY_TEMP_K = "tempK"
    private const val KEY_CONDITION = "condition"
    private const val KEY_RECEIVED_AT = "receivedAt"
    private const val KEY_UPDATED_AT = "forecastUpdatedAt"
    private const val KEY_SOURCE = "forecastSource"

    /** Kelvin, and wide enough for anywhere on Earth without accepting a parse gone wrong. */
    private val PLAUSIBLE_K = 150..350

    /** Enough for "Partly cloudy"; past this something is wrong with the sender, not the sky. */
    private const val MAX_CONDITION_CHARS = 40

    data class Reading(val tempK: Int, val condition: String)

    /** Null when nothing has arrived, or when what did is too old to repeat. */
    fun current(context: Context): Reading? {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val tempK = prefs.getInt(KEY_TEMP_K, 0)
        if (tempK !in PLAUSIBLE_K) return null
        val receivedAt = prefs.getLong(KEY_RECEIVED_AT, 0L)
        if (receivedAt <= 0L) return null
        val updatedAt = storedUpdatedAt(prefs, receivedAt)
        if (!weatherDataFresh(Instant.now(), Instant.ofEpochMilli(receivedAt), Instant.ofEpochMilli(updatedAt)))
            return null
        return Reading(tempK, prefs.getString(KEY_CONDITION, null).orEmpty())
    }

    /** Old caches already retained the source timestamp; use it before the next broadcast too. */
    private fun storedUpdatedAt(prefs: android.content.SharedPreferences, receivedAt: Long): Long {
        if (prefs.contains(KEY_UPDATED_AT)) return prefs.getLong(KEY_UPDATED_AT, receivedAt)
        val json = prefs.getString("forecast", null) ?: return receivedAt
        return runCatching { org.json.JSONObject(json).optLong("timestamp", 0L) }.getOrNull()
            ?.takeIf { it in 1..4_102_444_800L }?.times(1000L) ?: receivedAt
    }

    /** Returns false for anything that does not look like a temperature, having stored nothing. */
    @Synchronized
    fun store(context: Context, tempK: Int, condition: String?): Boolean {
        if (tempK !in PLAUSIBLE_K) return false
        val now = System.currentTimeMillis()
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putInt(KEY_TEMP_K, tempK)
            .putString(KEY_CONDITION, condition?.take(MAX_CONDITION_CHARS)?.trim().orEmpty())
            .putLong(KEY_RECEIVED_AT, now).putLong(KEY_UPDATED_AT, now)
            .apply()
        return true
    }

    fun snapshot(context: Context): WeatherSnapshot? {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val received = prefs.getLong(KEY_RECEIVED_AT, 0L)
        val json = prefs.getString("forecast", null) ?: return null
        val zone = runCatching { ZoneId.of(prefs.getString("forecastZone", "UTC")) }.getOrDefault(ZoneId.of("UTC"))
        return WeatherPayload.parse(json, Instant.ofEpochMilli(received), zone)
            ?.takeIf { it.fresh(Instant.now()) }
    }

    /** The source records the adapter used; it does not authenticate a broadcast's author. */
    fun source(context: Context): WeatherPayloadSource = readSource(
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE))

    private fun readSource(prefs: android.content.SharedPreferences): WeatherPayloadSource =
        runCatching { WeatherPayloadSource.valueOf(prefs.getString(KEY_SOURCE, null).orEmpty()) }
            .getOrDefault(WeatherPayloadSource.BRIDGE)

    @Synchronized
    fun storePayload(
        context: Context,
        json: String,
        source: WeatherPayloadSource = WeatherPayloadSource.BRIDGE,
    ): Boolean {
        val received = Instant.now()
        val zone = ZoneId.systemDefault()
        val initial = WeatherPayload.parse(json, received, zone) ?: return false
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val previousReceived = prefs.getLong(KEY_RECEIVED_AT, 0L)
        val previousSnapshot = prefs.getString("forecast", null)?.takeIf { previousReceived > 0L }?.let { oldJson ->
            val previousZone = runCatching { ZoneId.of(prefs.getString("forecastZone", "UTC")) }
                .getOrDefault(ZoneId.of("UTC"))
            WeatherPayload.parse(oldJson, Instant.ofEpochMilli(previousReceived), previousZone)
        }
        val normalized = if (source == WeatherPayloadSource.BREEZY_PROVIDER)
            WeatherPayload.withSameLocationAstronomy(json, previousSnapshot) else json
        val parsed = if (normalized == json) initial else WeatherPayload.parse(normalized, received, zone) ?: return false
        val retained = runCatching { WeatherPayload.retainedJson(normalized) }.getOrNull() ?: return false
        val previous = previousSnapshot?.let { WeatherCacheStamp(it.location, it.updatedAt, readSource(prefs)) }
        if (!canReplaceWeatherCache(previous, WeatherCacheStamp(parsed.location, parsed.updatedAt, source)))
            return false
        prefs.edit()
            .putInt(KEY_TEMP_K, parsed.tempK).putString(KEY_CONDITION, parsed.condition)
            .putLong(KEY_RECEIVED_AT, received.toEpochMilli())
            .putLong(KEY_UPDATED_AT, parsed.updatedAt.toEpochMilli()).putString(KEY_SOURCE, source.name)
            .putString("forecast", retained).putString("forecastZone", zone.id).apply()
        return true
    }

    fun degrees(kelvin: Int, fahrenheit: Boolean): String {
        val celsius = kelvin - 273.15
        return String.format(Locale.getDefault(), "%.0f°", if (fahrenheit) celsius * 9 / 5 + 32 else celsius)
    }

    fun formatted(reading: Reading, fahrenheit: Boolean): String {
        val celsius = reading.tempK - 273.15
        val value = if (fahrenheit) celsius * 9 / 5 + 32 else celsius
        val unit = if (fahrenheit) "F" else "C"
        val degrees = String.format(Locale.getDefault(), "%.0f°%s", value, unit)
        return if (reading.condition.isEmpty()) degrees else "$degrees  ${reading.condition}"
    }

    /** Where Fahrenheit is what people mean by a temperature, so the first draw is not wrong. */
    fun fahrenheitByDefault(): Boolean =
        Locale.getDefault().country in setOf("US", "LR", "MM", "BS", "BZ", "KY", "PW")

    /**
     * Turns the receiver on, or off again once no widget wants weather.
     *
     * It ships disabled. A receiver another app can reach is surface that should not exist
     * while the feature is switched off, and this is the one way to have none at all rather
     * than some that is merely careful.
     */
    fun setReceiverEnabled(context: Context, enabled: Boolean) {
        runCatching {
            context.packageManager.setComponentEnabledSetting(
                ComponentName(context, ClockWidgetWeatherReceiver::class.java),
                if (enabled) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                } else {
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                },
                PackageManager.DONT_KILL_APP,
            )
        }
    }
}
