// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.route

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import dev.nikelauncher.NikeApp
import dev.nikelauncher.data.AppInfo
import dev.nikelauncher.data.Folder
import dev.nikelauncher.data.folderToken
import dev.nikelauncher.ui.common.FolderPickerDialog
import kotlinx.coroutines.launch

/** Asks which folder [target] should go into, or for the name of a new one. */
@Composable
internal fun FolderPickerHost(
    app: NikeApp,
    target: AppInfo,
    folders: List<Folder>,
    appsByKey: Map<String, AppInfo>,
    nameOverrides: Map<String, String>,
    onDone: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    FolderPickerDialog(
        appLabel = nameOverrides[target.key] ?: target.label,
        folders = folders,
        // The rows this launcher can actually draw, which is what the home screen's own badge
        // counts. A folder's stored keys include whatever a locked private space is hiding.
        memberCount = { folder -> folder.apps.count { it in appsByKey } },
        onPickFolder = { folder ->
            scope.launch { app.prefs.library.addAppToFolder(folder.id, target.key) }
            onDone()
        },
        onCreateFolder = { name ->
            scope.launch {
                val folder = Folder(id = System.currentTimeMillis().toString(36), name = name, apps = listOf(target.key))
                app.prefs.library.upsertFolder(folder)
                // Drop the app's own row and give the folder one in its place.
                app.prefs.library.removeFavorite(target.key)
                app.prefs.library.addFavorite(folderToken(folder.id))
            }
            onDone()
        },
        onDismiss = onDone,
    )
}

/** Consumes every touch, for the home screen while the list is up in front of it. */
internal suspend fun PointerInputScope.swallowTouches() {
    awaitEachGesture {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            event.changes.forEach { it.consume() }
            if (event.changes.none { it.pressed }) break
        }
    }
}
