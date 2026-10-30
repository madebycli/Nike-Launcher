// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.applist.rows

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nikelauncher.data.AppInfo
import dev.nikelauncher.data.EntryKind
import dev.nikelauncher.data.HomeAlignment
import dev.nikelauncher.data.IconSide
import dev.nikelauncher.ui.common.AppIcon
import dev.nikelauncher.ui.common.AppShortcutMenu
import dev.nikelauncher.ui.common.LocalIconConfig
import dev.nikelauncher.ui.common.recordTouchPosition
import dev.nikelauncher.ui.common.swipeForShortcuts

/** What a row's long-press menu can do with its app. */
internal class AppRowActions(
    val onLaunch: () -> Unit,
    val onSetFavorite: (Boolean) -> Unit,
    val onEdit: () -> Unit,
    val onAppInfo: () -> Unit,
    val onUnpin: () -> Unit,
    val onHide: () -> Unit,
    val onMoveToFolder: () -> Unit,
    /** Set for an app listed inside an open folder. */
    val onRemoveFromFolder: (() -> Unit)?,
)

/** One app in the list: icon and name, a ripple for the press, and its long-press menu. */
@Composable
internal fun AppRow(
    app: AppInfo,
    label: String,
    style: ListRowStyle,
    isFavorite: Boolean,
    /** Hidden apps only ever reach this list through a search. */
    isHidden: Boolean,
    menuExpanded: Boolean,
    menuOffset: DpOffset,
    touchPosition: MutableState<Offset>,
    actions: AppRowActions,
    onLongPress: (DpOffset) -> Unit,
    onDismissMenu: () -> Unit,
) {
    val density = LocalDensity.current
    var shortcutMenu by remember { mutableStateOf(false) }
    val alignment = style.alignment

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // Ahead of the inset, so the long-press menu is still placed against the whole row.
                .recordTouchPosition(touchPosition)
                .swipeForShortcuts(style.shortcutSwipe, app.kind == EntryKind.APP) { shortcutMenu = true }
                .padding(horizontal = 20.dp)
                .clip(MaterialTheme.shapes.large)
                .combinedClickable(
                    indication = ripple(color = style.contentColor),
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    onClick = actions.onLaunch,
                    onLongClick = {
                        onLongPress(with(density) { DpOffset(touchPosition.value.x.toDp(), touchPosition.value.y.toDp()) })
                    },
                )
                // The whole row is the target, not the label: at small icon sizes the strip left to
                // tap was thinner than a fingertip.
                .heightIn(min = MIN_ROW_HEIGHT)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = alignment.rowArrangement(),
        ) {
            val showIcons = LocalIconConfig.current.showIcons
            val gap = if (showIcons) 16.dp else 0.dp
            val text: @Composable () -> Unit = {
                Text(
                    label,
                    // Dimmed, because a hidden app only ever turns up here through a search and
                    // nothing else on the row says it is one. A shortcut its publisher has switched
                    // off gets the same treatment: it is listed, but tapping it only explains why.
                    color = if (isHidden || app.disabled) style.contentColor.copy(alpha = 0.5f) else style.contentColor,
                    fontSize = style.labelSizeSp.sp,
                    modifier = Modifier.weight(1f, fill = false),
                    textAlign = alignment.textAlign(),
                )
            }
            // Centred rows balance the icon with a spacer on the label's far side, so the text
            // lands on the screen's centre line rather than the pair straddling it.
            val balance: @Composable () -> Unit = {
                if (alignment == HomeAlignment.CENTER && showIcons) Spacer(Modifier.width(style.iconSizeDp.dp + gap))
            }
            if (style.iconSide == IconSide.RIGHT) {
                balance(); text(); Spacer(Modifier.width(gap)); AppIcon(app = app, sizeDp = style.iconSizeDp)
            } else {
                AppIcon(app = app, sizeDp = style.iconSizeDp); Spacer(Modifier.width(gap)); text(); balance()
            }
        }

        AppShortcutMenu(app, shortcutMenu) { shortcutMenu = false }

        AppRowMenu(
            app = app,
            expanded = menuExpanded,
            offset = menuOffset,
            favoritesEditable = style.favoritesEditable,
            isFavorite = isFavorite,
            isHidden = isHidden,
            actions = actions,
            onDismiss = onDismissMenu,
        )
    }
}
