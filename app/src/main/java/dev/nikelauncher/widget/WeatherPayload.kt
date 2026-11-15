// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.widget

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import org.json.JSONObject

data class WeatherMetrics(
    val humidity: Int? = null,
    val windKph: Float? = null,
    val windDirection: Int? = null,
    val uv: Float? = null,
    val pressureHpa: Float? = null,
    val cloudCover: Int? = null,
    val dewPointK: Int? = null,
    val visibilityM: Float? = null,
    val feelsLikeK: Int? = null,
    val airQuality: WeatherAirQuality? = null,
)

data class WeatherAirQuality(
    val index: Int?,
    val pollutants: Map<String, Float>,
    val indices: Map<String, Int>,
)

data class WeatherHour(
    val at: Instant, val tempK: Int, val code: Int, val rain: Int?,
    val metrics: WeatherMetrics = WeatherMetrics(),
    /** Explicit provider daylight, then sun window, otherwise transient solar geometry. */
    val night: Boolean? = null,
    val providerNight: Boolean? = null,
)

data class WeatherDay(
    val date: LocalDate,
    val minK: Int,
    val maxK: Int,
    val code: Int,
    val rain: Int?,
    val metrics: WeatherMetrics = WeatherMetrics(),
    val sunrise: Instant? = null,
    val sunset: Instant? = null,
    val moonrise: Instant? = null,
    val moonset: Instant? = null,
    val moonPhase: Int? = null,
)

data class WeatherSnapshot(
    val receivedAt: Instant,
    val tempK: Int,
    val condition: String,
    val code: Int,
    val location: String,
    val sunrise: Instant?,
    val sunset: Instant?,
    val hours: List<WeatherHour>,
    val days: List<WeatherDay>,
    val metrics: WeatherMetrics = WeatherMetrics(),
    val moonrise: Instant? = null,
    val moonset: Instant? = null,
    val moonPhase: Int? = null,
    val rain: Int? = null,
    /** Timestamp of the provider forecast; a repeated broadcast does not make it newer. */
    val updatedAt: Instant = receivedAt,
    val zone: ZoneId? = null,
) {
    fun fresh(now: Instant): Boolean = weatherDataFresh(now, receivedAt, updatedAt)
}

/** The existing Gadgetbridge broadcast, bounded before parsing. No network or location access. */
object WeatherPayload {
    /** Keep only fields used by this feature; coordinates and unrelated sender data are dropped. */
    private val metricFields = listOf(
        "currentHumidity", "humidity", "windSpeed", "windDirection", "uvIndex", "dewPoint",
        "pressure", "cloudCover", "visibility", "feelsLikeTemp",
        "moonRise", "moonSet", "moonPhase", "sunRise", "sunSet",
    )
    private val pollutants = listOf("co", "no2", "o3", "pm10", "pm25", "so2")
    // Local cache metadata only. Persist the computed classification, never sender coordinates.
    private const val NIGHT_CACHE = "_gridNight"
    private const val PROVIDER_NIGHT = "_gridProviderNight"

    private fun localDate(obj: JSONObject, key: String): LocalDate? =
        runCatching { LocalDate.parse(obj.optString(key)) }.getOrNull()

    private fun providerZone(obj: JSONObject): ZoneId? =
        runCatching { ZoneId.of(obj.optString("_gridZone")) }.getOrNull()

