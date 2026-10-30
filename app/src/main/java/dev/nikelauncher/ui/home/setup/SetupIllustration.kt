// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.setup

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import dev.nikelauncher.ui.kit.motion.AppMotion
import dev.nikelauncher.ui.kit.motion.MotionConstants
import dev.nikelauncher.ui.kit.motion.motionEnabled
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope

/** How many favorites the drawing holds. */
private const val ILLUSTRATION_ROWS = 4

/**
 * A phone held upright, showing the launcher the way it is: the time and the day's agenda up top,
 * and below them a plain list of favorites. It is a drawing of the idea rather than a screenshot
 * of it, so it never goes out of date with a setting.
 *
 * The rows arrive one after another. With [settled] the favorites sit centred in the screen, the
 * way a first run holds them, instead of straight under the clock.
 */
@Composable
internal fun SetupIllustration(settled: Boolean, modifier: Modifier = Modifier) {
    val rows = remember { List(ILLUSTRATION_ROWS) { Animatable(0f) } }
    val arrive = AppMotion.emphasized<Float>()
    val settle = AppMotion.spatial<Float>()
    val shift = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        coroutineScope {
            rows.forEachIndexed { index, row ->
                launch {
                    if (motionEnabled()) delay(MotionConstants.STAGGER_DELAY_MS * 2L * index + 120)
                    row.animateTo(1f, arrive)
                }
            }
        }
    }
    LaunchedEffect(settled) { shift.animateTo(if (settled) 1f else 0f, settle) }

    val scheme = MaterialTheme.colorScheme
    val measurer = rememberTextMeasurer()
    val frame = scheme.onSurface.copy(alpha = 0.82f)
    val screenTop = scheme.primaryContainer.copy(alpha = 0.55f)
    val screenBottom = scheme.surfaceContainerLow
    val ink = scheme.onSurface
    val accents = listOf(scheme.primary, scheme.tertiary, scheme.secondary)

    Canvas(modifier.aspectRatio(0.52f)) {
        val w = size.width
        val h = size.height
        val corner = w * 0.16f
        val bezel = w * 0.028f

        // The glass, then the frame around it.
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(screenTop, screenBottom)),
            cornerRadius = CornerRadius(corner),
            size = Size(w, h),
        )
        drawRoundRect(
            color = frame,
            cornerRadius = CornerRadius(corner),
            style = Stroke(width = bezel),
        )
        // Camera cut-out.
        drawRoundRect(
            color = frame,
            topLeft = Offset(w * 0.5f - w * 0.07f, h * 0.022f),
            size = Size(w * 0.14f, h * 0.018f),
            cornerRadius = CornerRadius(h * 0.009f),
        )

        // Time and date, held at the top where the clock sits.
        val time = measurer.measure(
            "9:41",
            TextStyle(fontSize = (h * 0.082f).toSp(), fontWeight = FontWeight.Bold, color = ink),
        )
        drawText(time, topLeft = Offset(w * 0.1f, h * 0.075f))
        drawRoundRect(
            color = ink.copy(alpha = 0.42f),
            topLeft = Offset(w * 0.1f, h * 0.075f + time.size.height + h * 0.012f),
            size = Size(w * 0.34f, h * 0.012f),
            cornerRadius = CornerRadius(h * 0.006f),
        )
        drawRoundRect(
            color = ink.copy(alpha = 0.22f),
            topLeft = Offset(w * 0.1f, h * 0.075f + time.size.height + h * 0.034f),
            size = Size(w * 0.5f, h * 0.012f),
            cornerRadius = CornerRadius(h * 0.006f),
        )

        // The favorites: first straight under the clock, then, settled, in the middle.
        val rowHeight = h * 0.074f
        val rowGap = h * 0.022f
        val listHeight = ILLUSTRATION_ROWS * rowHeight + (ILLUSTRATION_ROWS - 1) * rowGap
        val flowTop = h * 0.285f
        val centredTop = (h - listHeight) / 2f + h * 0.085f
        val top = flowTop + (centredTop - flowTop) * shift.value
        rows.forEachIndexed { index, progress ->
            val p = progress.value
            if (p <= 0f) return@forEachIndexed
            val y = top + index * (rowHeight + rowGap) + (1f - p) * h * 0.03f
            val icon = rowHeight * 0.86f
            val accent = accents[index % accents.size]
            drawRoundRect(
                color = accent.copy(alpha = 0.9f * p.coerceIn(0f, 1f)),
                topLeft = Offset(w * 0.1f, y + (rowHeight - icon) / 2f),
                size = Size(icon, icon),
                cornerRadius = CornerRadius(icon * 0.3f),
            )
            val label = w * (0.28f + 0.12f * ((index * 7) % 3))
            drawRoundRect(
                color = ink.copy(alpha = 0.58f * p.coerceIn(0f, 1f)),
                topLeft = Offset(w * 0.1f + icon + w * 0.05f, y + rowHeight * 0.34f),
                size = Size(label, rowHeight * 0.3f),
                cornerRadius = CornerRadius(rowHeight * 0.15f),
            )
        }
    }
}
