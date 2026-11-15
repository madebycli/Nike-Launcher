// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.media

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.nikelauncher.NikeApp
import dev.nikelauncher.R
import dev.nikelauncher.data.prefs.*
import dev.nikelauncher.ui.kit.settings.ChoiceChips
import dev.nikelauncher.ui.kit.settings.SettingsGroup
import dev.nikelauncher.ui.kit.settings.switchItem
import kotlinx.coroutines.launch

@Composable
internal fun MediaWidgetSettingsSheet(onDismiss: () -> Unit) {
    val prefs = (LocalContext.current.applicationContext as NikeApp).prefs.mediaWidget
    val options by prefs.options.collectAsState(MediaWidgetOptions())
    val scope = rememberCoroutineScope()
    val controls = stringResource(R.string.media_widget_controls)
    val progress = stringResource(R.string.media_widget_progress)
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.media_widget_settings), style = MaterialTheme.typography.titleLarge)
            ChoiceChips(MediaWidgetStyle.entries, options.style, { style ->
                stringResource(if (style == MediaWidgetStyle.CLASSIC) R.string.media_widget_classic else R.string.media_widget_expressive)
            }, { style -> scope.launch { prefs.setStyle(style) } })
            SettingsGroup {
                switchItem(controls, options.controls, { value -> scope.launch { prefs.setControls(value) } })
                switchItem(progress, options.progress, { value -> scope.launch { prefs.setProgress(value) } })
            }
        }
    }
}
