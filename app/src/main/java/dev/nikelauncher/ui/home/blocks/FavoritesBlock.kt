// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.blocks

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.nikelauncher.R
import dev.nikelauncher.data.AppInfo
import dev.nikelauncher.data.Folder
import dev.nikelauncher.service.HapticUtil
import dev.nikelauncher.ui.common.EditAppDialog
import dev.nikelauncher.ui.common.LocalIconConfig
import dev.nikelauncher.ui.home.FavoriteEntry
import dev.nikelauncher.ui.home.HomeActions
import dev.nikelauncher.ui.home.HomeData
import dev.nikelauncher.ui.home.HomeSettings
import dev.nikelauncher.ui.home.rows.FavoriteMenuActions
import dev.nikelauncher.ui.home.rows.FavoriteRow
import dev.nikelauncher.ui.home.rows.FolderEditDialog
import dev.nikelauncher.ui.home.rows.FolderMemberActions
import dev.nikelauncher.ui.home.rows.FolderMenuActions
import dev.nikelauncher.ui.home.rows.FolderRow
import dev.nikelauncher.ui.home.rows.RowStyle
import androidx.compose.runtime.CompositionLocalProvider
import kotlin.math.abs

/**
 * The favorites: one row per app or folder, reorderable by a grab handle in edit mode.
 *
 * [onFolderOpened] is told where an opened folder ends up once it has stopped growing, so the
 * screen can move up enough to show all of it.
 */
