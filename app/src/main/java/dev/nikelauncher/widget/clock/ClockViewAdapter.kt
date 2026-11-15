// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.widget.clock

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import dev.nikelauncher.R
import dev.nikelauncher.ui.theme.WidgetTypefaces
import dev.nikelauncher.widget.AgendaText
import dev.nikelauncher.widget.ClockStyle
import dev.nikelauncher.widget.ClockWidgetConfig
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlin.math.ceil

internal val nativeClockStyles = setOf(ClockStyle.SPLIT, ClockStyle.SERIF, ClockStyle.CLASSIC,
    ClockStyle.LINES, ClockStyle.PIXEL, ClockStyle.GLITCH)

/** Victoria Grid's own clock view. Measures glyphs with platform font metrics and fits its width. */
internal class ClockViewAdapter(context: Context) : View(context) {
    private val large = Paint(Paint.ANTI_ALIAS_FLAG)
    private val small = Paint(Paint.ANTI_ALIAS_FLAG)
    private var first = ""
    private var second = ""
    private var split = false
    private var desiredWidth = 1f
    private var desiredHeight = 1f
    private var baseline = 0f
    private var secondBaseline = 0f
    private var secondX = 0f

    fun bind(config: ClockWidgetConfig, now: Instant, sizePx: Float, is24Hour: Boolean) {
        split = config.style == ClockStyle.SPLIT || config.style == ClockStyle.SERIF
        val face = if (config.style == ClockStyle.SERIF) android.graphics.Typeface.create("serif", android.graphics.Typeface.BOLD)
        else WidgetTypefaces.get(context, when (config.style) {
            ClockStyle.SPLIT -> R.font.lexend
            ClockStyle.LINES -> R.font.monoton
            ClockStyle.PIXEL -> R.font.press_start_2p
            ClockStyle.GLITCH -> R.font.rubik_glitch
            else -> R.font.kumbh_sans_semibold
        }, if (config.style == ClockStyle.SPLIT) 800 else if (config.style == ClockStyle.CLASSIC) 600 else 400)
        large.typeface = face
        large.textSize = sizePx
        large.color = config.colorAt(config.timeOpacity)
        small.set(large)
        small.textSize = sizePx * .7f
        val time = now.atZone(ZoneId.systemDefault())
        val hour = if (is24Hour) time.hour else ((time.hour + 11) % 12) + 1
        first = if (split) String.format(Locale.getDefault(), "%02d", hour) else AgendaText.clock(context, now, config.hourFormat)
        second = if (split) String.format(Locale.getDefault(), "%02d", time.minute) else ""
        val mainMetrics = large.fontMetrics
        val smallMetrics = small.fontMetrics
        baseline = -mainMetrics.ascent
        secondBaseline = baseline + sizePx / 6f
        secondX = large.measureText(first) + sizePx / 15f
        desiredWidth = if (split) secondX + small.measureText(second) else large.measureText(first)
        desiredHeight = maxOf(baseline + mainMetrics.descent, if (split) secondBaseline + smallMetrics.descent else 0f)
        contentDescription = AgendaText.clock(context, now, config.hourFormat)
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val available = if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) desiredWidth
            else MeasureSpec.getSize(widthMeasureSpec).toFloat()
        val scale = minOf(1f, available / desiredWidth.coerceAtLeast(1f))
        setMeasuredDimension(resolveSize(ceil(desiredWidth * scale).toInt(), widthMeasureSpec),
            resolveSize(ceil(desiredHeight * scale).toInt(), heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val save = canvas.save()
        canvas.scale(minOf(1f, width / desiredWidth.coerceAtLeast(1f)), minOf(1f, width / desiredWidth.coerceAtLeast(1f)))
        canvas.drawText(first, 0f, baseline, large)
        if (split) canvas.drawText(second, secondX, secondBaseline, small)
        canvas.restoreToCount(save)
    }
}

@Composable
internal fun NativeClockFace(config: ClockWidgetConfig, now: Instant, is24Hour: Boolean, modifier: Modifier) {
    val size = with(LocalDensity.current) { config.timeSizeSp.sp.toPx() }
    AndroidView(modifier = modifier, factory = { ClockViewAdapter(it) },
        update = { it.bind(config, now, size, is24Hour) })
}
