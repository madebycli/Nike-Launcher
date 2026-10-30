// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.screens

import android.Manifest
import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.CalendarContract
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowContentResolver

/** Two invented calendars; no device calendar data is read by the render tests. */
internal fun installFakeCalendars(application: Application) {
    shadowOf(application).grantPermissions(Manifest.permission.READ_CALENDAR)
    ShadowContentResolver.registerProviderInternal(CalendarContract.AUTHORITY, FakeCalendarProvider())
}

private class FakeCalendarProvider : ContentProvider() {
    override fun onCreate() = true
    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
        val columns = projection ?: emptyArray()
        return MatrixCursor(columns).apply {
            if (uri.path == "/calendars") {
                listOf("Personal", "Work").forEachIndexed { index, name ->
                    addRow(columns.map { column -> when (column) {
                        "_id" -> index + 1
                        "calendar_displayName" -> name
                        "calendar_color" -> 0xFF10707F.toInt()
                        else -> null
                    } })
                }
            }
        }
    }
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
}