@Composable
internal fun FavoritesBlock(
    data: HomeData,
    settings: HomeSettings,
    style: RowStyle,
    actions: HomeActions,
    editMode: Boolean,
    onEditModeChange: (Boolean) -> Unit,
    stripInset: Modifier,
    folderControlsInset: Modifier,
    onFolderOpened: (topPx: Float, heightPx: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val reorder = remember { FavoritesReorder() }
    val touch = remember { mutableStateOf(Offset.Zero) }
    val workSuffix = stringResource(R.string.app_work_suffix)
    fun nameOf(app: AppInfo) = data.displayName(app, settings.markWorkApps, workSuffix)

    var menuForKey by remember { mutableStateOf<String?>(null) }
    var folderMenuFor by remember { mutableStateOf<String?>(null) }
    var menuOffset by remember { mutableStateOf(DpOffset.Zero) }
    var renameFor by remember { mutableStateOf<AppInfo?>(null) }
    var folderRenameFor by remember { mutableStateOf<Folder?>(null) }
    var expanded by remember { mutableStateOf(setOf<String>()) }
    var opened by remember { mutableStateOf<Pair<String, Long>?>(null) }

    val reorderable = settings.favoritesReorderable
    val byToken = remember(data.favorites) { data.favorites.associateBy { it.token } }
    val entries = reorder.order?.mapNotNull { byToken[it] } ?: data.favorites

    // Leaving edit mode drops anything a drag left standing: a gesture can be taken away before
    // its release, and nothing is told when the row holding it is disposed.
    LaunchedEffect(editMode) { if (!editMode) reorder.cancel() }
    LaunchedEffect(entries) { reorder.retain(entries.map { it.token }.toSet()) }

    // A folder opens with an animation, so its height is followed frame by frame until it stops
    // growing, and only then does the screen move to show it.
    LaunchedEffect(opened) {
        val token = opened?.first ?: return@LaunchedEffect
        var last = -1
        var still = 0
        var frames = 0
        while (still < 3 && frames++ < 90) {
            withFrameNanos { }
            val height = reorder.heights[token] ?: return@LaunchedEffect
            if (height == last) still++ else still = 0
            last = height
        }
        onFolderOpened(reorder.tops[token] ?: return@LaunchedEffect, last)
    }

    val baseIconConfig = LocalIconConfig.current
    val iconConfig = remember(baseIconConfig, settings.showFavoriteIcons) {
        baseIconConfig.copy(showIcons = settings.showFavoriteIcons)
    }

    Column(modifier.fillMaxWidth()) {
        if (entries.isEmpty() && settings.showEmptyHint) {
            FilledTonalButton(
                onClick = actions.onManageFavorites,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp).align(Alignment.CenterHorizontally),
            ) { Text(stringResource(R.string.home_choose_favorites)) }
        }
        // The favorites answer to their own icon setting, so a list of bare names here can sit
        // above an app list that still has icons.
        CompositionLocalProvider(LocalIconConfig provides iconConfig) {
            entries.forEachIndexed { index, entry ->
                // Identified by which row it is, not by where it sits, so a reorder moves the
                // node instead of feeding different data into the same one.
                key(entry.token) {
                    val token = entry.token
                    val dragHandle = if (editMode && reorderable) {
                        Modifier.pointerInput(token) {
                            try {
                                detectDragGestures(
                                    onDragStart = {
                                        HapticUtil.tick(view, settings.hapticsEnabled)
                                        reorder.begin(token, entries.map { it.token })
                                    },
                                    onDragEnd = { reorder.finish(actions.layout.onReorderFavorites) },
                                    onDragCancel = { reorder.finish(actions.layout.onReorderFavorites) },
                                ) { change, amount ->
                                    change.consume()
                                    reorder.dragBy(amount.y)
                                }
                            } finally {
                                // The gesture was taken away without a release: put the order
                                // back to what was saved instead of keeping a held row.
                                if (reorder.isHeld(token)) reorder.cancel()
                            }
                        }
                    } else null

                    Column {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .then(stripInset)
                                .onGloballyPositioned { coords ->
                                    reorder.heights[token] = coords.size.height
                                    reorder.tops[token] = coords.positionInWindow().y
                                }
                                .zIndex(if (reorder.isHeld(token)) 1f else 0f)
                                .graphicsLayer {
                                    if (reorder.isHeld(token)) {
                                        translationY = reorder.dragOffset
                                        scaleX = 1.03f
                                        scaleY = 1.03f
                                        alpha = 0.9f
                                    }
                                },
                        ) {
                            when (entry) {
                                is FavoriteEntry.App -> FavoriteRow(
                                    app = entry.app,
                                    label = nameOf(entry.app),
                                    style = style,
                                    editMode = editMode,
                                    dragHandle = dragHandle,
                                    menuExpanded = menuForKey == entry.app.key,
                                    menuOffset = menuOffset,
                                    touchPosition = touch,
                                    onLaunch = { actions.apps.onLaunch(entry.app) },
                                    onOpenMenu = { offset -> menuOffset = offset; menuForKey = entry.app.key },
                                    onDismissMenu = { menuForKey = null },
                                    menu = FavoriteMenuActions(
                                        onMoveToFolder = { menuForKey = null; actions.apps.onMoveToFolder(entry.app) },
                                        onEditLayout = { menuForKey = null; onEditModeChange(true) },
                                        onAppInfo = { menuForKey = null; actions.apps.onAppInfo(entry.app) },
                                        onUnpin = { menuForKey = null; actions.apps.onUnpinShortcut(entry.app) },
                                        onRemove = { menuForKey = null; actions.apps.onRemoveFavorite(entry.app) },
                                        onEditIconName = { menuForKey = null; renameFor = entry.app },
                                        onOpenSettings = { menuForKey = null; actions.onOpenSettings() },
                                    ),
                                )

                                is FavoriteEntry.FolderRef -> {
                                    val folder = entry.folder
                                    FolderRow(
                                        folder = folder,
                                        members = folder.apps.mapNotNull { data.appsByKey[it] },
                                        style = style,
                                        expanded = folder.id in expanded,
                                        editMode = editMode,
                                        dragHandle = dragHandle,
                                        controlsInset = folderControlsInset,
                                        menuExpanded = folderMenuFor == folder.id,
                                        menuOffset = menuOffset,
                                        touchPosition = touch,
                                        widgetActions = actions.widgets,
                                        memberActions = FolderMemberActions(
                                            displayName = ::nameOf,
                                            onOpenApp = { member ->
                                                // Closed as the app starts rather than on the way
                                                // back: a folder that shuts while you are still
                                                // looking at it is the launcher fidgeting.
                                                if (settings.closeFolderOnLaunch) expanded = expanded - folder.id
                                                actions.folders.onOpenApp(member)
                                            },
                                            onRemoveApp = { member -> actions.folders.onRemoveApp(folder, member) },
                                            onAppInfo = actions.apps.onAppInfo,
                                            onUnpin = actions.apps.onUnpinShortcut,
                                            onEditIconName = { member -> renameFor = member },
                                            onManage = { actions.folders.onManage(folder) },
                                            onOpenSettings = actions.onOpenSettings,
                                        ),
                                        menu = FolderMenuActions(
                                            onManage = { folderMenuFor = null; actions.folders.onManage(folder) },
                                            onEdit = { folderMenuFor = null; folderRenameFor = folder },
                                            onEditLayout = { folderMenuFor = null; onEditModeChange(true) },
                                            onDelete = { folderMenuFor = null; actions.folders.onDelete(folder) },
                                            onRemoveFromHome = actions.folders.onRemoveFromHome?.let { remove ->
                                                { folderMenuFor = null; remove(folder) }
                                            },
                                        ),
                                        onToggleExpanded = {
                                            expanded = if (folder.id in expanded) {
                                                expanded - folder.id
                                            } else {
                                                opened = token to System.nanoTime()
                                                expanded + folder.id
                                            }
                                        },
                                        onOpenMenu = { offset -> menuOffset = offset; folderMenuFor = folder.id },
                                        onDismissMenu = { folderMenuFor = null },
                                    )
                                }
                            }
                        }
                        if (index < entries.lastIndex) Spacer(Modifier.height(settings.itemSpacingDp.dp))
                    }
                }
            }
        }
    }

    folderRenameFor?.let { folder ->
        FolderEditDialog(
            currentName = folder.name,
            hasCustomIcon = folder.icon != null,
            onConfirm = { name -> actions.folders.onRename(folder, name); folderRenameFor = null },
            onChangeIcon = { name ->
                // Picking an icon leaves this dialog for another screen, taking the half-typed
                // name with it unless it is saved on the way out.
                if (name.isNotBlank() && name.trim() != folder.name) actions.folders.onRename(folder, name.trim())
                actions.folders.onChangeIcon(folder)
                folderRenameFor = null
            },
            onResetIcon = { actions.folders.onResetIcon(folder); folderRenameFor = null },
            onDismiss = { folderRenameFor = null },
        )
    }
    renameFor?.let { app ->
        EditAppDialog(
            currentName = nameOf(app),
            onConfirmName = { name -> actions.apps.onSetName(app, name); renameFor = null },
            onChangeIcon = { name ->
                if (name.trim() != nameOf(app)) actions.apps.onSetName(app, name.trim())
                actions.apps.onChangeIcon(app)
                renameFor = null
            },
            onDismiss = { renameFor = null },
        )
    }
}