    /** Provider 0.2 omits astronomy. Reuse only this location's explicitly dated bridge values. */
    internal fun withSameLocationAstronomy(json: String, previous: WeatherSnapshot?): String {
        if (previous == null) return json
        return runCatching {
            val obj = JSONObject(json)
            fun canonical(value: String) = value.trim().lowercase(Locale.ROOT)
            if (canonical(obj.optString("location")) != canonical(previous.location)) return json
            val zone = providerZone(obj) ?: return json
            val today = localDate(obj, "_gridTodayDate") ?: return json
            fun copyDay(target: JSONObject, date: LocalDate, day: WeatherDay?) {
                val rise = day?.sunrise
                val set = day?.sunset
                if (rise != null && set != null && rise < set &&
                    rise.atZone(zone).toLocalDate() == date && set.atZone(zone).toLocalDate() == date) {
                    target.put("sunRise", rise.epochSecond).put("sunSet", set.epochSecond)
                }
                day?.moonrise?.takeIf { it.atZone(zone).toLocalDate() == date }?.let { target.put("moonRise", it.epochSecond) }
                day?.moonset?.takeIf { it.atZone(zone).toLocalDate() == date }?.let { target.put("moonSet", it.epochSecond) }
                if (day?.moonPhase != null) target.put("moonPhase", day.moonPhase)
            }
            val currentDay = previous.days.firstOrNull { it.date == today }
                ?: WeatherDay(today, previous.tempK, previous.tempK, previous.code, null,
                    sunrise = previous.sunrise, sunset = previous.sunset,
                    moonrise = previous.moonrise, moonset = previous.moonset,
                    moonPhase = previous.moonPhase.takeIf { previous.updatedAt.atZone(zone).toLocalDate() == today })
            copyDay(obj, today, currentDay)
            val forecasts = obj.optJSONArray("forecasts")
            for (i in 0 until (forecasts?.length() ?: 0)) {
                val day = forecasts?.optJSONObject(i) ?: continue
                val date = localDate(day, "_gridDate") ?: continue
                copyDay(day, date, previous.days.firstOrNull { it.date == date })
            }
            obj.toString()
        }.getOrDefault(json)
    }

    private fun coordinate(obj: JSONObject, key: String, min: Double, max: Double): Double? =
        obj.optDouble(key, Double.NaN).takeIf { it.isFinite() && it in min..max }

    private fun hourNight(item: JSONObject, root: JSONObject): Boolean? =
        (item.opt(PROVIDER_NIGHT) as? Boolean) ?: epoch(item, "timestamp")?.let { at ->
            solarNight(at, coordinate(root, "latitude", -90.0, 90.0),
                coordinate(root, "longitude", -180.0, 180.0))
        } ?: (item.opt(NIGHT_CACHE) as? Boolean)

    private fun retainMetrics(source: JSONObject, target: JSONObject) {
        metricFields.forEach { key ->
            source.optDouble(key, Double.NaN).takeIf { it.isFinite() }?.let { target.put(key, it) }
        }
        source.optJSONObject("airQuality")?.let { aq ->
            val clean = JSONObject()
            (listOf("aqi") + pollutants + pollutants.map { it + "Aqi" }).forEach {
                aq.optDouble(it, Double.NaN).takeIf { value -> value.isFinite() }?.let { value -> clean.put(it, value) }
            }
            target.put("airQuality", clean)
        }
    }

    fun retainedJson(json: String): String {
        val source = JSONObject(json)
        val result = JSONObject()
        for (key in
            listOf(
                "timestamp",
                "currentTemp",
                "currentCondition",
                "currentConditionCode",
                "location",
                "sunRise",
                "sunSet",
                "todayMinTemp",
                "todayMaxTemp",
                "precipProbability",
            )) {
            if (source.has(key)) result.put(key, source.get(key))
        }
        retainMetrics(source, result)
        providerZone(source)?.let { result.put("_gridZone", it.id) }
        localDate(source, "_gridTodayDate")?.let { result.put("_gridTodayDate", it.toString()) }
        integer(source, "_gridTodayConditionCode", 0..9999)?.let { result.put("_gridTodayConditionCode", it) }
        for ((name, fields) in
            listOf(
                "hourly" to listOf("timestamp", "temp", "conditionCode", "precipProbability"),
                "forecasts" to listOf("minTemp", "maxTemp", "conditionCode", "precipProbability"),
            )) {
            val items = source.optJSONArray(name) ?: continue
            val kept = org.json.JSONArray()
            for (i in 0 until minOf(items.length(), if (name == "hourly") 168 else 14)) {
                val item = items.optJSONObject(i)
                val clean = JSONObject()
                fields.forEach { field ->
                    if (item?.has(field) == true) { clean.put(field, item.get(field)) }
                }
                if (item != null) {
                    retainMetrics(item, clean)
                    if (name == "hourly") {
                        hourNight(item, source)?.let { clean.put(NIGHT_CACHE, it) }
                        (item.opt(PROVIDER_NIGHT) as? Boolean)?.let { clean.put(PROVIDER_NIGHT, it) }
                    } else localDate(item, "_gridDate")?.let { clean.put("_gridDate", it.toString()) }
                }
                kept.put(clean)
            }
            result.put(name, kept)
        }
        return result.toString()
    }

