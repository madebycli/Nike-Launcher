// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.widget

import java.time.Duration
import java.time.Instant

/** A time ratio along a display arc, not a computed solar elevation. */
internal fun daylightProgress(now: Instant, rise: Instant?, set: Instant?): Float? {
    if (rise == null || set == null) return null
    val seconds = Duration.between(rise, set).seconds
    if (seconds !in 1..86400) return null
    return (Duration.between(rise, now).seconds.toDouble() / seconds).toFloat().coerceIn(0f, 1f)
}

/** Closest of eight conventional phases; Gadgetbridge supplies 0=new and 180=full. */
internal fun moonPhaseSector(angle: Int): Int {
    require(angle in 0..360)
    return ((angle % 360 + 22) / 45) % 8
}

internal fun sunWindowNight(
    at: Instant, rise: Instant?, set: Instant?, zone: java.time.ZoneId,
): Boolean? {
    if (rise == null || set == null || daylightProgress(at, rise, set) == null) return null
    if (at.atZone(zone).toLocalDate() != rise.atZone(zone).toLocalDate()) return null
    return at < rise || at >= set
}
