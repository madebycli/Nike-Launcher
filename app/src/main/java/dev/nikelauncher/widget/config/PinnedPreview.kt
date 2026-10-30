// SPDX-License-Identifier: GPL-3.0-or-later
// Adapted from Victoria Grid (GPL-3.0-or-later), widget/ClockWidgetSettings.kt (WidgetSettingsPreview).
package dev.nikelauncher.widget.config

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.ui.platform.testTag
import dev.nikelauncher.ui.common.previewPlate
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.nikelauncher.widget.ClockHeader
import dev.nikelauncher.widget.ClockLiveState

/** The live preview on top and the page's rows scrolling under it, so every change is seen as it is made. */
@Composable
internal fun PinnedPreview(state: ClockLiveState, content: @Composable ColumnScope.() -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val previewHeight = (maxHeight * .33f).coerceIn(120.dp, 240.dp)
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 8.dp, bottom = 16.dp)) {
                ConfigPreview(state, previewHeight)
            }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                content = content,
            )
        }
    }
}

/**
 * The widget as it will read, drawn on a plate that contrasts with its text colour: a light clock
 * on a dark plate and the reverse, since the wallpaper it will sit on is not known here.
 */
@Composable
private fun ConfigPreview(state: ClockLiveState, maxHeight: Dp) {
    val colors = MaterialTheme.colorScheme
    val plate = previewPlate(Color(state.config.textColor), listOf(colors.surfaceContainerLowest, colors.surfaceContainerHighest, colors.inverseSurface))
    Surface(modifier = Modifier.fillMaxWidth().height(maxHeight).testTag("clock-preview"), shape = MaterialTheme.shapes.extraLarge, color = plate) {
        Layout(
            content = { ClockHeader(state, interactive = false) },
            modifier = Modifier.fillMaxSize().padding(20.dp),
        ) { measurables, constraints ->
            val child = measurables.single().measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
            val available = (maxHeight - 40.dp).roundToPx().coerceAtLeast(1)
            val scale = minOf(1f, available.toFloat() / child.height.coerceAtLeast(1))
            val height = constraints.maxHeight
            layout(constraints.maxWidth, height) {
                child.placeWithLayer(((constraints.maxWidth - child.width * scale) / 2).toInt(), (height - child.height * scale).toInt() / 2) {
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(0f, 0f)
                }
            }
        }
    }
}
