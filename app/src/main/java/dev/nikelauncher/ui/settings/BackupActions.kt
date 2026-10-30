// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import dev.nikelauncher.NikeApp
import dev.nikelauncher.R
import dev.nikelauncher.data.PrivateSpace
import dev.nikelauncher.data.stripsOtherProfiles
import dev.nikelauncher.ui.common.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Starts the document picker for saving or restoring the settings file. */
class BackupActions(val export: () -> Unit, val import: () -> Unit)

/**
 * Written through the document picker rather than to a path of our own: the file is the user's
 * to keep, and this way it lands wherever they keep things without the app asking for storage.
 */
@Composable
fun rememberBackupActions(app: NikeApp, privateSpace: PrivateSpace): BackupActions {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            // Every state but Absent strips, including the ones with no serial to strip by: an
            // unreadable space is the case most in need of it.
            val stripOtherProfiles = privateSpace.stripsOtherProfiles
            val omittedPrivateSpace = withContext(Dispatchers.IO) {
                runCatching {
                    val result = app.prefs.exportJson(stripOtherProfiles)
                    context.contentResolver.openOutputStream(uri)?.use { it.write(result.json.toByteArray()) }
                        ?: return@runCatching null
                    result.omittedPrivateSpace
                }.getOrNull()
            }
            context.toast(
                when (omittedPrivateSpace) {
                    null -> R.string.settings_backup_failed
                    true -> R.string.settings_export_done_private_omitted
                    false -> R.string.settings_export_done
                },
            )
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }.getOrNull()
            }
            val ok = text != null && app.prefs.importJson(text)
            context.toast(if (ok) R.string.settings_import_done else R.string.settings_backup_failed)
        }
    }
    return BackupActions(
        export = { exportLauncher.launch("nike-launcher-settings.json") },
        import = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
    )
}
