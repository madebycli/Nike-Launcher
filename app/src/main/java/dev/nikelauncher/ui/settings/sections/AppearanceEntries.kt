// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.settings.sections

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import dev.nikelauncher.R
import dev.nikelauncher.data.AppFont
import dev.nikelauncher.data.HomeAlignment
import dev.nikelauncher.data.IconShape
import dev.nikelauncher.data.IconSide
import dev.nikelauncher.data.TextColorMode
import dev.nikelauncher.ui.common.AppIcon
import dev.nikelauncher.ui.common.clearIconCache
import dev.nikelauncher.ui.kit.settings.ChoiceChips
import dev.nikelauncher.ui.kit.settings.ColorPickerSheet
import dev.nikelauncher.ui.kit.settings.SettingsSlider
import dev.nikelauncher.ui.kit.settings.settingsBlock
import dev.nikelauncher.ui.kit.settings.settingsItem
import dev.nikelauncher.ui.kit.settings.settingsSwitch
import dev.nikelauncher.ui.settings.SettingsEntry
import dev.nikelauncher.ui.settings.SettingsEnv
import dev.nikelauncher.ui.settings.SettingsSection
import dev.nikelauncher.ui.settings.labelRes
import dev.nikelauncher.ui.theme.SHADOW_STRENGTH_RANGE
import dev.nikelauncher.ui.theme.fontFamilyOf
import dev.nikelauncher.ui.theme.rememberWallpaperPalette
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.toArgb

private val SECTION = SettingsSection.APPEARANCE

