// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.applist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nikelauncher.R
import dev.nikelauncher.data.EdgeSide
import dev.nikelauncher.data.HomeAlignment
import dev.nikelauncher.data.folderToken

@Composable
internal fun AppListSession.Rows() {
    LazyColumn(
        state = listState,
        modifier =
            Modifier.fillMaxSize()
                .graphicsLayer {
                    translationY = stretchProvider()
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .drawWithContent {
                    drawContent()
                    val fade = (FADE_HEIGHT.toPx() / size.height).coerceIn(0f, 0.5f)
                    if (fade > 0f) {
                        drawRect(
                            brush =
                                Brush.verticalGradient(
                                    0f to Color.Black.copy(alpha = 1f - topFade),
                                    fade to Color.Black,
                                    1f - fade to Color.Black,
                                    1f to Color.Black.copy(alpha = 1f - bottomFade),
                                ),
                            blendMode = BlendMode.DstIn,
                        )
                    }
                },
        contentPadding =
            with(density) {
                val top =
                    when {
                        searching -> SEARCH_EDGE_PADDING
                        scrubLetter != null || !highlightRange.isEmpty() -> sectionTopPx.toDp()
                        else -> restingTopPadding
                    }
                PaddingValues(
                    start = if (edgeSide != EdgeSide.RIGHT) stripInset else 0.dp,
                    end = if (edgeSide != EdgeSide.LEFT) stripInset else 0.dp,
                    top = top,
                    bottom =
                        when {
                            searching -> SEARCH_EDGE_PADDING
                            lastLetterToLine && bottomRoomLive ->
                                (viewportHeightPx - sectionTopPx).coerceAtLeast(0).toDp()
                            else -> restingBottomPadding
                        },
                )
            },
    ) {
        if (searching && webSearchFallback && displayModel.rows.isEmpty()) {
            item(key = "websearch") {
                val term = query.trim()
                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .clickable { onWebSearch(term) }
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        stringResource(R.string.applist_search_web, term),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        itemsIndexed(
            items = displayModel.rows,
            key = { _, row -> rowKey(row) },
            contentType = { _, row ->
                when (row) {
                    is AppListRow.Header -> 0
                    is AppListRow.Entry -> 1
                    is AppListRow.FolderRow -> 2
                }
            },
        ) { index, row ->
            Box(
                modifier =
                    Modifier.graphicsLayer {
                        alpha = if (index in highlightRange) 1f else othersAlpha
                    }
            ) {
                when (row) {
                    is AppListRow.Header ->
                        SectionHeader(row.text, labelSizeSp, contentColor, alignment)
                    is AppListRow.FolderRow ->
                        FolderListRow(
                            row = row,
                            contentColor = contentColor,
                            alignment = alignment,
                            iconSide = iconSide,
                            iconSizeDp = iconSizeDp,
                            labelSizeSp = labelSizeSp,
                            onHome = favoriteKeys.contains(folderToken(row.folder.id)),
                            menuExpanded = menuForKey == rowKey(row),
                            menuOffset = menuOffset,
                            touchPosition = touchPosition,
                            onToggle = { onToggleFolder(row.folder) },
                            onLongPress = { offset ->
                                menuOffset = offset
                                menuForKey = rowKey(row)
                            },
                            onDismissMenu = { menuForKey = null },
                            onManage = {
                                menuForKey = null
                                onManageFolder(row.folder)
                            },
                            onSetOnHome = {
                                menuForKey = null
                                onSetFolderOnHome(row.folder, it)
                            },
                            onDelete = {
                                menuForKey = null
                                onDeleteFolder(row.folder)
                            },
                        )
                    is AppListRow.Entry ->
                        Box(
                            Modifier.padding(
                                start =
                                    if (row.inFolder && alignment == HomeAlignment.LEFT)
                                        FOLDER_INDENT
                                    else 0.dp,
                                end =
                                    if (row.inFolder && alignment == HomeAlignment.RIGHT)
                                        FOLDER_INDENT
                                    else 0.dp,
                            )
                        ) {
                            AppRow(
                                shortcutSwipe = shortcutSwipe,
                                favoritesEditable = favoritesEditable,
                                contentColor = contentColor,
                                alignment = alignment,
                                iconSide = iconSide,
                                app = row.app,
                                label = displayName(row.app),
                                iconSizeDp = iconSizeDp,
                                labelSizeSp = labelSizeSp,
                                isFavorite = favoriteKeys.contains(row.app.key),
                                menuExpanded = menuForKey == rowKey(row),
                                menuOffset = menuOffset,
                                touchPosition = touchPosition,
                                onLaunch = { onLaunch(row.app) },
                                onLongPress = { offset ->
                                    menuOffset = offset
                                    menuForKey = rowKey(row)
                                },
                                onDismissMenu = { menuForKey = null },
                                onSetFavorite = { onSetFavorite(row.app, it) },
                                onEdit = { editDialogFor = row.app },
                                onAppInfo = { onAppInfo(row.app) },
                                onUnpin = { onUnpinShortcut(row.app) },
                                onHide = { onHideApp(row.app, row.app.key !in hiddenApps) },
                                isHidden = row.app.key in hiddenApps,
                                onMoveToFolder = { onMoveToFolder(row.app) },
                                onRemoveFromFolder =
                                    row.section
                                        ?.removePrefix("folder:")
                                        ?.takeIf { row.inFolder }
                                        ?.let { id -> folderById[id] }
                                        ?.let { folder -> { onRemoveFromFolder(folder, row.app) } },
                            )
                        }
                }
            }
        }

        if (showSettingsRow)
            item(key = "settings", contentType = "settings") {
                Spacer(Modifier.height(SETTINGS_ROW_GAP))
                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .graphicsLayer { alpha = othersAlpha }
                            .clickable(onClick = onOpenSettings)
                            .heightIn(min = MIN_ROW_HEIGHT)
                            .padding(horizontal = 28.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement =
                        when (alignment) {
                            HomeAlignment.LEFT -> Arrangement.Start
                            HomeAlignment.CENTER -> Arrangement.Center
                            HomeAlignment.RIGHT -> Arrangement.End
                        },
                ) {
                    if (alignment == HomeAlignment.RIGHT) {
                        Text(
                            stringResource(R.string.action_open_settings),
                            color = contentColor.copy(alpha = 0.8f),
                            fontSize = labelSizeSp.sp,
                        )
                        Spacer(Modifier.width(16.dp))
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = null,
                            tint = contentColor.copy(alpha = 0.8f),
                        )
                    } else {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = null,
                            tint = contentColor.copy(alpha = 0.8f),
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            stringResource(R.string.action_open_settings),
                            color = contentColor.copy(alpha = 0.8f),
                            fontSize = labelSizeSp.sp,
                        )
                    }
                }
            }
    }
}
