// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.widget

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import dev.nikelauncher.R
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/** Distinct, untinted glyphs shared by the current, hourly and daily weather views. */
@Composable
internal fun WeatherConditionIcon(
    code: Int,
    night: Boolean = false,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val description = contentDescription?.let {
        stringResource(R.string.weather_condition_period, it,
            stringResource(if (night) R.string.weather_nighttime else R.string.weather_daytime))
    }
    Icon(weatherConditionVector(code, night), description,
        modifier.testTag("weather-condition-$code-${if (night) "night" else "day"}"), tint = Color.Unspecified)
}

// The small, fixed condition set bounds this cache; recomposition never rebuilds paths.
private val conditionVectors: Map<Pair<WeatherCondition, Boolean>, Lazy<ImageVector>> =
    WeatherCondition.entries.flatMap { condition ->
        listOf(false, true).map { night ->
            (condition to night) to lazy { buildConditionVector(condition, night) }
        }
    }.toMap()

internal fun weatherConditionVector(code: Int, night: Boolean): ImageVector =
    conditionVectors.getValue(weatherCondition(code) to night).value

private val cloudLight = Color(0xFFE3EBF2)
private val cloudDark = Color(0xFF879BAB)
private val rainBlue = Color(0xFF62ABF5)
private val snowBlue = Color(0xFFB9DFFC)
private val sunGold = Color(0xFFFFCE57)
private val moonBlue = Color(0xFFB9D5FF)

private fun ImageVector.Builder.fill(color: Color, block: PathBuilder.() -> Unit) {
    path(fill = SolidColor(color), pathFillType = PathFillType.NonZero, pathBuilder = block)
}

private fun PathBuilder.circle(x: Float, y: Float, radius: Float) {
    val k = radius * .5522848f
    moveTo(x + radius, y)
    curveTo(x + radius, y + k, x + k, y + radius, x, y + radius)
    curveTo(x - k, y + radius, x - radius, y + k, x - radius, y)
    curveTo(x - radius, y - k, x - k, y - radius, x, y - radius)
    curveTo(x + k, y - radius, x + radius, y - k, x + radius, y)
    close()
}

private fun ImageVector.Builder.sun(x: Float, y: Float, radius: Float) {
    fill(sunGold) {
        circle(x, y, radius)
        repeat(8) { index ->
            val a = index * Math.PI / 4
            val ux = cos(a).toFloat()
            val uy = sin(a).toFloat()
            val inner = radius + 2f
            val outer = radius + 5f
            moveTo(x + ux * inner - uy, y + uy * inner + ux)
            lineTo(x + ux * outer - uy, y + uy * outer + ux)
            lineTo(x + ux * outer + uy, y + uy * outer - ux)
            lineTo(x + ux * inner + uy, y + uy * inner - ux)
            close()
        }
    }
    fill(Color(0xFFFFE48C)) { circle(x - radius * .2f, y - radius * .2f, radius * .65f) }
}

private fun ImageVector.Builder.moon() {
    fill(moonBlue) {
        moveTo(28f, 5f)
        curveTo(17f, 4f, 8f, 13f, 8f, 24f)
        curveTo(8f, 35f, 17f, 43f, 28f, 43f)
        curveTo(34f, 43f, 39f, 40f, 42f, 35f)
        curveTo(31f, 38f, 21f, 30f, 21f, 19f)
        curveTo(21f, 13f, 24f, 8f, 28f, 5f)
        close()
    }
}

private fun ImageVector.Builder.cloud(color: Color = cloudLight, shiftY: Float = 0f) {
    fill(color) {
        moveTo(13f, 34f + shiftY)
        curveTo(7f, 34f + shiftY, 4f, 31f + shiftY, 4f, 27f + shiftY)
        curveTo(4f, 23f + shiftY, 7f, 20f + shiftY, 11f, 20f + shiftY)
        curveTo(11f, 14f + shiftY, 15f, 10f + shiftY, 21f, 10f + shiftY)
        curveTo(27f, 10f + shiftY, 31f, 14f + shiftY, 32f, 19f + shiftY)
        curveTo(38f, 17f + shiftY, 43f, 21f + shiftY, 43f, 27f + shiftY)
        curveTo(43f, 31f + shiftY, 40f, 34f + shiftY, 35f, 34f + shiftY)
        close()
    }
}

