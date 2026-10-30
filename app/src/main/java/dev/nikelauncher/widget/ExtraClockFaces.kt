// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.widget

import androidx.compose.ui.platform.LocalConfiguration
import android.graphics.Paint
import android.graphics.Typeface
import android.text.format.DateFormat
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
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.sin

internal val extraClockStyles =
    setOf(
        ClockStyle.FINE,
        ClockStyle.CAPSULE,
        ClockStyle.RAIL,
        ClockStyle.RINGS,
        ClockStyle.DOT_MATRIX,
        ClockStyle.ORBIT,
    )

/** Small independent vector faces; no font or image assets are required. */
@Composable
internal fun ExtraClockFace(config: ClockWidgetConfig, now: Instant, modifier: Modifier) {
    val context = LocalContext.current
    val unit = with(LocalDensity.current) { config.timeSizeSp.sp.toPx() }
    val density = LocalDensity.current
    val description = AgendaText.clock(context, now, config.hourFormat)
    val time = now.atZone(ZoneId.systemDefault())
    val is24 =
        when (config.hourFormat) {
            ClockWidgetConfig.HourFormat.SYSTEM -> DateFormat.is24HourFormat(context)
            ClockWidgetConfig.HourFormat.TWELVE -> false
            ClockWidgetConfig.HourFormat.TWENTY_FOUR -> true
        }
    val h = if (is24) time.hour else (time.hour % 12).let { if (it == 0) 12 else it }
    val hours = if (is24) h.toString().padStart(2, '0') else h.toString()
    val minutes = time.minute.toString().padStart(2, '0')
    val period =
        if (is24) ""
        else
            DateTimeFormatter.ofPattern("a", LocalConfiguration.current.locales[0])
                .format(time)
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val p =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = config.colorAt(config.timeOpacity)
                textSize = unit
                typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            }
        val analog = config.style == ClockStyle.ORBIT || config.style == ClockStyle.RINGS
        val base =
            when (config.style) {
                ClockStyle.FINE -> p.measureText(description)
                ClockStyle.CAPSULE -> p.measureText(description) + unit * .55f
                ClockStyle.RAIL -> {
                    val hourPaint =
                        Paint(p).apply { typeface = Typeface.create("sans-serif", Typeface.BOLD) }
                    hourPaint.measureText(hours) + p.measureText(minutes) * .78f + unit * .36f
                }
                ClockStyle.DOT_MATRIX -> (hours.length + minutes.length) * unit * .55f + unit * .25f
                else -> unit * 1.45f
            }
        val markWidth =
            if (
                !analog &&
                    config.style != ClockStyle.FINE &&
                    config.style != ClockStyle.CAPSULE &&
                    period.isNotEmpty()
            ) {
                val m = Paint(p).apply { textSize = unit * .22f }
                m.measureText(period) + unit * .14f
            } else 0f
        val scale = minOf(1f, constraints.maxWidth.toFloat() / (base + markWidth).coerceAtLeast(1f))
        val height = with(density) { (unit * 1.5f * scale).toDp() }
        Canvas(
            Modifier.fillMaxWidth().height(height).semantics { contentDescription = description }
        ) {
            drawIntoCanvas { draw ->
                val c = draw.nativeCanvas
                p.color = config.colorAt(config.timeOpacity)
                p.textSize = unit
                p.typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
                p.style = Paint.Style.FILL
                c.save()
                c.scale(scale, scale)
                when (config.style) {
                    ClockStyle.FINE -> c.drawText(description, 0f, unit * 1.07f, p)
                    ClockStyle.CAPSULE -> {
                        val original = p.color
                        p.alpha = (p.alpha * .13f).toInt()
                        c.drawRoundRect(
                            0f,
                            unit * .08f,
                            base,
                            unit * 1.4f,
                            unit * .66f,
                            unit * .66f,
                            p,
                        )
                        p.color = original
                        p.textSize = unit * .86f
                        p.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                        c.drawText(
                            description,
                            (base - p.measureText(description)) / 2f,
                            unit * 1.02f,
                            p,
                        )
                    }
                    ClockStyle.RAIL -> {
                        p.typeface = Typeface.create("sans-serif", Typeface.BOLD)
                        c.drawText(hours, 0f, unit * 1.02f, p)
                        val x = p.measureText(hours) + unit * .17f
                        p.strokeWidth = unit * .018f
                        c.drawLine(x, unit * .25f, x, unit * 1.15f, p)
                        p.typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
                        p.textSize = unit * .78f
                        c.drawText(minutes, x + unit * .17f, unit * 1.02f, p)
                    }
                    ClockStyle.DOT_MATRIX -> {
                        val digits = hours + minutes
                        for ((index, digit) in digits.withIndex()) {
                            val pattern = dotDigits[digit - '0']
                            val x =
                                index * unit * .55f + if (index >= hours.length) unit * .25f else 0f
                            for (row in 0..6) for (col in 0..4) if (
                                pattern[row] and (1 shl (4 - col)) != 0
                            )
                                c.drawCircle(
                                    x + col * unit * .09f + unit * .04f,
                                    unit * .25f + row * unit * .13f,
                                    unit * .035f,
                                    p,
                                )
                        }
                        val x = hours.length * unit * .55f + unit * .07f
                        c.drawCircle(x, unit * .53f, unit * .035f, p)
                        c.drawCircle(x, unit * .92f, unit * .035f, p)
                    }
                    ClockStyle.RINGS,
                    ClockStyle.ORBIT -> {
                        val r = unit * .65f
                        val cx = unit * .72f
                        val cy = unit * .74f
                        p.style = Paint.Style.STROKE
                        p.strokeWidth = unit * .045f
                        p.strokeCap = Paint.Cap.ROUND
                        val original = p.color
                        if (config.style == ClockStyle.RINGS) {
                            for ((radius, sweep) in
                                listOf(
                                    r to ((time.hour % 12 + time.minute / 60f) / 12f * 360),
                                    r * .73f to (time.minute / 60f * 360),
                                )) {
                                p.alpha = (android.graphics.Color.alpha(original) * .17f).toInt()
                                c.drawCircle(cx, cy, radius, p)
                                p.color = original
                                c.drawArc(
                                    cx - radius,
                                    cy - radius,
                                    cx + radius,
                                    cy + radius,
                                    -90f,
                                    sweep,
                                    false,
                                    p,
                                )
                            }
                            p.style = Paint.Style.FILL
                            p.textSize = unit * .24f
                            p.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                            val text = "$hours:$minutes"
                            c.drawText(text, cx - p.measureText(text) / 2, cy + unit * .08f, p)
                        } else {
                            for (i in 0..11) {
                                val a = i * Math.PI / 6
                                p.style = Paint.Style.FILL
                                c.drawCircle(
                                    cx + sin(a).toFloat() * r,
                                    cy - cos(a).toFloat() * r,
                                    unit * .018f,
                                    p,
                                )
                            }
                            fun hand(angle: Double, length: Float, width: Float) {
                                p.strokeWidth = unit * width
                                c.drawLine(
                                    cx,
                                    cy,
                                    cx + sin(angle).toFloat() * r * length,
                                    cy - cos(angle).toFloat() * r * length,
                                    p,
                                )
                            }
                            hand((time.hour % 12 + time.minute / 60.0) * Math.PI / 6, .5f, .065f)
                            hand(time.minute * Math.PI / 30, .85f, .027f)
                            p.style = Paint.Style.FILL
                            c.drawCircle(cx, cy, unit * .05f, p)
                        }
                    }
                    else -> Unit
                }
                if (markWidth > 0) {
                    p.style = Paint.Style.FILL
                    p.textSize = unit * .22f
                    c.drawText(period, base + unit * .14f, unit * 1.02f, p)
                }
                c.restore()
            }
        }
    }
}

private val dotDigits =
    arrayOf(
        intArrayOf(14, 17, 19, 21, 25, 17, 14),
        intArrayOf(4, 12, 4, 4, 4, 4, 14),
        intArrayOf(14, 17, 1, 2, 4, 8, 31),
        intArrayOf(30, 1, 1, 14, 1, 1, 30),
        intArrayOf(2, 6, 10, 18, 31, 2, 2),
        intArrayOf(31, 16, 16, 30, 1, 1, 30),
        intArrayOf(14, 16, 16, 30, 17, 17, 14),
        intArrayOf(31, 1, 2, 4, 8, 8, 8),
        intArrayOf(14, 17, 17, 14, 17, 17, 14),
        intArrayOf(14, 17, 17, 15, 1, 1, 14),
    )