@Composable
internal fun appearanceEntries(env: SettingsEnv): List<SettingsEntry> {
    val a = env.prefs.appearance
    val iconSize by a.iconSizeDp.collectAsState(56)
    val labelSize by a.labelSizeSp.collectAsState(16)
    val spacing by a.itemSpacingDp.collectAsState(10)
    val font by a.font.collectAsState(AppFont.SYSTEM)
    val fontFile by a.fontFile.collectAsState(null)
    val textColorMode by a.textColorMode.collectAsState(TextColorMode.AUTO)
    val textColorCustom by a.textColorCustom.collectAsState(0xFFFFFFFF.toInt())
    val iconShape by a.iconShape.collectAsState(IconShape.SYSTEM)
    val themed by a.themedIcons.collectAsState(false)
    val iconPack by a.iconPackPackage.collectAsState(null)
    val showAppIcons by a.showAppIcons.collectAsState(true)
    val showFavoriteIcons by a.showFavoriteIcons.collectAsState(true)
    val showLabels by a.showFavoriteLabels.collectAsState(true)
    val alignment by a.alignment.collectAsState(HomeAlignment.LEFT)
    val listAlignment by a.appListAlignment.collectAsState(HomeAlignment.LEFT)
    val iconSide by a.iconSide.collectAsState(IconSide.LEFT)
    val dimHome by a.dimHomeAlpha.collectAsState(0f)
    val dimList by a.dimWallpaperAlpha.collectAsState(0.35f)
    val dimColor by a.dimColor.collectAsState(0xFF000000.toInt())
    val textShadow by a.textShadow.collectAsState(false)
    val shadowStrength by a.textShadowStrength.collectAsState(5)

    var pickingText by remember { mutableStateOf(false) }
    var pickingDim by remember { mutableStateOf(false) }
    val palette = rememberWallpaperPalette() ?: MaterialTheme.colorScheme
    val wallpaperColors = listOf(
        palette.primary, palette.secondary, palette.tertiary,
        palette.primaryContainer, palette.surfaceVariant, palette.surface,
    )
    if (pickingText) {
        ColorPickerSheet(
            initial = Color(textColorCustom),
            wallpaperColors = wallpaperColors,
            onPick = { color -> env.write { a.setTextColorCustom(color.toArgb()) } },
            onDismiss = { pickingText = false },
        )
    }
    if (pickingDim) {
        ColorPickerSheet(
            initial = Color(dimColor),
            wallpaperColors = wallpaperColors,
            onPick = { color -> env.write { a.setDimColor(color.toArgb()) } },
            onDismiss = { pickingDim = false },
        )
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) env.actions.onPickFontFile(uri)
    }

    return listOf(
        SettingsEntry(SECTION, settingsBlock(stringResource(R.string.settings_preview)) {
            RowPreview(env, iconSize, labelSize, spacing, font, fontFile)
        }),
        SettingsEntry(SECTION, settingsBlock(stringResource(R.string.settings_icon_pack)) {
            val packs = env.iconPacks
            val options = buildList {
                add(IconChoice.DEFAULT)
                add(IconChoice.THEMED)
                packs.forEach { add(IconChoice.Pack(it.packageName, it.label)) }
                add(IconChoice.NONE)
            }
            val current = when {
                !showAppIcons && !showFavoriteIcons -> IconChoice.NONE
                themed -> IconChoice.THEMED
                iconPack != null -> options.filterIsInstance<IconChoice.Pack>().firstOrNull { it.packageName == iconPack } ?: IconChoice.DEFAULT
                else -> IconChoice.DEFAULT
            }
            ChoiceChips(
                options = options,
                selected = current,
                label = { choice -> choice.label() },
                onSelect = { choice ->
                    env.write {
                        when (choice) {
                            IconChoice.NONE -> {
                                a.setShowAppIcons(false); a.setShowFavoriteIcons(false)
                            }
                            IconChoice.DEFAULT -> {
                                a.setShowAppIcons(true); a.setShowFavoriteIcons(true)
                                a.setThemedIcons(false); a.setIconPackPackage(null)
                            }
                            IconChoice.THEMED -> {
                                a.setShowAppIcons(true); a.setIconPackPackage(null); a.setThemedIcons(true)
                            }
                            is IconChoice.Pack -> {
                                a.setShowAppIcons(true); a.setThemedIcons(false); a.setIconPackPackage(choice.packageName)
                            }
                        }
                        clearIconCache()
                    }
                },
            )
        }, keywords = "icons themed pack"),
        SettingsEntry(SECTION, settingsSwitch(stringResource(R.string.settings_icons_favorites), showFavoriteIcons) { env.write { a.setShowFavoriteIcons(it) } }, "icons"),
        SettingsEntry(SECTION, settingsSwitch(stringResource(R.string.settings_icons_app_list), showAppIcons) { env.write { a.setShowAppIcons(it) } }, "icons"),
        SettingsEntry(SECTION, settingsBlock(stringResource(R.string.settings_icon_size), value = "${iconSize}dp") {
            SettingsSlider(iconSize.toFloat(), 12f..96f, { env.write { a.setIconSizeDp(it.toInt()) } })
        }, "size"),
        SettingsEntry(SECTION, settingsBlock(stringResource(R.string.settings_text_size), value = "${labelSize}sp") {
            SettingsSlider(labelSize.toFloat(), 10f..28f, { env.write { a.setLabelSizeSp(it.toInt()) } })
        }, "font label size"),
        SettingsEntry(SECTION, settingsBlock(stringResource(R.string.settings_favorite_spacing), value = "${spacing}dp") {
            SettingsSlider(spacing.toFloat(), 0f..40f, { env.write { a.setItemSpacingDp(it.toInt()) } })
        }, "gap spacing"),
        SettingsEntry(SECTION, settingsBlock(stringResource(R.string.settings_font)) {
            ChoiceChips(
                options = AppFont.entries,
                selected = font,
                label = { stringResource(it.labelRes()) },
                fontFamily = { fontFamilyOf(it, fontFile) },
                onSelect = { choice ->
                    // Nothing to select until there is a file, so the first tap asks for one.
                    if (choice == AppFont.CUSTOM && fontFile == null) picker.launch(arrayOf("*/*"))
                    else env.write { a.setFont(choice) }
                },
            )
            if (font == AppFont.CUSTOM) {
                TextButton(onClick = { picker.launch(arrayOf("*/*")) }) { Text(stringResource(R.string.font_custom_pick)) }
            }
        }, "typeface"),
        SettingsEntry(SECTION, settingsBlock(stringResource(R.string.settings_text_color)) {
            ChoiceChips(
                options = TextColorMode.entries,
                selected = textColorMode,
                label = { stringResource(it.labelRes()) },
                onSelect = { mode ->
                    env.write { a.setTextColorMode(mode) }
                    if (mode == TextColorMode.CUSTOM) pickingText = true
                },
            )
            if (textColorMode == TextColorMode.CUSTOM) {
                TextButton(onClick = { pickingText = true }) { Text(stringResource(R.string.text_color_pick)) }
            }
        }, "colour"),
        SettingsEntry(SECTION, settingsBlock(stringResource(R.string.icon_shape)) {
            ChoiceChips(
                options = IconShape.entries,
                selected = iconShape,
                label = { stringResource(it.labelRes()) },
                onSelect = { env.write { a.setIconShape(it); clearIconCache() } },
            )
        }, "shape"),
        SettingsEntry(SECTION, settingsBlock(stringResource(R.string.settings_alignment_favorites)) {
            ChoiceChips(HomeAlignment.entries, alignment, { stringResource(it.labelRes()) }, { env.write { a.setAlignment(it) } })
        }, "align left right center"),
        SettingsEntry(SECTION, settingsBlock(stringResource(R.string.settings_alignment_applist)) {
            ChoiceChips(HomeAlignment.entries, listAlignment, { stringResource(it.labelRes()) }, { env.write { a.setAppListAlignment(it) } })
        }, "align left right center"),
        SettingsEntry(SECTION, settingsBlock(stringResource(R.string.settings_icon_side)) {
            ChoiceChips(IconSide.entries, iconSide, { stringResource(it.labelRes()) }, { env.write { a.setIconSide(it) } })
        }, "left right"),
        SettingsEntry(SECTION, settingsSwitch(stringResource(R.string.settings_show_names), showLabels) { env.write { a.setShowFavoriteLabels(it) } }, "labels"),
        SettingsEntry(
            SECTION,
            settingsItem(
                stringResource(R.string.settings_dim_color),
                onClick = { pickingDim = true },
                value = stringResource(R.string.settings_dim_color_detail),
            ),
            keywords = "colour wallpaper",
            visible = dimHome > 0f || dimList > 0f,
        ),
        SettingsEntry(SECTION, settingsSwitch(stringResource(R.string.settings_text_shadow), textShadow, { env.write { a.setTextShadow(it) } }, value = stringResource(R.string.settings_text_shadow_detail)), "contrast readability wallpaper"),
        SettingsEntry(
            SECTION,
            settingsBlock(stringResource(R.string.settings_text_shadow_strength), value = shadowStrength.toString()) {
                SettingsSlider(
                    shadowStrength.toFloat(),
                    SHADOW_STRENGTH_RANGE.first.toFloat()..SHADOW_STRENGTH_RANGE.last.toFloat(),
                    { env.write { a.setTextShadowStrength(it.toInt()) } },
                )
            },
            keywords = "contrast readability wallpaper",
            visible = textShadow,
        ),
    )
}

