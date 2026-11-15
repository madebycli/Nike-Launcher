// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.blocks

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import dev.nikelauncher.R
import dev.nikelauncher.data.HomeAlignment
import dev.nikelauncher.media.MediaWidgetSettingsSheet
import dev.nikelauncher.media.NowPlayingWidget
import dev.nikelauncher.media.openNowPlayingApp
import dev.nikelauncher.ui.common.recordTouchPosition
import dev.nikelauncher.ui.common.toast
import dev.nikelauncher.ui.kit.menu.TouchMenu

/** The Now Playing card: a tap opens what is playing, a long press offers the layout and settings. */
@Composable
internal fun NowPlayingBlock(
    heightDp: Int,
    contentColor: Color,
    alignment: HomeAlignment,
    sidePaddingDp: Int,
    showPlaceholder: Boolean,
    onEditLayout: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val touch = remember { mutableStateOf(Offset.Zero) }
    var menuExpanded by remember { mutableStateOf(false) }
    var configure by remember { mutableStateOf(false) }
    var menuOffset by remember { mutableStateOf(DpOffset.Zero) }

    Box(modifier) {
        NowPlayingWidget(
            heightDp = heightDp,
            contentColor = contentColor,
            alignment = alignment,
            showPlaceholder = showPlaceholder,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = sidePaddingDp.dp)
                .recordTouchPosition(touch)
                .combinedClickable(
                    // The transport buttons consume their own taps, so this is only ever the card
                    // itself, which should get you to what is playing.
                    onClick = { if (!openNowPlayingApp(context)) context.toast(R.string.now_playing_open_failed) },
                    onLongClick = {
                        menuOffset = with(density) { DpOffset(touch.value.x.toDp(), touch.value.y.toDp()) }
                        menuExpanded = true
                    },
                ),
        )
        TouchMenu(expanded = menuExpanded, offset = menuOffset, onDismissRequest = { menuExpanded = false }) {
            item(stringResource(R.string.widget_change_settings), { menuExpanded = false; configure = true }, leadingIcon = Icons.Filled.Tune)
            item(stringResource(R.string.action_edit_layout), { menuExpanded = false; onEditLayout() }, leadingIcon = Icons.Filled.Edit)
            item(stringResource(R.string.action_open_settings), { menuExpanded = false; onOpenSettings() }, leadingIcon = Icons.Filled.Settings)
        }
        if (configure) MediaWidgetSettingsSheet { configure = false }
    }
}
