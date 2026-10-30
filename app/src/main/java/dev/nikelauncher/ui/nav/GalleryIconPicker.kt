// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.nav

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import dev.nikelauncher.NikeApp
import dev.nikelauncher.data.folderIdFromToken
import kotlinx.coroutines.launch

/** Starts the image picker for [key] (an app or folder token) and stores the result as its icon. */
@Composable
fun rememberGalleryIconPicker(app: NikeApp): (String) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var target by remember { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        val key = target
        if (uri != null && key != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            val folderId = folderIdFromToken(key)
            scope.launch {
                if (folderId != null) app.prefs.library.setFolderIcon(folderId, uri.toString())
                else app.prefs.library.setIconOverride(key, uri.toString())
            }
        }
        target = null
    }
    return { key ->
        target = key
        launcher.launch(arrayOf("image/*"))
    }
}