/** What an icon-pack choice stands for: the stock icons, the themed ones, a pack, or none. */
private sealed interface IconChoice {
    data object DEFAULT : IconChoice
    data object THEMED : IconChoice
    data object NONE : IconChoice
    data class Pack(val packageName: String, val name: String) : IconChoice

    @Composable
    fun label(): String = when (this) {
        DEFAULT -> stringResource(R.string.settings_icon_pack_default)
        THEMED -> stringResource(R.string.settings_icon_pack_themed)
        NONE -> stringResource(R.string.settings_icon_pack_no_icons)
        is Pack -> name
    }
}

@Composable
private fun RowPreview(env: SettingsEnv, iconSizeDp: Int, labelSizeSp: Int, spacingDp: Int, font: AppFont, fontFile: String?) {
    val family = fontFamilyOf(font, fontFile)
    // Two rows, because one cannot show the gap between them.
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        repeat(2) { index ->
            if (index > 0) androidx.compose.foundation.layout.Spacer(Modifier.height(spacingDp.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val app = env.previewApp
                if (app != null) {
                    AppIcon(app = app, sizeDp = iconSizeDp)
                    androidx.compose.foundation.layout.Spacer(Modifier.width(16.dp))
                    Text(app.label, fontSize = labelSizeSp.sp, fontFamily = family)
                } else {
                    Text(stringResource(R.string.settings_preview_sample), fontSize = labelSizeSp.sp, fontFamily = family)
                }
            }
        }
    }
}
