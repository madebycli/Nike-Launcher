// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dev.nikelauncher.R

/** Shared by setup and per-widget app choices. A missing package retains its stored identity. */
@Composable
internal fun clockAppLabel(packageName: String?): String {
    val context = LocalContext.current
    val default = stringResource(R.string.widget_settings_system_app)
    return remember(context, packageName, default) {
        packageName?.let {
            runCatching { context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(it, 0)).toString() }.getOrDefault(it)
        } ?: default
    }
}