    fun parse(json: String, receivedAt: Instant, zone: ZoneId): WeatherSnapshot? {
        if (json.length > 65_536) return null
        return runCatching {
                val obj = JSONObject(json)
                val suppliedZone = providerZone(obj)
                val weatherZone = suppliedZone ?: zone
                val temperature = obj.optInt("currentTemp")
                if (temperature !in 150..350) return null
                val sent = epoch(obj, "timestamp") ?: receivedAt
                // Do not revive an old forecast because a broadcaster retransmitted it.
                if (sent > receivedAt.plusSeconds(300) || sent < receivedAt.minusSeconds(10_800))
                    return null
                val senderDate = sent.atZone(weatherZone).toLocalDate()
                val today = localDate(obj, "_gridTodayDate")?.takeIf {
                    it >= senderDate.minusDays(1) && it <= senderDate.plusDays(1)
                } ?: senderDate
                fun probability(item: JSONObject) =
                    if (item.has("precipProbability")) {
                        item.optInt("precipProbability", -1).takeIf { it in 0..100 }
                    } else null
                val hours =
                    buildList {
                            val array = obj.optJSONArray("hourly") ?: return@buildList
                            for (i in 0 until minOf(array.length(), 168)) {
                                val item = array.optJSONObject(i) ?: continue
                                val at = epoch(item, "timestamp") ?: continue
                                val temp = item.optInt("temp")
                                if (
                                    temp in 150..350 &&
                                        at >= sent.minusSeconds(3600) &&
                                        at < sent.plusSeconds(604_800)
                                ) {
                                    add(
                                        WeatherHour(
                                            at,
                                            temp,
                                            item.optInt("conditionCode", 3200),
                                            probability(item),
                                            metrics(item),
                                            night = hourNight(item, obj),
                                            providerNight = item.opt(PROVIDER_NIGHT) as? Boolean,
                                        )
                                    )
                                }
                            }
                        }
                        .distinctBy { it.at }
                        .sortedBy { it.at }
                val days = buildList {
                    val low = obj.optInt("todayMinTemp")
                    val high = obj.optInt("todayMaxTemp")
                    if (low in 150..350 && high in low..350)
                        add(
                            WeatherDay(
                                today,
                                low,
                                high,
                                integer(obj, "_gridTodayConditionCode", 0..9999)
                                    ?: obj.optInt("currentConditionCode", 3200),
                                probability(obj), metrics(obj),
                                epoch(obj, "sunRise"), epoch(obj, "sunSet"),
                                epoch(obj, "moonRise"), epoch(obj, "moonSet"),
                                integer(obj, "moonPhase", 0..360),
                            )
                        )
                    val array = obj.optJSONArray("forecasts") ?: return@buildList
                    // WeatherSpec forecasts begin tomorrow and carry no date. Anchor once to
                    // the sender's date, never to the date on which the widget is redrawn.
                    for (i in 0 until minOf(array.length(), 14)) {
                        val item = array.optJSONObject(i) ?: continue
                        val min = item.optInt("minTemp")
                        val max = item.optInt("maxTemp")
                        val date = if (item.has("_gridDate")) localDate(item, "_gridDate")
                            ?.takeIf { it > today && it <= today.plusDays(14) } ?: continue
                        else today.plusDays(i + 1L)
                        if (min in 150..350 && max in min..350)
                            add(
                                WeatherDay(
                                    date,
                                    min,
                                    max,
                                    item.optInt("conditionCode", 3200),
                                    probability(item), metrics(item),
                                    epoch(item, "sunRise"), epoch(item, "sunSet"),
                                    epoch(item, "moonRise"), epoch(item, "moonSet"),
                                    integer(item, "moonPhase", 0..360),
                                )
                            )
                    }
                }
                WeatherSnapshot(
                    receivedAt,
                    temperature,
                    obj.optString("currentCondition").take(40).trim(),
                    obj.optInt("currentConditionCode", 3200),
                    obj.optString("location").take(80).trim(),
                    epoch(obj, "sunRise"),
                    epoch(obj, "sunSet"),
                    hours.map { hour ->
                        val date = hour.at.atZone(weatherZone).toLocalDate()
                        val day = days.firstOrNull { it.date == date }
                        val fromProvider = sunWindowNight(hour.at,
                            day?.sunrise ?: epoch(obj, "sunRise"),
                            day?.sunset ?: epoch(obj, "sunSet"), weatherZone)
                        hour.copy(night = hour.providerNight ?: fromProvider ?: hour.night)
                    },
                    days, metrics(obj),
                    epoch(obj, "moonRise"), epoch(obj, "moonSet"),
                    integer(obj, "moonPhase", 0..360), probability(obj), sent, suppliedZone,
                )
            }
            .getOrNull()
    }

