// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.widget

import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/** Reads original classifications from the already installed Breezy app, with its Android grant. */
object BreezyWeatherProvider {
    private val supportedPackages = setOf("org.breezyweather", "org.breezyweather.debug")

    sealed interface Result {
        data object Unsupported : Result
        data class PermissionRequired(val packageName: String, val permission: String) : Result
        data class Unavailable(val reason: Reason) : Result
        data class Ready(
            val payload: String,
            val locationName: String,
            val locationId: String,
            val packageName: String,
        ) : Result
    }

    enum class Reason { MULTIPLE_APPS, INCOMPATIBLE_VERSION, NO_LOCATIONS, LOCATION_AMBIGUOUS, NO_WEATHER, INVALID_DATA, PROVIDER_ERROR }

    internal data class Endpoint(val packageName: String, val authority: String, val permission: String)
    private data class Location(val id: String, val name: String, val aliases: Set<String>, val zone: ZoneId)

    /** Call on an IO dispatcher. No query occurs until Android reports a granted read permission. */
    fun read(
        context: Context,
        configuredPackage: String?,
        matchingLocation: String? = null,
        now: Instant = Instant.now(),
    ): Result {
        if (!configuredPackage.isNullOrBlank() && configuredPackage !in supportedPackages) return Result.Unsupported
        val candidates = (configuredPackage?.takeIf { it.isNotBlank() }?.let(::listOf) ?: supportedPackages.toList())
            .mapNotNull { name ->
                val authority = "$name.provider.weather"
                val provider = context.packageManager.resolveContentProvider(authority, 0)
                if (provider?.packageName == name && provider.exported && provider.enabled &&
                    provider.readPermission == "$name.READ_PROVIDER") Endpoint(name, authority, "$name.READ_PROVIDER")
                else null
            }
        if (candidates.isEmpty()) return Result.Unsupported
        if (candidates.size != 1) return Result.Unavailable(Reason.MULTIPLE_APPS)
        return readEndpoint(context, candidates.single(), matchingLocation, now)
    }

    /** Instrumentation seam; production only enters through the whitelisted owner check above. */
    internal fun readEndpoint(
        context: Context,
        endpoint: Endpoint,
        matchingLocation: String? = null,
        now: Instant = Instant.now(),
    ): Result {
        if (context.checkSelfPermission(endpoint.permission) != PackageManager.PERMISSION_GRANTED)
            return Result.PermissionRequired(endpoint.packageName, endpoint.permission)
        return try {
            val base = Uri.Builder().scheme("content").authority(endpoint.authority).build()
            val resolver = context.contentResolver
            val compatible = resolver.query(base.buildUpon().appendPath("version").build(), null, null, null, null)
                ?.use { cursor ->
                    // Breezy 6.2.2 ships 0.1. Its hourly/current/daily fields are the same
                    // ones consumed here; 0.2 only adds pollenLevels. Do not require an
                    // unreleased Breezy build for the original forecast classifications.
                    cursor.moveToFirst() && cursor.getInt(cursor.getColumnIndexOrThrow("major")) == 0 &&
                        cursor.getInt(cursor.getColumnIndexOrThrow("minor")) in 1..2
                } ?: false
            if (!compatible) return Result.Unavailable(Reason.INCOMPATIBLE_VERSION)
            val locations = resolver.query(base.buildUpon().appendPath("locations").appendQueryParameter("limit", "5").build(),
                null, null, null, null)?.use { cursor ->
                buildList {
                    while (size < 5 && cursor.moveToNext()) {
                        val id = cursor.text("id")?.takeIf { it.isNotBlank() && it.length <= 512 } ?: continue
                        val zone = cursor.text("timezone")?.let { runCatching { ZoneId.of(it) }.getOrNull() } ?: continue
                        val custom = cursor.text("custom_name")?.trim().orEmpty()
                        val city = cursor.text("city")?.trim().orEmpty()
                        val district = cursor.text("district")?.trim().orEmpty()
                        val cityDistrict = listOf(city, district.takeIf { it != city }.orEmpty()).filter { it.isNotBlank() }.joinToString(", ")
                        val name = custom.ifBlank { cityDistrict }.takeIf { it.isNotBlank() } ?: continue
                        add(Location(id, name, setOf(custom, city, cityDistrict, name).filter { it.isNotBlank() }.map(::canonical).toSet(), zone))
                    }
                }
            }.orEmpty()
            if (locations.isEmpty()) return Result.Unavailable(Reason.NO_LOCATIONS)
            val matches = matchingLocation?.takeIf { it.isNotBlank() }?.let { target -> locations.filter { canonical(target) in it.aliases } }.orEmpty()
            val location = when {
                matches.size == 1 -> matches.single()
                matches.size > 1 -> return Result.Unavailable(Reason.LOCATION_AMBIGUOUS)
                locations.size == 1 -> locations.single()
                else -> return Result.Unavailable(Reason.LOCATION_AMBIGUOUS)
            }
            val weatherUri = base.buildUpon().appendPath("weather")
                .appendQueryParameter("withDaily", "true").appendQueryParameter("withHourly", "true")
                .appendQueryParameter("withMinutely", "false").appendQueryParameter("withAlerts", "false")
                .appendQueryParameter("withNormals", "false").appendQueryParameter("temperatureUnit", "k")
                .appendQueryParameter("speedUnit", "kph").appendQueryParameter("distanceUnit", "m")
                .appendQueryParameter("pressureUnit", "hpa").build()
            val blob = resolver.query(weatherUri, null, "id=${location.id}", null, null)?.use { cursor ->
                if (!cursor.moveToFirst() || cursor.text("id") != location.id) return@use null
                val column = cursor.getColumnIndex("weather")
                if (column < 0 || cursor.isNull(column)) null else cursor.getBlob(column)
            } ?: return Result.Unavailable(Reason.NO_WEATHER)
            val payload = BreezyWeatherDecoder.decodeGzip(blob, location.name, location.zone, now)
                ?: return Result.Unavailable(Reason.INVALID_DATA)
            Result.Ready(payload, location.name, location.id, endpoint.packageName)
        } catch (_: SecurityException) {
            Result.PermissionRequired(endpoint.packageName, endpoint.permission)
        } catch (_: Exception) {
            Result.Unavailable(Reason.PROVIDER_ERROR)
        }
    }

    private fun Cursor.text(name: String): String? {
        val column = getColumnIndex(name)
        return if (column < 0 || isNull(column)) null else getString(column)
    }

    private fun canonical(value: String) = value.trim().lowercase(Locale.ROOT)
}
