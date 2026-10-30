// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.widget

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.zip.GZIPInputStream
import kotlin.math.roundToInt
import org.json.JSONArray
import org.json.JSONObject

/** Adapter for the verified forecast fields of Breezy data-sharing 0.1/0.2, not a weather source. */
internal object BreezyWeatherDecoder {
    private const val MAX_COMPRESSED = 524_288
    private const val MAX_EXPANDED = 1_048_576

    fun decodeGzip(bytes: ByteArray, locationName: String, zone: ZoneId, now: Instant): String? {
        if (bytes.isEmpty() || bytes.size > MAX_COMPRESSED) return null
        return runCatching {
            val output = ByteArrayOutputStream()
            GZIPInputStream(ByteArrayInputStream(bytes)).use { input ->
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    if (output.size() + count > MAX_EXPANDED) return null
                    output.write(buffer, 0, count)
                }
            }
            decode(output.toString(Charsets.UTF_8.name()), locationName, zone, now)
        }.getOrNull()
    }

    fun decode(json: String, locationName: String, zone: ZoneId, now: Instant): String? {
        if (json.length > MAX_EXPANDED) return null
        return runCatching {
            val source = JSONObject(json)
            val refreshed = millis(source, "refreshTime") ?: return null
            if (refreshed < now.minusSeconds(10_800) || refreshed > now.plusSeconds(300)) return null
            val current = source.optJSONObject("current") ?: return null
            val currentTemp = temperature(current) ?: return null
            val today = now.atZone(zone).toLocalDate()
            val result = JSONObject()
                .put("timestamp", refreshed.epochSecond)
                .put("currentTemp", currentTemp)
                .put("currentCondition", current.optString("weatherText").take(40))
                .put("currentConditionCode", conditionCode(current.optString("weatherCode")))
                .put("location", locationName.take(80))
                .put("_gridZone", zone.id)
                .put("_gridTodayDate", today.toString())
            metrics(current, result)
            val hours = JSONArray()
            val hourly = source.optJSONArray("hourly")
            for (i in 0 until minOf(hourly?.length() ?: 0, 168)) {
                val hour = hourly?.optJSONObject(i) ?: continue
                val at = millis(hour, "date") ?: continue
                val temp = temperature(hour) ?: continue
                if (at < refreshed.minusSeconds(3600) || at >= refreshed.plusSeconds(604_800)) continue
                val normalized = JSONObject().put("timestamp", at.epochSecond).put("temp", temp)
                    .put("conditionCode", conditionCode(hour.optString("weatherCode")))
                // Kotlin serialization omits isDaylight=true, its documented default in
                // both supported contracts. Explicit malformed values remain unknown.
                val daylight = if (!hour.has("isDaylight")) true else hour.opt("isDaylight") as? Boolean
                daylight?.let { normalized.put("_gridProviderNight", !it) }
                probability(hour)?.let { normalized.put("precipProbability", it) }
                metrics(hour, normalized)
                hours.put(normalized)
            }
            result.put("hourly", hours)
            val forecasts = JSONArray()
            val days = source.optJSONArray("daily")
            val seen = mutableSetOf<LocalDate>()
            for (i in 0 until minOf(days?.length() ?: 0, 16)) {
                val day = days?.optJSONObject(i) ?: continue
                val date = millis(day, "date")?.atZone(zone)?.toLocalDate() ?: continue
                if (date < today || date > today.plusDays(14) || !seen.add(date)) continue
                val daytime = day.optJSONObject("day") ?: continue
                val nighttime = day.optJSONObject("night") ?: continue
                val dayTemp = temperature(daytime) ?: continue
                val nightTemp = temperature(nighttime) ?: continue
                val low = minOf(dayTemp, nightTemp)
                val high = maxOf(dayTemp, nightTemp)
                val code = conditionCode(daytime.optString("weatherCode"))
                val rain = listOfNotNull(probability(daytime), probability(nighttime)).maxOrNull()
                if (date == today) {
                    result.put("todayMinTemp", low).put("todayMaxTemp", high)
                        .put("_gridTodayConditionCode", code)
                    rain?.let { result.put("precipProbability", it) }
                } else {
                    val normalized = JSONObject().put("_gridDate", date.toString())
                        .put("minTemp", low).put("maxTemp", high).put("conditionCode", code)
                    rain?.let { normalized.put("precipProbability", it) }
                    dailyMetrics(day, normalized)
                    metrics(daytime, normalized)
                    forecasts.put(normalized)
                }
            }
            result.put("forecasts", forecasts)
            result.toString().takeIf { it.length <= 65_536 }
        }.getOrNull()
    }

    /** Original WeatherCode identifiers, before Gadgetbridge's cloud-cover quantization. */
    internal fun conditionCode(code: String): Int = when (code) {
        "clear" -> 800
        "partly_cloudy" -> 802
        "cloudy" -> 804
        "rain" -> 500
        "snow" -> 600
        "wind" -> 771
        "fog" -> 741
        "haze" -> 721
        "sleet" -> 616
        "hail" -> 611
        "thunder" -> 210
        "thunderstorm" -> 211
        else -> 3200
    }

    private fun millis(obj: JSONObject, key: String): Instant? {
        val value = obj.opt(key) as? Number ?: return null
        val number = value.toDouble()
        if (!number.isFinite() || number % 1.0 != 0.0 || number !in 1.0..4_102_444_800_000.0) return null
        return Instant.ofEpochMilli(number.toLong())
    }

    private fun unit(obj: JSONObject?, key: String, expected: String, min: Double, max: Double): Double? {
        val value = obj?.optJSONObject(key) ?: return null
        if (value.optString("unit") != expected) return null
        val number = (value.opt("value") as? Number)?.toDouble() ?: return null
        return number.takeIf { it.isFinite() && it in min..max }
    }

    private fun temperature(obj: JSONObject): Int? =
        unit(obj.optJSONObject("temperature"), "temperature", "k", 150.0, 350.0)?.roundToInt()

    private fun probability(obj: JSONObject): Int? =
        unit(obj.optJSONObject("precipitationProbability"), "total", "percent", 0.0, 100.0)?.roundToInt()

    private fun metrics(source: JSONObject, target: JSONObject) {
        fun copy(key: String, output: String, expected: String, min: Double, max: Double) {
            unit(source, key, expected, min, max)?.let {
                target.put(output, if (output in setOf("humidity", "cloudCover", "dewPoint")) it.roundToInt() else it)
            }
        }
        copy("relativeHumidity", "humidity", "percent", 0.0, 100.0)
        copy("cloudCover", "cloudCover", "percent", 0.0, 100.0)
        copy("pressure", "pressure", "hpa", 300.0, 1200.0)
        copy("visibility", "visibility", "m", 0.0, 1_000_000.0)
        copy("dewPoint", "dewPoint", "k", 150.0, 350.0)
        copy("uV", "uvIndex", "uvi", 0.0, 50.0)
        unit(source.optJSONObject("temperature"), "sourceFeelsLike", "k", 150.0, 350.0)
            ?.let { target.put("feelsLikeTemp", it.roundToInt()) }
        val wind = source.optJSONObject("wind")
        unit(wind, "speed", "kph", 0.0, 500.0)?.let { target.put("windSpeed", it) }
        (wind?.opt("degree") as? Number)?.toDouble()?.takeIf { it.isFinite() && it in 0.0..360.0 }
            ?.let { target.put("windDirection", it.roundToInt()) }
        airQuality(source, target)
    }

    private fun dailyMetrics(source: JSONObject, target: JSONObject) {
        listOf(
            Triple("relativeHumidity", "humidity", "percent"),
            Triple("cloudCover", "cloudCover", "percent"),
            Triple("pressure", "pressure", "hpa"),
            Triple("visibility", "visibility", "m"),
            Triple("dewPoint", "dewPoint", "k"),
        ).forEach { (input, output, expected) ->
            val max = when (expected) { "hpa" -> 1200.0; "m" -> 1_000_000.0; "k" -> 350.0; else -> 100.0 }
            val min = when (expected) { "hpa" -> 300.0; "k" -> 150.0; else -> 0.0 }
            unit(source.optJSONObject(input), "avg", expected, min, max)?.let {
                target.put(output, if (output in setOf("humidity", "cloudCover", "dewPoint")) it.roundToInt() else it)
            }
        }
        unit(source, "uV", "uvi", 0.0, 50.0)?.let { target.put("uvIndex", it) }
        airQuality(source, target)
    }

    private fun airQuality(source: JSONObject, target: JSONObject) {
        val air = source.optJSONObject("airQuality") ?: return
        val normalized = JSONObject()
        fun index(source: JSONObject, key: String) {
            unit(source, "index", "aqi", 0.0, 1000.0)?.takeIf { it % 1.0 == 0.0 }
                ?.let { normalized.put(key, it.toInt()) }
        }
        index(air, "aqi")
        val pollutants = air.optJSONObject("pollutants")
        listOf("co", "no2", "o3", "pm10", "pm25", "so2").forEach { id ->
            val pollutant = pollutants?.optJSONObject(id) ?: return@forEach
            index(pollutant, id + "Aqi")
            val concentration = unit(pollutant, "concentration", "microgpcum", 0.0, 100_000.0)
                ?: unit(pollutant, "concentration", "mgpcum", 0.0, 100.0)?.times(1000.0)
            concentration?.takeIf { it.isFinite() && it in 0.0..100_000.0 }
                ?.let { normalized.put(id, it) }
        }
        if (normalized.length() > 0) target.put("airQuality", normalized)
    }
}
