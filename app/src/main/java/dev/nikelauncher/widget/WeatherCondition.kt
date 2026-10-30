// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.widget

/** OpenWeather condition codes carried by the existing Gadgetbridge WeatherSpec. */
internal enum class WeatherCondition {
    CLEAR, FEW_CLOUDS, PARTLY_CLOUDY, MOSTLY_CLOUDY, OVERCAST,
    DRIZZLE, RAIN, FREEZING_RAIN, SNOW, SLEET, HAIL, THUNDER,
    FOG, HAZE, DUST, WIND, TORNADO, UNKNOWN,
}

internal fun weatherCondition(code: Int): WeatherCondition = when (code) {
    in 200..299 -> WeatherCondition.THUNDER
    in 300..399 -> WeatherCondition.DRIZZLE
    511 -> WeatherCondition.FREEZING_RAIN
    in 500..599 -> WeatherCondition.RAIN
    // Breezy sends its HAIL condition as 611 and its SLEET condition as 616.
    611 -> WeatherCondition.HAIL
    612, 613 -> WeatherCondition.SLEET
    615, 616 -> WeatherCondition.SLEET
    in 600..699 -> WeatherCondition.SNOW
    701, 741 -> WeatherCondition.FOG
    711, 721 -> WeatherCondition.HAZE
    731, 751, 761, 762 -> WeatherCondition.DUST
    771 -> WeatherCondition.WIND
    781 -> WeatherCondition.TORNADO
    800 -> WeatherCondition.CLEAR
    801 -> WeatherCondition.FEW_CLOUDS
    802 -> WeatherCondition.PARTLY_CLOUDY
    803 -> WeatherCondition.MOSTLY_CLOUDY
    804 -> WeatherCondition.OVERCAST
    else -> WeatherCondition.UNKNOWN
}
