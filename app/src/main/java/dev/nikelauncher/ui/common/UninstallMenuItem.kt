// SPDX-License-Identifier: GPL-3.0-or-later
// Adapted from Victoria Grid (GPL-3.0-or-later), ui/common/UninstallMenuItem.kt.
package dev.nikelauncher.ui.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dev.nikelauncher.NikeApp
import dev.nikelauncher.R
import dev.nikelauncher.data.AppInfo
import dev.nikelauncher.data.EntryKind

/**
 * Keeps the profile identity and lets Android show its own uninstall confirmation; never removes
 * anything itself. False for what is not an installed app, or sits in a locked private space.
 */
internal fun Context.requestUninstall(app: AppInfo): Boolean {
    val nike = applicationContext as NikeApp
    if (app.kind != EntryKind.APP || nike.appRepository.privateSpace().conceals(app.key)) return false
    val intent = Intent(Intent.ACTION_DELETE, Uri.fromParts("package", app.packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        .putExtra(Intent.EXTRA_RETURN_RESULT, false)
    app.user?.let { intent.putExtra(Intent.EXTRA_USER, it) }
    return runCatching { startActivity(intent); true }.getOrDefault(false)
}

/** The app list's menu row for it. */
@Composable
internal fun UninstallMenuItem(app: AppInfo, onDismiss: () -> Unit) {
    if (app.kind != EntryKind.APP) return
    val context = LocalContext.current
    DropdownMenuItem(
        text = { Text(stringResource(R.string.action_uninstall)) },
        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
        onClick = {
            onDismiss()
            if (!context.requestUninstall(app)) context.toast(R.string.clock_action_unavailable)
        },
    )
}
