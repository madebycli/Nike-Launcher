// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.widget

import java.time.Instant
import java.util.Locale

/** How this payload was adapted, not an assertion about a broadcast sender's identity. */
enum class WeatherPayloadSource { BRIDGE, BREEZY_PROVIDER }

internal data class WeatherCacheStamp(
    val location: String,
    val updatedAt: Instant,
    val source: WeatherPayloadSource,
)

/** A retransmission cannot extend the source forecast's useful life. Future skew adds no time. */
internal fun weatherDataFresh(now: Instant, receivedAt: Instant, updatedAt: Instant): Boolean {
    val effectiveUpdate = minOf(receivedAt, updatedAt)
    return now >= receivedAt && now < effectiveUpdate.plusSeconds(10_800)
}

/** Equal-time provider data retains the original conditions that the bridge may coarsen. */
internal fun canReplaceWeatherCache(previous: WeatherCacheStamp?, incoming: WeatherCacheStamp): Boolean {
    if (previous == null || canonicalLocation(previous.location) != canonicalLocation(incoming.location))
        return true
    if (incoming.updatedAt < previous.updatedAt) return false
    return incoming.updatedAt != previous.updatedAt ||
        previous.source != WeatherPayloadSource.BREEZY_PROVIDER ||
        incoming.source == WeatherPayloadSource.BREEZY_PROVIDER
}

private fun canonicalLocation(value: String) = value.trim().lowercase(Locale.ROOT)