private fun ImageVector.Builder.smallCloud(color: Color, x: Float, y: Float) {
    fill(color) {
        moveTo(x, y)
        curveTo(x - 4f, y, x - 6f, y - 2f, x - 6f, y - 5f)
        curveTo(x - 6f, y - 8f, x - 4f, y - 10f, x - 1f, y - 10f)
        curveTo(x - 1f, y - 15f, x + 3f, y - 17f, x + 7f, y - 16f)
        curveTo(x + 11f, y - 16f, x + 14f, y - 13f, x + 14f, y - 9f)
        curveTo(x + 19f, y - 9f, x + 22f, y - 6f, x + 21f, y - 3f)
        curveTo(x + 21f, y - 1f, x + 19f, y, x + 16f, y)
        close()
    }
}

private fun ImageVector.Builder.drop(x: Float, y: Float, small: Boolean = false) {
    val r = if (small) 1.5f else 2.2f
    fill(rainBlue) {
        moveTo(x + 1f, y - 4f)
        curveTo(x + r + 1f, y - 1f, x + r, y + 2f, x, y + 2f)
        curveTo(x - r, y + 2f, x - r, y, x - 1f, y - 2f)
        close()
    }
}

private fun ImageVector.Builder.snow(x: Float, y: Float) {
    fill(snowBlue) {
        circle(x, y, 1.7f)
        repeat(3) { index ->
            val a = index * Math.PI / 3
            val ux = cos(a).toFloat() * 3.5f
            val uy = sin(a).toFloat() * 3.5f
            val px = sin(a).toFloat() * .7f
            val py = -cos(a).toFloat() * .7f
            moveTo(x - ux + px, y - uy + py)
            lineTo(x + ux + px, y + uy + py)
            lineTo(x + ux - px, y + uy - py)
            lineTo(x - ux - px, y - uy - py)
            close()
        }
    }
}

private fun ImageVector.Builder.bands(color: Color, slant: Boolean = false) {
    fill(color) {
        repeat(3) { index ->
            val y = 30f + index * 5f
            val x = if (index == 1) 6f else 11f
            moveTo(x, y)
            lineTo(43f - x / 2, y)
            lineTo(43f - x / 2 - if (slant) 3f else 0f, y + 2f)
            lineTo(x - if (slant) 3f else 0f, y + 2f)
            close()
        }
    }
}

