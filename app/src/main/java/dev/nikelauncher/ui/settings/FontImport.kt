// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.settings

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import dev.nikelauncher.NikeApp
import dev.nikelauncher.R
import dev.nikelauncher.data.AppFont
import dev.nikelauncher.ui.common.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Copies a picked font into app storage and selects it.
 *
 * Copied in rather than referenced: a document URI is only as durable as the file behind it, and a
 * font picked from Downloads would break the moment it was moved. The copy gets a new name every
 * time, because the font on screen is mapped from its file and writing the next one over it
 * garbled the text until a restart.
 */
@Composable
fun rememberFontImport(app: NikeApp): (Uri) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return { uri ->
        scope.launch {
            val path = withContext(Dispatchers.IO) {
                runCatching {
                    val out = File(context.filesDir, "custom_font_${System.currentTimeMillis()}")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        out.outputStream().use { input.copyTo(it) }
                    } ?: return@runCatching null
                    // Proves it parses before anything starts drawing with it.
                    runCatching { android.graphics.Typeface.createFromFile(out) }
                        .onFailure { out.delete() }.getOrThrow()
                    context.filesDir.listFiles()
                        ?.filter { it.name.startsWith("custom_font") && it != out }
                        ?.forEach { it.delete() }
                    out.absolutePath
                }.getOrNull()
            }
            if (path == null) {
                context.toast(R.string.font_custom_failed)
            } else {
                app.prefs.appearance.setFontFile(path)
                app.prefs.appearance.setFont(AppFont.CUSTOM)
            }
        }
    }
}
