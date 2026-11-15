// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.settings

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dev.nikelauncher.NikeApp
import dev.nikelauncher.R
import dev.nikelauncher.data.AppInfo
import dev.nikelauncher.data.EntryKeys
import dev.nikelauncher.data.PrivateSpace
import dev.nikelauncher.media.isListenerEnabled
import dev.nikelauncher.service.SystemUi

/**
 * Gathers what the settings screen needs from the rest of the app and hands it over as one
 * [SettingsEnv]. [homeTick] changes whenever the launcher is returned to, which is when the
 * answers that live outside this app (default launcher, permissions) may have changed.
 */
@Composable
fun SettingsRoute(
    app: NikeApp,
    allApps: List<AppInfo>,
    privateSpace: PrivateSpace,
    homeTick: Int,
    nav: SettingsNav,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val previews = remember { SettingsPreviews() }
    var refresh by remember { mutableIntStateOf(0) }

    val hidden by app.prefs.library.hiddenApps.collectAsState(emptySet())
    val nameOverrides by app.prefs.library.nameOverrides.collectAsState(emptyMap())
    val quickLeft by app.prefs.library.quickLaunchLeft.collectAsState(null)
    val quickRight by app.prefs.library.quickLaunchRight.collectAsState(null)
    val corner by app.prefs.library.cornerButtonKey.collectAsState(null)

    val byKey = remember(allApps) { allApps.associateBy { it.key } }
    fun labelOf(key: String?) = key?.let { byKey[it]?.let { app -> nameOverrides[app.key] ?: app.label } }

    val backup = rememberBackupActions(app, privateSpace)
    val fontImport = rememberFontImport(app)
    val actions = remember(context, backup, fontImport) {
        SettingsActions(
            onExport = backup.export,
            onImport = backup.import,
            onOpenAccessibilitySettings = { context.openSystem(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
            onOpenAppInfo = {
                context.openSystem(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.fromParts("package", context.packageName, null)),
                )
            },
            onOpenHomeSettings = {
                refresh++
                // The system screen, never a silent switch: which launcher runs is the user's to say.
                if (!context.openSystem(Intent(Settings.ACTION_HOME_SETTINGS))) {
                    context.openSystem(Intent(Settings.ACTION_SETTINGS))
                }
            },
            onOpenNotificationSettings = { context.openSystem(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
            onPickFontFile = fontImport,
        )
    }

    val env = SettingsEnv(
        prefs = app.prefs,
        scope = scope,
        previews = previews,
        nav = nav,
        actions = actions,
        iconPacks = remember { app.iconPackRepository.getInstalledIconPacks() },
        previewApp = remember(allApps) { previewApp(context, allApps) },
        // The number the hidden-apps screen itself arrives at: a hidden app inside a locked
        // private space has no row, and counting the stored set would say how many are in there.
        hiddenCount = remember(allApps, hidden) { allApps.count { it.key in hidden } },
        isDefaultLauncher = remember(homeTick, refresh) { context.isDefaultLauncher() },
        listenerEnabled = remember(homeTick) { isListenerEnabled(context) },
        shadeGestureReady = remember(homeTick) { SystemUi.canExpandShade() },
        lockGestureReady = remember(homeTick) { SystemUi.canLockScreen() },
        quickLaunchLeftLabel = labelOf(quickLeft),
        quickLaunchRightLabel = labelOf(quickRight),
        cornerButtonLabel = when (corner) {
            null -> null
            EntryKeys.SEARCH -> stringResource(R.string.settings_corner_search)
            else -> labelOf(corner)
        },
    )
    SettingsScreen(env, onBack, modifier)
}

private fun Context.openSystem(intent: Intent): Boolean =
    runCatching { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess

private fun Context.isDefaultLauncher(): Boolean = runCatching {
    val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName == packageName
}.getOrDefault(false)

/** The Settings app: a stable, recognisable icon to judge an icon size against on every device. */
private fun previewApp(context: Context, apps: List<AppInfo>): AppInfo? {
    val settingsPackage = runCatching {
        @Suppress("DEPRECATION")
        context.packageManager.resolveActivity(Intent(Settings.ACTION_SETTINGS), 0)?.activityInfo?.packageName
    }.getOrNull()
    return apps.firstOrNull { it.packageName == settingsPackage }
        ?: apps.firstOrNull { it.packageName == "com.android.settings" }
        ?: apps.firstOrNull()
}