    // Units follow Breezy's GadgetbridgeService: km/h, hPa, metres, Kelvin.
    private fun integer(obj: JSONObject, key: String, range: IntRange): Int? =
        if (obj.has(key) && !obj.isNull(key)) obj.optDouble(key, Double.NaN)
            .takeIf { it.isFinite() && it % 1.0 == 0.0 && it >= range.first && it <= range.last }?.toInt()
        else null

    private fun number(obj: JSONObject, key: String, max: Float): Float? =
        if (obj.has(key) && !obj.isNull(key)) obj.optDouble(key, Double.NaN).toFloat()
            .takeIf { it.isFinite() && it >= 0f && it <= max }
        else null

    private fun metrics(obj: JSONObject): WeatherMetrics {
        val aq = obj.optJSONObject("airQuality")
        val quality = aq?.let {
            WeatherAirQuality(integer(it, "aqi", 0..1000),
                pollutants.mapNotNull { key -> number(it, key, 100000f)?.let { key to it } }.toMap(),
                pollutants.mapNotNull { key -> integer(it, key + "Aqi", 0..1000)?.let { key to it } }.toMap())
        }?.takeIf { it.index != null || it.pollutants.isNotEmpty() || it.indices.isNotEmpty() }
        return WeatherMetrics(
            humidity = integer(obj, "currentHumidity", 0..100) ?: integer(obj, "humidity", 0..100),
            windKph = number(obj, "windSpeed", 500f),
            windDirection = integer(obj, "windDirection", 0..360),
            uv = number(obj, "uvIndex", 50f),
            pressureHpa = number(obj, "pressure", 1200f)?.takeIf { it >= 300f },
            cloudCover = integer(obj, "cloudCover", 0..100),
            dewPointK = integer(obj, "dewPoint", 150..350),
            visibilityM = number(obj, "visibility", 1000000f),
            feelsLikeK = integer(obj, "feelsLikeTemp", 150..350),
            airQuality = quality,
        )
    }

    private fun epoch(obj: JSONObject, key: String): Instant? =
        obj.optLong(key, 0).takeIf { it in 1..4_102_444_800L }?.let(Instant::ofEpochSecond)
}
