// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.notifications

import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import java.util.Locale

/** Platform-localized compact age; follows Victoria Grid preview.12's minute/hour/day boundaries. */
internal fun notificationAge(locale: Locale, time: Long, now: Long): String {
    val minutes = (now - time).coerceAtLeast(0) / 60_000
    val (value, unit) = when {
        minutes < 60 -> minutes to MeasureUnit.MINUTE
        minutes < 1440 -> minutes / 60 to MeasureUnit.HOUR
        else -> minutes / 1440 to MeasureUnit.DAY
    }
    return MeasureFormat.getInstance(locale, MeasureFormat.FormatWidth.SHORT).format(Measure(value, unit))
}
