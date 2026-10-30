// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.widget

import androidx.compose.ui.platform.LocalConfiguration
import android.graphics.Paint
import android.graphics.Typeface
import android.text.format.DateFormat
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.sp
import dev.nikelauncher.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

enum class ClockStyle(@StringRes val label: Int) {
    CLASSIC(R.string.clock_style_classic),
    SPLIT(R.string.clock_style_split),
    OVERLAP(R.string.clock_style_overlap),
    VERTICAL(R.string.clock_style_vertical),
    FLIP(R.string.clock_style_flip),
    ANALOG(R.string.clock_style_analog),
    WEEKDAY(R.string.clock_style_weekday),
    MINIMAL(R.string.clock_style_minimal),
    SERIF(R.string.clock_style_serif),
    OUTLINE(R.string.clock_style_outline),
    LINES(R.string.clock_style_lines),
    PIXEL(R.string.clock_style_pixel),
    GLITCH(R.string.clock_style_glitch),
    NONE(R.string.clock_style_none),
    FINE(R.string.clock_style_fine),
    CAPSULE(R.string.clock_style_capsule),
    RAIL(R.string.clock_style_rail),
    RINGS(R.string.clock_style_rings),
    DOT_MATRIX(R.string.clock_style_dot_matrix),
    ORBIT(R.string.clock_style_orbit),
}

