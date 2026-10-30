// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.PhotoSizeSelectLarge
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.nikelauncher.NikeApp
import dev.nikelauncher.R
import dev.nikelauncher.data.AppInfo
import dev.nikelauncher.data.AppListBackgroundEffect
import dev.nikelauncher.data.HomeAlignment
import dev.nikelauncher.data.IconPackRepository
import dev.nikelauncher.data.IconSide
import dev.nikelauncher.data.WidgetPageSide
import dev.nikelauncher.data.WidgetPagesConfig
import dev.nikelauncher.ui.common.clearIconCache
import dev.nikelauncher.ui.kit.onboarding.SetupHeadline
import dev.nikelauncher.ui.kit.settings.ChoiceChips
import dev.nikelauncher.ui.kit.settings.SettingsGroup
import dev.nikelauncher.ui.kit.settings.SettingsSectionLabel
import dev.nikelauncher.ui.kit.settings.SettingsSlider
import dev.nikelauncher.ui.kit.settings.SettingsRow
import dev.nikelauncher.ui.kit.settings.settingsBlock
import dev.nikelauncher.ui.kit.settings.settingsSwitch
import dev.nikelauncher.ui.settings.labelRes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * How the launcher looks, with the favorites drawn above the controls so every change is seen where
 * it lands. The same preferences the settings screen writes; nothing here is a copy of them.
 */
