// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.notifications

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.nikelauncher.NikeApp
import dev.nikelauncher.R
import dev.nikelauncher.data.AppInfo
import dev.nikelauncher.media.NotificationPreviewBus
import dev.nikelauncher.media.NowPlayingListenerService
import dev.nikelauncher.media.isListenerEnabled
import kotlinx.coroutines.launch

/** Whether notification access is granted, read again every time the app comes back to the front. */
@Composable
private fun notificationAccess(): Boolean {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var granted by remember { mutableStateOf(isListenerEnabled(context)) }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                granted = isListenerEnabled(context)
                if (granted) NowPlayingListenerService.rebindIfPermitted(context)
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    return granted
}

/**
 * The notifications on the shade, for one app or for all of them when [app] is null: a list, and
 * a page for each with its pictures, its conversation, its actions and a reply field.
 *
 * Content lives in memory only, while the phone is unlocked and the setting is on.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NotificationSheet(
    app: AppInfo? = null,
    initialKey: String? = null,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val prefs = (context.applicationContext as NikeApp).prefs
    // Until the setting has been read, assume it is on: starting as "off" shows the permission
    // button first and resizes the sheet the moment it is up.
    val enabled by prefs.appList.notificationPreviews.collectAsState(true)
    val access = notificationAccess()
    val notifications by NotificationPreviewBus.notifications.collectAsState()
    val shown = remember(notifications, app) { notifications.filter { app == null || it.packageName == app.packageName } }
    val controller = remember(context) { BusNotificationController(context) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val available = enabled && access
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().heightIn(max = (LocalConfiguration.current.screenHeightDp * .83f).dp)) {
            if (!available) {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    FilledTonalButton(onClick = {
                        scope.launch { prefs.appList.setNotificationPreviews(true) }
                        if (!access) context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }) { Text(stringResource(R.string.notification_allow_access)) }
                }
            } else {
                NotificationSheetContent(shown, initialKey, controller, onDismiss)
            }
        }
    }
}