private fun buildConditionVector(condition: WeatherCondition, night: Boolean): ImageVector =
    ImageVector.Builder("weather-${condition.name.lowercase()}-${if (night) "night" else "day"}",
        48.dp, 48.dp, 48f, 48f).apply {
        when (condition) {
            WeatherCondition.CLEAR -> if (night) moon() else sun(24f, 24f, 10f)
            WeatherCondition.FEW_CLOUDS, WeatherCondition.PARTLY_CLOUDY -> {
                if (night) moon() else sun(18f, 17f, 7.5f)
                if (condition == WeatherCondition.FEW_CLOUDS) smallCloud(cloudLight, 22f, 37f)
                else cloud(shiftY = 7f)
            }
            WeatherCondition.MOSTLY_CLOUDY -> {
                smallCloud(cloudDark, 24f, 25f)
                cloud(shiftY = 5f)
            }
            WeatherCondition.OVERCAST -> {
                smallCloud(cloudDark, 24f, 25f)
                cloud(Color(0xFFB6C6D3), shiftY = 5f)
            }
            WeatherCondition.DRIZZLE, WeatherCondition.RAIN, WeatherCondition.FREEZING_RAIN -> {
                cloud(shiftY = -3f)
                drop(14f, 39f, condition == WeatherCondition.DRIZZLE)
                drop(25f, 40f, condition == WeatherCondition.DRIZZLE)
                if (condition == WeatherCondition.FREEZING_RAIN) snow(36f, 39f)
                else drop(36f, 39f, condition == WeatherCondition.DRIZZLE)
            }
            WeatherCondition.SNOW, WeatherCondition.SLEET, WeatherCondition.HAIL -> {
                cloud(shiftY = -3f)
                if (condition == WeatherCondition.SLEET) {
                    drop(14f, 39f)
                    snow(28f, 39f)
                    drop(39f, 39f)
                } else if (condition == WeatherCondition.HAIL) {
                    fill(snowBlue) { circle(14f, 39f, 2.6f); circle(25f, 41f, 2.6f); circle(36f, 39f, 2.6f) }
                } else {
                    snow(13f, 39f); snow(25f, 40f); snow(37f, 39f)
                }
            }
            WeatherCondition.THUNDER -> {
                cloud(cloudDark, shiftY = -3f)
                fill(sunGold) {
                    moveTo(25f, 25f); lineTo(18f, 37f); lineTo(25f, 37f)
                    lineTo(22f, 47f); lineTo(34f, 32f); lineTo(27f, 32f); lineTo(31f, 25f); close()
                }
                drop(11f, 39f, true)
                drop(39f, 39f, true)
            }
            WeatherCondition.FOG, WeatherCondition.HAZE, WeatherCondition.DUST -> {
                val color = when (condition) {
                    WeatherCondition.HAZE -> Color(0xFFE4C88E)
                    WeatherCondition.DUST -> Color(0xFFCEA47D)
                    else -> cloudDark
                }
                if (condition != WeatherCondition.FOG) {
                    if (night) moon() else sun(23f, 18f, 6f)
                } else smallCloud(cloudLight, 17f, 26f)
                bands(color)
            }
            WeatherCondition.WIND -> {
                fill(cloudDark) {
                    moveTo(5f, 16f); lineTo(29f, 16f)
                    curveTo(37f, 16f, 37f, 6f, 30f, 6f)
                    lineTo(30f, 9f); curveTo(33f, 9f, 33f, 13f, 29f, 13f)
                    lineTo(5f, 13f); close()
                    moveTo(4f, 24f); lineTo(39f, 24f)
                    curveTo(46f, 24f, 46f, 14f, 40f, 14f)
                    lineTo(40f, 17f); curveTo(43f, 17f, 43f, 21f, 39f, 21f)
                    lineTo(4f, 21f); close()
                    moveTo(8f, 29f); lineTo(31f, 29f)
                    curveTo(40f, 29f, 40f, 42f, 31f, 42f)
                    lineTo(31f, 39f); curveTo(36f, 39f, 36f, 32f, 31f, 32f)
                    lineTo(8f, 32f); close()
                }
            }
            WeatherCondition.TORNADO -> fill(cloudDark) {
                repeat(6) { index ->
                    val left = 6f + index * 3f
                    val right = 43f - index * 3f
                    val y = 9f + index * 6f
                    moveTo(left, y); lineTo(right, y); lineTo(right - 1f, y + 3f); lineTo(left + 1f, y + 3f); close()
                }
            }
            WeatherCondition.UNKNOWN -> fill(cloudDark) {
                moveTo(21f, 28f); lineTo(21f, 24f)
                curveTo(21f, 20f, 29f, 20f, 29f, 16f)
                curveTo(29f, 10f, 19f, 10f, 19f, 16f)
                lineTo(15f, 16f); curveTo(15f, 5f, 33f, 5f, 33f, 16f)
                curveTo(33f, 23f, 25f, 23f, 25f, 26f)
                lineTo(25f, 28f); close(); circle(23f, 36f, 2.5f)
            }
        }
    }.build()