/** The same face draws in the widget and in every style preview. */
@Composable
fun ClockFace(config: ClockWidgetConfig, now: Instant, modifier: Modifier = Modifier) {
    if (config.style == ClockStyle.NONE) return
    if (config.style in extraClockStyles) {
        ExtraClockFace(config, now, modifier)
        return
    }
    val context = LocalContext.current
    val density = LocalDensity.current
    val tall = config.style == ClockStyle.VERTICAL
    val unit = with(density) { config.timeSizeSp.sp.toPx() }
    val description = AgendaText.clock(context, now, config.hourFormat)
    val is24 =
        when (config.hourFormat) {
            ClockWidgetConfig.HourFormat.SYSTEM -> DateFormat.is24HourFormat(context)
            ClockWidgetConfig.HourFormat.TWELVE -> false
            ClockWidgetConfig.HourFormat.TWENTY_FOUR -> true
        }
    if (config.style in dev.nikelauncher.widget.clock.nativeClockStyles) {
        dev.nikelauncher.widget.clock.NativeClockFace(config, now, is24, modifier.fillMaxWidth())
        return
    }
    val time = now.atZone(ZoneId.systemDefault())
    val locale = LocalConfiguration.current.locales[0]
    val hour = DateTimeFormatter.ofPattern(if (is24) "HH" else "h", locale).format(time)
    val minute = DateTimeFormatter.ofPattern("mm", locale).format(time)
    val separatePeriod =
        !is24 &&
            config.style in
                setOf(
                    ClockStyle.SPLIT,
                    ClockStyle.SERIF,
                    ClockStyle.OUTLINE,
                    ClockStyle.OVERLAP,
                    ClockStyle.VERTICAL,
                    ClockStyle.FLIP,
                    ClockStyle.MINIMAL,
                )
    val period = if (separatePeriod) DateTimeFormatter.ofPattern("a", locale).format(time) else ""
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = config.colorAt(config.timeOpacity)
                typeface =
                    Typeface.create(
                        if (config.style == ClockStyle.SERIF) "serif" else "sans-serif",
                        if (
                            config.style in
                                setOf(ClockStyle.CLASSIC, ClockStyle.MINIMAL, ClockStyle.WEEKDAY)
                        )
                            Typeface.NORMAL
                        else Typeface.BOLD,
                    )
                textSize = unit
            }
        val split =
            config.style in
                setOf(ClockStyle.SPLIT, ClockStyle.SERIF, ClockStyle.OUTLINE, ClockStyle.OVERLAP)
        val full = if (config.style == ClockStyle.MINIMAL) "$hour $minute" else description
        val faceWidth =
            when {
                config.style == ClockStyle.ANALOG -> unit * 1.4f
                config.style == ClockStyle.PIXEL -> {
                    paint.typeface = Typeface.MONOSPACE
                    paint.measureText(full)
                }
                tall -> maxOf(paint.measureText(hour), paint.measureText(minute))
                split -> paint.measureText(hour) + paint.measureText(minute) * .7f + unit * .07f
                config.style == ClockStyle.WEEKDAY -> paint.measureText(full) + unit * 1.4f
                config.style == ClockStyle.FLIP ->
                    2 * maxOf(paint.measureText(hour), paint.measureText(minute)) + unit * .46f
                config.style == ClockStyle.LINES -> paint.measureText(full) + unit * .08f
                else -> paint.measureText(full)
            }
        val needed =
            faceWidth +
                if (separatePeriod) {
                    val periodPaint = Paint(paint).apply { textSize = unit * .24f }
                    periodPaint.measureText(period) + unit * .12f
                } else 0f
        val scale = min(1f, constraints.maxWidth.toFloat() / needed.coerceAtLeast(1f))
        val height = with(density) { (unit * (if (tall) 2.4f else 1.45f) * scale).toDp() }
        Canvas(
            Modifier.fillMaxWidth().height(height).semantics { contentDescription = description }
        ) {
            drawIntoCanvas { compose ->
                val canvas = compose.nativeCanvas
                // Drawing changes the paint; restore the measured base on each frame.
                paint.color = config.colorAt(config.timeOpacity)
                paint.textSize = unit
                paint.style = Paint.Style.FILL
                paint.typeface =
                    Typeface.create(
                        if (config.style == ClockStyle.SERIF) "serif" else "sans-serif",
                        if (
                            config.style in
                                setOf(ClockStyle.CLASSIC, ClockStyle.MINIMAL, ClockStyle.WEEKDAY)
                        )
                            Typeface.NORMAL
                        else Typeface.BOLD,
                    )
                canvas.save()
                canvas.scale(scale, scale)
                val baseline = unit * 1.05f
                when (config.style) {
                    ClockStyle.ANALOG -> {
                        val r = unit * .63f
                        val cx = r + unit * .04f
                        val cy = unit * .72f
                        paint.style = Paint.Style.STROKE
                        paint.strokeWidth = unit * .025f
                        canvas.drawCircle(cx, cy, r, paint)
                        for (i in 0..11) {
                            val angle = i * Math.PI / 6
                            canvas.drawLine(
                                cx + cos(angle).toFloat() * r * .86f,
                                cy + sin(angle).toFloat() * r * .86f,
                                cx + cos(angle).toFloat() * r * .94f,
                                cy + sin(angle).toFloat() * r * .94f,
                                paint,
                            )
                        }
                        fun hand(angle: Double, length: Float, width: Float) {
                            paint.strokeWidth = unit * width
                            paint.strokeCap = Paint.Cap.ROUND
                            canvas.drawLine(
                                cx,
                                cy,
                                cx + sin(angle).toFloat() * r * length,
                                cy - cos(angle).toFloat() * r * length,
                                paint,
                            )
                        }
                        hand((time.hour % 12 + time.minute / 60.0) * Math.PI / 6, .52f, .055f)
                        hand(time.minute * Math.PI / 30, .78f, .035f)
                    }
                    ClockStyle.VERTICAL -> {
                        canvas.drawText(hour, 0f, baseline, paint)
                        canvas.drawText(minute, 0f, baseline + unit * 1.05f, paint)
                    }
                    ClockStyle.FLIP -> {
                        val color = paint.color
                        val width =
                            maxOf(paint.measureText(hour), paint.measureText(minute)) + unit * .17f
                        for ((i, text) in listOf(hour, minute).withIndex()) {
                            val x = i * (width + unit * .12f)
                            paint.color =
                                android.graphics.Color.argb(
                                    45,
                                    android.graphics.Color.red(color),
                                    android.graphics.Color.green(color),
                                    android.graphics.Color.blue(color),
                                )
                            canvas.drawRoundRect(
                                x,
                                unit * .08f,
                                x + width,
                                unit * 1.3f,
                                unit * .1f,
                                unit * .1f,
                                paint,
                            )
                            paint.color = color
                            canvas.drawText(text, x + unit * .085f, baseline, paint)
                            paint.strokeWidth = unit * .012f
                            canvas.drawLine(x, unit * .7f, x + width, unit * .7f, paint)
                        }
                    }
                    ClockStyle.WEEKDAY -> {
                        canvas.drawText(full, 0f, baseline, paint)
                        val x = paint.measureText(full) + unit * .15f
                        paint.textSize = unit * .28f
                        canvas.drawText(
                            time.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, locale),
                            x,
                            baseline,
                            paint,
                        )
                    }
                    ClockStyle.SPLIT,
                    ClockStyle.SERIF,
                    ClockStyle.OUTLINE,
                    ClockStyle.OVERLAP -> {
                        if (config.style == ClockStyle.OUTLINE) {
                            paint.style = Paint.Style.STROKE
                            paint.strokeWidth = unit * .017f
                        }
                        canvas.drawText(hour, 0f, baseline - unit * .17f, paint)
                        val width = paint.measureText(hour)
                        paint.textSize = unit * .7f
                        if (config.style == ClockStyle.OVERLAP) paint.alpha = paint.alpha * 65 / 100
                        canvas.drawText(
                            minute,
                            width + unit * if (config.style == ClockStyle.OVERLAP) -.2f else .07f,
                            baseline,
                            paint,
                        )
                    }
                    ClockStyle.LINES -> {
                        val path = android.graphics.Path()
                        paint.getTextPath(full, 0, full.length, 0f, baseline, path)
                        paint.style = Paint.Style.STROKE
                        paint.strokeWidth = unit * .012f
                        canvas.drawPath(path, paint)
                        for (offset in listOf(.035f, .07f)) {
                            canvas.save()
                            canvas.translate(unit * offset, 0f)
                            canvas.drawPath(path, paint)
                            canvas.restore()
                        }
                    }
                    ClockStyle.PIXEL -> {
                        paint.typeface = Typeface.MONOSPACE
                        // Rasterize our system glyphs at a low resolution, then draw square pixels.
                        val w = (needed / unit * 18).toInt().coerceAtLeast(1)
                        val bitmap =
                            android.graphics.Bitmap.createBitmap(
                                w,
                                25,
                                android.graphics.Bitmap.Config.ALPHA_8,
                            )
                        val small =
                            Paint().apply {
                                textSize = 18f
                                typeface = Typeface.MONOSPACE
                                color = android.graphics.Color.WHITE
                            }
                        android.graphics.Canvas(bitmap).drawText(full, 0f, 19f, small)
                        for (y in 0 until 25) for (x in 0 until w) if (
                            android.graphics.Color.alpha(bitmap.getPixel(x, y)) > 128
                        ) {
                            canvas.drawRect(
                                x * unit / 18,
                                y * unit / 18,
                                (x + 1) * unit / 18,
                                (y + 1) * unit / 18,
                                paint,
                            )
                        }
                        bitmap.recycle()
                    }
                    ClockStyle.GLITCH -> {
                        for (i in 0..7) {
                            canvas.save()
                            canvas.clipRect(
                                -unit * .06f,
                                unit * i / 7,
                                needed + unit * .06f,
                                unit * (i + 1) / 7,
                            )
                            canvas.drawText(
                                full,
                                if (i % 3 == 0) unit * .04f else 0f,
                                baseline,
                                paint,
                            )
                            canvas.restore()
                        }
                    }
                    else -> canvas.drawText(full, 0f, baseline, paint)
                }
                if (separatePeriod) {
                    paint.style = Paint.Style.FILL
                    paint.color = config.colorAt(config.timeOpacity)
                    paint.textSize = unit * .24f
                    canvas.drawText(
                        period,
                        faceWidth + unit * .12f,
                        if (tall) baseline + unit * 1.05f else baseline,
                        paint,
                    )
                }
                canvas.restore()
            }
        }
    }
}
