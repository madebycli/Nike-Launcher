// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.edit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.zIndex
import dev.nikelauncher.R
import dev.nikelauncher.data.HomeBlock

private fun HomeBlock.label(): Int = when (this) {
    HomeBlock.WIDGET -> R.string.edit_block_widget
    HomeBlock.NOW_PLAYING -> R.string.edit_block_now_playing
    HomeBlock.FAVORITES -> R.string.edit_block_favorites
}

/** What a block's frame reports about the finger. */
internal class FrameCallbacks(
    val onSelect: () -> Unit,
    val onDragStart: () -> Unit,
    val onDrag: (Float) -> Unit,
    val onDragEnd: () -> Unit,
)

/**
 * What edit mode wraps around a home block: an outline, and a chip to move it by.
 *
 * Moving is the whole of it. Where a block ends up is where it stays: the finger decides, there is
 * no number to type. Widget and Now Playing can also be taken by their body, since nothing inside
 * them answers a touch while editing; the favorites cannot, because their rows have handles of
 * their own. Out of edit mode this is only its content.
 */
@Composable
internal fun BlockFrame(
    block: HomeBlock,
    editing: Boolean,
    selected: Boolean,
    takeByBody: Boolean,
    coverBody: Boolean,
    topInsidePx: () -> Int,
    callbacks: FrameCallbacks,
    modifier: Modifier = Modifier,
    /** How far the content is moved sideways inside the field; the field itself never leaves the screen edges. */
    contentOffsetDp: Int = 0,
    content: @Composable () -> Unit,
) {
    if (!editing) {
        Box(modifier.clipToBounds()) { Box(Modifier.offset(x = contentOffsetDp.dp)) { content() } }
        return
    }
    val current by rememberUpdatedState(callbacks)
    val outline = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline

    Box(
        modifier
            .then(
                if (takeByBody) Modifier.pointerInput(block) {
                    detectVerticalDragGestures(
                        onDragStart = { current.onDragStart() },
                        onDragEnd = { current.onDragEnd() },
                        onDragCancel = { current.onDragEnd() },
                        onVerticalDrag = { change, amount -> change.consume(); current.onDrag(amount) },
                    )
                } else Modifier,
            )
            .border(BorderStroke(if (selected) 2.dp else 1.dp, outline), MaterialTheme.shapes.large),
    ) {
        // The chip straddles the top edge, so its lower half would sit on whatever comes first.
        Box(Modifier.padding(top = ChipHalfHeight).clipToBounds()) { Box(Modifier.offset(x = contentOffsetDp.dp)) { content() } }
        if (coverBody) {
            // Nothing inside answers a touch while editing, so the body only selects and moves.
            Box(Modifier.matchParentSize().pointerInput(block) { detectTapGestures(onTap = { current.onSelect() }) })
        }
        AssistChip(
            onClick = { current.onSelect() },
            label = { Text(stringResource(block.label())) },
            leadingIcon = { Icon(Icons.Filled.DragHandle, contentDescription = null) },
            // Straddles the top edge where there is room above, and sits inside where there is not.
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(2f)
                .offset { IntOffset(0, -minOf(ChipHalfHeight.roundToPx(), topInsidePx())) }
                .pointerInput(block) {
                    detectDragGestures(
                        onDragStart = { current.onDragStart() },
                        onDragEnd = { current.onDragEnd() },
                        onDragCancel = { current.onDragEnd() },
                    ) { change, amount -> change.consume(); current.onDrag(amount.y) }
                },
        )
    }
}

private val ChipHalfHeight = 16.dp
