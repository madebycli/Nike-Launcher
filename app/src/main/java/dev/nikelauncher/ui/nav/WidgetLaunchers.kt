// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.nav

import android.app.Activity.RESULT_OK
import android.appwidget.AppWidgetManager
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import dev.nikelauncher.NikeApp
import dev.nikelauncher.widget.WidgetPickerActivity
import dev.nikelauncher.widget.WidgetSlotActions
import kotlinx.coroutines.launch

/** What the widget slot and the folders can ask for, wired to the pickers and the preferences. */
@Composable
fun rememberWidgetActions(app: NikeApp, onOpenSettings: () -> Unit): WidgetSlotActions {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val id = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
            // Appended rather than replacing: picking a widget adds a page.
            if (id != -1) scope.launch { app.prefs.home.addWidgetId(id) }
        }
    }

    // The folder a widget being picked is for. Held here rather than passed through the picker,
    // which only knows about widget ids.
    var folderTarget by rememberSaveable { mutableStateOf<String?>(null) }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val folderId = folderTarget
        folderTarget = null
        if (result.resultCode == RESULT_OK && folderId != null) {
            val id = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
            if (id != -1) scope.launch { app.prefs.library.addFolderWidget(folderId, id) }
        }
    }

    return remember(app) {
        WidgetSlotActions(
            onAddWidget = { picker.launch(Intent(context, WidgetPickerActivity::class.java)) },
            onRemoveWidget = { id ->
                scope.launch {
                    // Releasing the host id matters: left allocated, its provider keeps
                    // broadcasting updates to a widget nobody can see.
                    if (id > 0) app.widgetHost.deleteAppWidgetId(id)
                    app.prefs.home.removeWidgetId(id)
                }
            },
            onWidgetSettings = { id ->
                val configure = AppWidgetManager.getInstance(context).getAppWidgetInfo(id)?.configure
                if (configure != null) {
                    val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                        component = configure
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                    }
                    runCatching { context.startActivity(intent) }
                }
            },
            onAppInfo = { id ->
                AppWidgetManager.getInstance(context).getAppWidgetInfo(id)?.let { app.appRepository.openAppInfo(it.provider.packageName) }
            },
            onResize = { height -> scope.launch { app.prefs.home.setWidgetHeightDp(height) } },
            onOpenSettings = onOpenSettings,
            onAddWidgetToFolder = { folderId ->
                folderTarget = folderId
                folderPicker.launch(Intent(context, WidgetPickerActivity::class.java))
            },
            onRemoveFolderWidget = { folderId, id ->
                scope.launch {
                    if (id > 0) runCatching { app.widgetHost.deleteAppWidgetId(id) }
                    app.prefs.library.removeFolderWidget(folderId, id)
                }
            },
            onResizeFolderWidget = { folderId, id, height ->
                scope.launch { app.prefs.library.setFolderWidgetHeight(folderId, id, height) }
            },
            onSetFolderNowPlaying = { folderId, on -> scope.launch { app.prefs.library.setFolderNowPlaying(folderId, on) } },
        )
    }
}
