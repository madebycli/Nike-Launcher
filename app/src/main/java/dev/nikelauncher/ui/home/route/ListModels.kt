// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import dev.nikelauncher.NikeApp
import dev.nikelauncher.R
import dev.nikelauncher.data.AppInfo
import dev.nikelauncher.data.Folder
import dev.nikelauncher.ui.applist.AppListModel
import dev.nikelauncher.ui.applist.buildAppListModel
import dev.nikelauncher.ui.home.HomeSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The models the app list draws from, and the one a search runs against. */
internal class ListModels(val list: AppListModel, val search: AppListModel)

/**
 * Grouping, sorting and flattening every installed app is too much to do in composition, and it
 * re-runs whenever a name override changes, so it is done off the main thread.
 *
 * Hidden apps are left out of the list's model, so reaching them by name needs a model of its own;
 * it is only built when the setting asks for it.
 */
@Composable
internal fun rememberListModels(
    app: NikeApp,
    settings: HomeSettings,
    appsByKey: Map<String, AppInfo>,
    hiddenApps: Set<String>,
    nameOverrides: Map<String, String>,
    launchCounts: Map<String, Int>,
    folders: List<Folder>,
    openFolders: Set<String>,
): ListModels {
    // Read here because the models are built off the main thread, where a string resource cannot be asked for.
    val privateLabel = stringResource(R.string.applist_private_space_section).takeIf { settings.privateSpaceSection }
    val recentArg = if (settings.recentSection) stringResource(R.string.applist_recent_section) to settings.recentSectionCount else null
    val japanese = LocalConfiguration.current.locales[0].language == "ja"
    val listFolders = if (settings.foldersInAppList) folders else emptyList()
    val counts = if (settings.sortByUsage) launchCounts else emptyMap()
    val includeHidden = settings.appListSearchHidden && hiddenApps.isNotEmpty()

    suspend fun build(hidden: Set<String>): AppListModel = withContext(Dispatchers.Default) {
        buildAppListModel(
            appsByKey.values.toList(),
            hidden,
            { nameOverrides[it.key] ?: it.label },
            counts,
            englishName = { app.appRepository.englishLabel(it) },
            showHeaders = settings.showListHeaders,
            renamed = { it.key in nameOverrides },
            privateSectionLabel = privateLabel,
            recentSection = recentArg,
            japanese = japanese,
            folders = listFolders,
            expandedFolders = openFolders,
        )
    }

    val list: State<AppListModel> = produceState(
        AppListModel(emptyList(), emptyList()),
        appsByKey, hiddenApps, nameOverrides, counts, settings.showListHeaders, listFolders, openFolders,
    ) { value = build(hiddenApps) }

    val hiddenModel: State<AppListModel?> = produceState<AppListModel?>(
        null,
        appsByKey, nameOverrides, includeHidden, counts, settings.showListHeaders, listFolders, openFolders,
    ) { value = if (includeHidden) build(emptySet()) else null }

    return ListModels(list.value, hiddenModel.value ?: list.value)
}
