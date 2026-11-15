// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.widget

import java.time.Instant
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Apparent solar altitude for a forecast instant, using the sender's coordinates only in RAM.
 * Standard low-order solar-position equations (Julian days, ecliptic longitude, declination,
 * sidereal angle), as also used by solar calculators such as SunCalc/NOAA. The -0.833° horizon
 * includes conventional refraction and the solar disc. This classifies a symbol; it does not
 * invent provider sunrise/set times, retrieve a location or expose coordinates in the cache.
 */
internal fun solarNight(at: Instant, latitude: Double?, longitude: Double?): Boolean? {
    if (latitude == null || longitude == null || !latitude.isFinite() || !longitude.isFinite() ||
        latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return null
    if (at.epochSecond !in 1..4_102_444_800L) return null
    val radians = Math.PI / 180.0
    // J2000 is 2000-01-01T12:00:00Z. All angles depend on the instant, not local clock/DST.
    val days = (at.epochSecond - 946_728_000L + at.nano / 1_000_000_000.0) / 86_400.0
    val meanAnomaly = radians * (357.5291 + .98560028 * days)
    val equation = radians * (1.9148 * sin(meanAnomaly) +
        .0200 * sin(2 * meanAnomaly) + .0003 * sin(3 * meanAnomaly))
    val longitudeEcliptic = meanAnomaly + equation + radians * 102.9372 + Math.PI
    val obliquity = radians * 23.4397
    val declination = asin(sin(longitudeEcliptic) * sin(obliquity))
    val ascension = atan2(sin(longitudeEcliptic) * cos(obliquity), cos(longitudeEcliptic))
    val hourAngle = radians * (280.16 + 360.9856235 * days + longitude) - ascension
    val phi = latitude * radians
    val altitude = asin((sin(phi) * sin(declination) +
        cos(phi) * cos(declination) * cos(hourAngle)).coerceIn(-1.0, 1.0))
    return altitude < -.833 * radians
}
