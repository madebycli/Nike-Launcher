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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nikelauncher.R
import dev.nikelauncher.data.HomeAlignment
import dev.nikelauncher.data.IconSide
import dev.nikelauncher.ui.common.LocalIconConfig
import dev.nikelauncher.ui.common.recordTouchPosition
import dev.nikelauncher.ui.home.rows.FolderIcon

@Composable
internal fun FolderListRow(
    row: AppListRow.FolderRow,
    contentColor: Color,
    alignment: HomeAlignment,
    iconSide: IconSide,
    iconSizeDp: Int,
    labelSizeSp: Int,
    onHome: Boolean,
    menuExpanded: Boolean,
    menuOffset: DpOffset,
    touchPosition: MutableState<Offset>,
    onToggle: () -> Unit,
    onLongPress: (DpOffset) -> Unit,
    onDismissMenu: () -> Unit,
    onManage: () -> Unit,
    onSetOnHome: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val density = LocalDensity.current
    Box {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .recordTouchPosition(touchPosition)
                    .padding(horizontal = 20.dp)
                    .background(
                        color =
                            if (pressed) contentColor.copy(alpha = 0.15f) else Color.Transparent,
                        shape = MaterialTheme.shapes.largeIncreased,
                    )
                    .combinedClickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = onToggle,
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
            val icon: @Composable () -> Unit = {
                FolderIcon(row.members, iconSizeDp, contentColor, row.folder.icon)
            }
            val text: @Composable () -> Unit = {
                Text(
                    row.folder.name,
                    color = contentColor,
                    fontSize = labelSizeSp.sp,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
            if (iconSide == IconSide.RIGHT) {
                text()
                Spacer(Modifier.width(gap))
                icon()
            } else {
                icon()
                Spacer(Modifier.width(gap))
                text()
            }
        }

        dev.nikelauncher.ui.kit.menu.TouchMenu(menuExpanded, menuOffset, onDismissMenu) {
            item(
                stringResource(R.string.home_folder_choose_apps),
                {
                    onDismissMenu()
                    onManage()
                },
                leadingIcon = Icons.Filled.Tune,
            )
            item(
                stringResource(
                    if (onHome) R.string.folder_remove_from_home else R.string.folder_add_to_home
                ),
                {
                    onDismissMenu()
                    onSetOnHome(!onHome)
                },
                leadingIcon = if (onHome) Icons.Filled.StarBorder else Icons.Filled.Star,
            )
            gap()
            item(
                stringResource(R.string.home_folder_delete),
                {
                    onDismissMenu()
                    onDelete()
                },
                leadingIcon = Icons.Filled.Delete,
                destructive = true,
            )
        }
    }
}
