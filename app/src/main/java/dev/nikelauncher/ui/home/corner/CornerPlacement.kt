// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.corner

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.nikelauncher.R
import dev.nikelauncher.data.*

/** A separate placement canvas: settings are closed and no position is saved until Done. */
@Composable
internal fun CornerPlacement(settings: CornerSettings, app: AppInfo?, alignment: HomeAlignment, color: Color,
    onSave: (CornerPosition?) -> Unit, onCancel: () -> Unit) {
    var draft by rememberSaveable { mutableStateOf(settings.position) }
    val density = LocalDensity.current
    val insets = WindowInsets.safeDrawing
    BackHandler(onBack = onCancel)
    BoxWithConstraints(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures {} }.testTag("corner-placement")) {
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val size = with(density) { settings.sizeDp.dp.roundToPx() }
        val side = with(density) { 20.dp.roundToPx() }
        val travel = cornerTravel(width, height, size, side,
            insets.getTop(density), insets.getBottom(density) + with(density) { 24.dp.roundToPx() })
        val default = CornerPosition(if (alignment == HomeAlignment.RIGHT) 0f else 1f, 1f)
        val position = draft ?: default
        val location = travel.location(position)
        val latest by rememberUpdatedState(location)
        CornerButtonFace(settings.shape, settings.sizeDp, settings.tap, app, color, appearance = settings.appearance,
            modifier = Modifier.offset { IntOffset(location.first, location.second) }.testTag("corner-placement-button")
                .pointerInput(travel) {
                    var startX = 0f
                    var startY = 0f
                    detectDragGestures(onDragStart = { startX = latest.first.toFloat(); startY = latest.second.toFloat() }) { change, delta ->
                        change.consume(); startX += delta.x; startY += delta.y
                        draft = travel.position(startX, startY)
                    }
                })
        HorizontalFloatingToolbar(expanded = true, modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 16.dp)) {
            IconButton(onClick = { draft = null }) { Icon(Icons.Filled.RestartAlt, stringResource(R.string.corner_position_reset)) }
            IconButton(onClick = { onSave(draft) }) { Icon(Icons.Filled.Done, stringResource(R.string.action_done)) }
        }
    }
}
