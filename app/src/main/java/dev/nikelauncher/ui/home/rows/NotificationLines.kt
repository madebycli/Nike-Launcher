// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.rows

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import dev.nikelauncher.data.AppInfo
import dev.nikelauncher.data.EntryKind
import dev.nikelauncher.data.HomeAlignment
import dev.nikelauncher.media.NotificationCountBus
import dev.nikelauncher.media.NotificationLine
import dev.nikelauncher.ui.common.LocalIconConfig
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * An app's notifications, a line each. A tap opens that one and a sideways swipe clears it, as in
 * the shade; only clearable notifications ever reach this list.
 */
@Composable
internal fun NotificationLines(
    lines: List<NotificationLine>,
    alignment: HomeAlignment,
    contentColor: Color,
    interactive: Boolean,
    onFallback: () -> Unit,
) {
    val context = LocalContext.current
    val sizeSp = LocalIconConfig.current.notificationTextSp
    lines.forEach { line ->
        key(line.key) {
            val offset = remember { Animatable(0f) }
            val scope = rememberCoroutineScope()
            var width by remember { mutableStateOf(1) }
            Text(
                line.text,
                color = contentColor.copy(alpha = 0.6f * (1f - (abs(offset.value) / width).coerceIn(0f, 1f))),
                fontSize = sizeSp.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = alignment.textAlign(),
                modifier = Modifier
                    .onSizeChanged { width = it.width.coerceAtLeast(1) }
                    .graphicsLayer { translationX = offset.value }
                    .then(
                        if (!interactive) Modifier else Modifier
                            .clickable { if (!NotificationCountBus.open(context, line)) onFallback() }
                            .draggable(
                                orientation = Orientation.Horizontal,
                                state = rememberDraggableState { delta -> scope.launch { offset.snapTo(offset.value + delta) } },
                                onDragStopped = {
                                    if (abs(offset.value) > width / 3f) {
                                        offset.animateTo(if (offset.value > 0) width.toFloat() else -width.toFloat())
                                        NotificationCountBus.dismiss(line.key)
                                    } else {
                                        offset.animateTo(0f)
                                    }
                                },
                            ),
                    ),
            )
        }
    }
}

/** This app's newest notifications, or none when the setting is off. */
@Composable
internal fun notificationPreview(app: AppInfo): List<NotificationLine> {
    if (!LocalIconConfig.current.notificationPreviews || app.kind != EntryKind.APP) return emptyList()
    val previews by NotificationCountBus.previews.collectAsState()
    return previews[app.componentName.packageName].orEmpty()
}
