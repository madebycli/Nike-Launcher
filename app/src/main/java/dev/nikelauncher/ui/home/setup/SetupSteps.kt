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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Rocket
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import dev.nikelauncher.NikeApp
import dev.nikelauncher.R
import dev.nikelauncher.media.isListenerEnabled
import dev.nikelauncher.ui.apps.AppsState
import dev.nikelauncher.ui.kit.onboarding.IconBlob
import dev.nikelauncher.ui.kit.onboarding.SetupHeadline
import dev.nikelauncher.ui.kit.settings.SettingsGroup
import dev.nikelauncher.ui.kit.settings.SettingsSlider
import dev.nikelauncher.ui.kit.settings.block
import dev.nikelauncher.ui.kit.settings.item
import dev.nikelauncher.ui.kit.settings.switchItem
import dev.nikelauncher.ui.settings.FavoritesChooser
import dev.nikelauncher.ui.state.LibraryState
import kotlinx.coroutines.launch

@Composable
internal fun WelcomeStep(onRestore: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Hero(Icons.Filled.Rocket, R.string.app_name, headlineSize = true)
        TextButton(onClick = onRestore, modifier = Modifier.align(Alignment.BottomCenter)) {
            Text(stringResource(R.string.setup_restore))
        }
    }
}

@Composable
internal fun DoneStep() = Hero(Icons.Filled.CheckCircle, R.string.setup_ready, headlineSize = false)

@Composable
private fun Hero(icon: ImageVector, title: Int, headlineSize: Boolean) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconBlob(icon, size = 112.dp, iconSize = 56.dp)
        Spacer(Modifier.height(24.dp))
        Text(
            stringResource(title),
            style = if (headlineSize) MaterialTheme.typography.displayMedium else MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

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
    }
}

/** The few looks that change what the home screen is like at a glance. */
@Composable
internal fun StyleStep(app: NikeApp) {
    val scope = rememberCoroutineScope()
    val showIcons by app.prefs.appearance.showAppIcons.collectAsState(true)
    val iconSize by app.prefs.appearance.iconSizeDp.collectAsState(56)
    val search by app.prefs.appList.appListSearchEnabled.collectAsState(false)
    val iconsTitle = stringResource(R.string.settings_icons_app_list)
    val sizeTitle = stringResource(R.string.settings_icon_size)
    val searchTitle = stringResource(R.string.settings_search_bar)
    SetupPage(R.string.setup_style) {
        SettingsGroup {
            switchItem(iconsTitle, showIcons, { v -> scope.launch { app.prefs.appearance.setShowAppIcons(v) } })
            block(sizeTitle, value = "$iconSize dp") {
                SettingsSlider(iconSize.toFloat(), 24f..96f, { v -> scope.launch { app.prefs.appearance.setIconSizeDp(v.toInt()) } })
            }
            switchItem(searchTitle, search, { v -> scope.launch { app.prefs.appList.setAppListSearchEnabled(v) } })
        }
    }
}

/** Pick the favorites: search the list, tick, drag the chosen ones into order. */
@Composable
internal fun FavoritesStep(app: NikeApp, apps: AppsState, library: LibraryState, iconSizeDp: Int) {
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize()) {
        SetupHeadline(stringResource(R.string.favorites_title), Modifier.padding(top = 20.dp, bottom = 8.dp))
        FavoritesChooser(
            allApps = apps.apps,
            favoriteKeys = library.favoriteKeys,
            folders = library.folders,
            privateSpace = apps.privateSpace,
            nameOverrides = library.nameOverrides,
            iconSizeDp = iconSizeDp,
            onSetFavorite = { appInfo, add ->
                scope.launch { if (add) app.prefs.library.addFavorite(appInfo.key) else app.prefs.library.removeFavorite(appInfo.key) }
            },
            onForget = { key -> scope.launch { app.prefs.library.removeFavorite(key) } },
            onReorder = { keys -> scope.launch { app.prefs.library.setFavorites(keys) } },
            modifier = Modifier.weight(1f),
        )
    }
}

/** A page with its headline over a scrolling column. */
@Composable
private fun SetupPage(title: Int, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SetupHeadline(stringResource(title), Modifier.padding(top = 20.dp))
        content()
    }
}
