// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.setup

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import dev.nikelauncher.R
import dev.nikelauncher.media.isListenerEnabled
import dev.nikelauncher.ui.kit.settings.SettingsGroup
import dev.nikelauncher.ui.kit.settings.item
import kotlinx.coroutines.launch

/** What the launcher can be given: the home role, notification access, the calendar. */
@Composable
internal fun AccessStep(resumed: Int) {
    val context = LocalContext.current
    val homeGranted = remember(resumed) {
        if (Build.VERSION.SDK_INT >= 29) context.getSystemService(RoleManager::class.java)?.isRoleHeld(RoleManager.ROLE_HOME) == true
        else {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            @Suppress("DEPRECATION")
            context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName == context.packageName
        }
    }
    val listenerGranted = remember(resumed) { isListenerEnabled(context) }
    val calendarGranted = remember(resumed) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
    }
    var asked by remember { mutableIntStateOf(0) }
    val askHome = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { asked++ }
    val askCalendar = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { asked++ }
    val tint = MaterialTheme.colorScheme.primary
    val done: @Composable () -> Unit = { Icon(Icons.Filled.Check, contentDescription = null, tint = tint) }
    val homeTitle = stringResource(R.string.settings_default_launcher)
    val notificationsTitle = stringResource(R.string.settings_section_notifications)
    val calendarTitle = stringResource(R.string.setup_calendar)

    SetupPage(R.string.setup_access) {
        SettingsGroup {
            item(
                homeTitle,
                icon = Icons.Filled.Home,
                trailing = if (homeGranted) done else null,
                onClick = {
                    val intent = if (Build.VERSION.SDK_INT >= 29) {
                        val manager = context.getSystemService(RoleManager::class.java)
                        if (manager != null && manager.isRoleAvailable(RoleManager.ROLE_HOME)) manager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                        else Intent(Settings.ACTION_HOME_SETTINGS)
                    } else Intent(Settings.ACTION_HOME_SETTINGS)
                    runCatching { askHome.launch(intent) }
                },
            )
            item(
                notificationsTitle,
                icon = Icons.Filled.Notifications,
                trailing = if (listenerGranted) done else null,
                onClick = { runCatching { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) } },
            )
            item(
                calendarTitle,
                icon = Icons.Filled.Event,
                trailing = if (calendarGranted) done else null,
                onClick = { askCalendar.launch(Manifest.permission.READ_CALENDAR) },
            )
        }
        DefaultAppsChoice()
    }
}

