// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.applist

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nikelauncher.data.AppInfo
import dev.nikelauncher.data.EntryKind
import dev.nikelauncher.data.HomeAlignment
import dev.nikelauncher.data.IconSide
import dev.nikelauncher.data.ShortcutSwipe
import dev.nikelauncher.ui.common.AppIcon
import dev.nikelauncher.ui.common.AppShortcutMenu
import dev.nikelauncher.ui.common.LocalIconConfig
import dev.nikelauncher.ui.common.recordTouchPosition
import dev.nikelauncher.ui.common.swipeForShortcuts
import dev.nikelauncher.ui.notifications.*
import dev.nikelauncher.ui.theme.textContrastCloud

@Composable
internal fun AppRow(
    shortcutSwipe: ShortcutSwipe,
    /** False when the favorites are computed, so starring would write to a list nobody sees. */
    favoritesEditable: Boolean,
    contentColor: Color,
    alignment: HomeAlignment,
    iconSide: IconSide,
    touchPosition: MutableState<Offset>,
    app: AppInfo,
    label: String,
    iconSizeDp: Int,
    labelSizeSp: Int,
    isFavorite: Boolean,
    menuExpanded: Boolean,
    menuOffset: DpOffset,
    onLaunch: () -> Unit,
    onLongPress: (DpOffset) -> Unit,
    onDismissMenu: () -> Unit,
    onSetFavorite: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onAppInfo: () -> Unit,
    onUnpin: () -> Unit,
    onHide: () -> Unit,
    onMoveToFolder: () -> Unit,
    /** Hidden apps only ever reach this list through a search. */
    isHidden: Boolean,
    /** Set for an app listed inside an open folder. */
    onRemoveFromFolder: (() -> Unit)? = null,
) {
    // Same press treatment as the home screen: the stock ripple all but vanishes against a
    // wallpaper, and without any feedback a tap that did register reads as one that didn't.
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val density = LocalDensity.current
    var shortcutMenu by remember { mutableStateOf(false) }

    val preview = appNotification(app)
    var notificationsOpen by remember(app.key) { mutableStateOf(false) }
    Box {
        if (preview != null) {
            NotificationAppContent(
                app,
                label,
                preview,
                contentColor,
                labelSizeSp,
                iconSizeDp,
                iconSide,
                modifier =
                    Modifier.fillMaxWidth().recordTouchPosition(touchPosition).swipeForShortcuts(
                        shortcutSwipe,
                        true,
                        onNotifications = { notificationsOpen = true },
                    ) {
                        shortcutMenu = true
                    }.padding(horizontal = 20.dp),
                onOpen = { notificationsOpen = true },
                onLaunch = onLaunch,
                onLongClick = {
                    onLongPress(
                        with(density) {
                            DpOffset(touchPosition.value.x.toDp(), touchPosition.value.y.toDp())
                        }
                    )
                },
            )
        } else
            Row(
                modifier =
                    Modifier.fillMaxWidth()
                        // Ahead of the inset, so the long-press menu is still placed against the
                        // whole row rather than 20dp to the left of the finger.
                        .recordTouchPosition(touchPosition)
                        .swipeForShortcuts(shortcutSwipe, app.kind == EntryKind.APP) {
                            shortcutMenu = true
                        }
                        .padding(horizontal = 20.dp)
                        .background(
                            color =
                                if (pressed) contentColor.copy(alpha = 0.15f)
                                else Color.Transparent,
                            shape = MaterialTheme.shapes.largeIncreased,
                        )
                        .combinedClickable(
                            interactionSource = interaction,
                            indication = null,
                            onClick = onLaunch,
                            onLongClick = {
                                onLongPress(
                                    with(density) {
                                        DpOffset(
                                            touchPosition.value.x.toDp(),
                                            touchPosition.value.y.toDp(),
                                        )
                                    }
                                )
                            },
                        )
                        // The whole row is the target, not the label: at small icon sizes the strip
                        // left to tap was thinner than a fingertip.
                        .heightIn(min = MIN_ROW_HEIGHT)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement =
                    when (alignment) {
                        HomeAlignment.LEFT -> Arrangement.Start
                        HomeAlignment.CENTER -> Arrangement.Center
                        HomeAlignment.RIGHT -> Arrangement.End
                    },
            ) {
                val gap = if (LocalIconConfig.current.showIcons) 16.dp else 0.dp
                // fill = false so the label takes only the width it needs and the icon stays
                // beside it; a plain weight spans the row and pins the icon to the far edge.
                val labelModifier =
                    Modifier.weight(1f, fill = false).textContrastCloud(contentColor)
                val text: @Composable () -> Unit = {
                    Text(
                        label,
                        // Dimmed, because a hidden app only ever turns up here through a search
                        // and nothing else on the row says it is one. A shortcut its publisher
                        // has switched off gets the same treatment, for the same reason: it is
                        // listed, but tapping it only explains why it will not open.
                        color =
                            if (isHidden || app.disabled) contentColor.copy(alpha = 0.5f)
                            else contentColor,
                        fontSize = labelSizeSp.sp,
                        modifier = labelModifier,
                        textAlign =
                            when (alignment) {
                                HomeAlignment.LEFT -> TextAlign.Start
                                HomeAlignment.CENTER -> TextAlign.Center
                                HomeAlignment.RIGHT -> TextAlign.End
                            },
                    )
                }
                // Centerd rows balance the icon with a spacer on the label's far side, so the
                // text lands on the screen's center line rather than the pair straddling it.
                val showIcons = LocalIconConfig.current.showIcons
                val balance: @Composable () -> Unit = {
                    if (alignment == HomeAlignment.CENTER && showIcons) {
                        Spacer(Modifier.width(iconSizeDp.dp + gap))
                    }
                }
                if (iconSide == IconSide.RIGHT) {
                    balance()
                    text()
                    Spacer(Modifier.width(gap))
                    AppIcon(app = app, sizeDp = iconSizeDp)
                } else {
                    AppIcon(app = app, sizeDp = iconSizeDp)
                    Spacer(Modifier.width(gap))
                    text()
                    balance()
                }
            }

        if (notificationsOpen) NotificationSheet(app, onDismiss = { notificationsOpen = false })
        AppShortcutMenu(app, shortcutMenu) { shortcutMenu = false }

        AppListAppMenu(
            app,
            menuExpanded,
            menuOffset,
            favoritesEditable,
            isFavorite,
            isHidden,
            hasNotifications = preview != null,
            onNotifications = { notificationsOpen = true },
            onDismissMenu,
            onSetFavorite,
            onEdit,
            onAppInfo,
            onUnpin,
            onHide,
            onMoveToFolder,
            onRemoveFromFolder,
        )
    }
}