@Composable
internal fun StyleStep(app: NikeApp, apps: List<AppInfo>, favorites: List<AppInfo>) {
    val scope = rememberCoroutineScope()
    val a = app.prefs.appearance
    val list = app.prefs.appList
    val showAppIcons by a.showAppIcons.collectAsState(true)
    val showFavoriteIcons by a.showFavoriteIcons.collectAsState(true)
    val showNames by a.showFavoriteLabels.collectAsState(true)
    val iconSize by a.iconSizeDp.collectAsState(56)
    val labelSize by a.labelSizeSp.collectAsState(16)
    val spacing by a.itemSpacingDp.collectAsState(10)
    val alignment by a.alignment.collectAsState(HomeAlignment.LEFT)
    val iconSide by a.iconSide.collectAsState(IconSide.LEFT)
    val effect by list.backgroundEffect.collectAsState(AppListBackgroundEffect.NONE)
    val strength by list.backgroundStrength.collectAsState(50)
    val grain by list.grain.collectAsState(0)
    val search by list.appListSearchEnabled.collectAsState(false)
    val pages by app.prefs.pages.config.collectAsState(WidgetPagesConfig())

    Column(Modifier.fillMaxSize()) {
        SetupHeadline(stringResource(R.string.setup_style), Modifier.padding(top = 12.dp, bottom = 12.dp))
        StylePreview(app, apps, favorites)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SettingsSectionLabel(stringResource(R.string.settings_section_home))
            SettingsGroup(buildList<SettingsRow> {
                add(settingsBlock(stringResource(R.string.settings_icon_size), Icons.Filled.PhotoSizeSelectLarge, "$iconSize dp") {
                    SettingsSlider(iconSize.toFloat(), 24f..96f, { v -> scope.launch { a.setIconSizeDp(v.toInt()) } })
                })
                add(settingsBlock(stringResource(R.string.settings_text_size), Icons.Filled.FormatSize, "$labelSize sp") {
                    SettingsSlider(labelSize.toFloat(), 10f..32f, { v -> scope.launch { a.setLabelSizeSp(v.toInt()) } })
                })
                add(settingsBlock(stringResource(R.string.settings_favorite_spacing), Icons.Filled.SpaceBar, "$spacing dp") {
                    SettingsSlider(spacing.toFloat(), 0f..48f, { v -> scope.launch { a.setItemSpacingDp(v.toInt()) } })
                })
                add(settingsSwitch(stringResource(R.string.settings_icons_favorites), showFavoriteIcons, { v -> scope.launch { a.setShowFavoriteIcons(v) } }, Icons.Filled.Apps))
                add(settingsSwitch(stringResource(R.string.settings_show_names), showNames, { v -> scope.launch { a.setShowFavoriteLabels(v) } }, Icons.Filled.Label))
                add(settingsBlock(stringResource(R.string.settings_alignment_favorites)) {
                    ChoiceChips(HomeAlignment.entries, alignment, { stringResource(it.labelRes()) }, { v -> scope.launch { a.setAlignment(v) } })
                })
                add(settingsBlock(stringResource(R.string.settings_icon_side)) {
                    ChoiceChips(IconSide.entries, iconSide, { stringResource(it.labelRes()) }, { v -> scope.launch { a.setIconSide(v) } })
                })
            })

            SettingsSectionLabel(stringResource(R.string.settings_section_app_list))
            SettingsGroup(buildList<SettingsRow> {
                add(settingsSwitch(stringResource(R.string.settings_icons_app_list), showAppIcons, { v -> scope.launch { a.setShowAppIcons(v) } }, Icons.Filled.Apps))
                add(settingsSwitch(stringResource(R.string.settings_search_bar), search, { v -> scope.launch { list.setAppListSearchEnabled(v) } }, Icons.Filled.Search))
                add(settingsSwitch(
                    stringResource(R.string.settings_app_list_effect_blur),
                    effect == AppListBackgroundEffect.BLUR,
                    { on -> scope.launch { list.setBackgroundEffect(if (on) AppListBackgroundEffect.BLUR else AppListBackgroundEffect.NONE) } },
                    Icons.Filled.BlurOn,
                ))
                if (effect == AppListBackgroundEffect.BLUR) {
                    add(settingsBlock(stringResource(R.string.settings_app_list_effect_strength), value = "$strength%") {
                        SettingsSlider(strength.toFloat(), 0f..100f, { v -> scope.launch { list.setBackgroundStrength(v.toInt()) } })
                    })
                }
                add(settingsBlock(stringResource(R.string.settings_app_list_grain), Icons.Filled.Grain, "$grain%") {
                    SettingsSlider(grain.toFloat(), 0f..100f, { v -> scope.launch { list.setGrain(v.toInt()) } })
                })
            })

            SettingsSectionLabel(stringResource(R.string.settings_section_widgets))
            SettingsGroup(buildList<SettingsRow> {
                add(settingsSwitch(stringResource(R.string.widget_pages_left), pages.leftEnabled, { v -> scope.launch { app.prefs.pages.setEnabled(WidgetPageSide.LEFT, v) } }, Icons.Filled.ViewCarousel))
                add(settingsSwitch(stringResource(R.string.widget_pages_right), pages.rightEnabled, { v -> scope.launch { app.prefs.pages.setEnabled(WidgetPageSide.RIGHT, v) } }, Icons.Filled.ViewCarousel))
            })

            IconPackChoice(app)
        }
    }
}

/** The icon pack, when the phone has any beyond the stock icons. */
@Composable
private fun IconPackChoice(app: NikeApp) {
    val scope = rememberCoroutineScope()
    val selected by app.prefs.appearance.iconPackPackage.collectAsState(null)
    val packs by produceState(emptyList<IconPackRepository.IconPackInfo>()) {
        value = withContext(Dispatchers.IO) { app.iconPackRepository.getInstalledIconPacks() }
    }
    if (packs.isEmpty()) return
    SettingsGroup(buildList<SettingsRow> {
        add(settingsBlock(stringResource(R.string.settings_icon_pack)) {
            ChoiceChips(
                options = listOf<String?>(null) + packs.map { it.packageName },
                selected = selected,
                label = { pkg -> packs.firstOrNull { it.packageName == pkg }?.label ?: stringResource(R.string.settings_icon_pack_default) },
                onSelect = { pkg -> scope.launch { app.prefs.appearance.setIconPackPackage(pkg); clearIconCache() } },
            )
        })
    })
}
