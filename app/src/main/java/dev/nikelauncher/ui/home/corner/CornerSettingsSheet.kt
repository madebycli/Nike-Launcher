// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.corner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.FormatColorText
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.nikelauncher.NikeApp
import dev.nikelauncher.R
import dev.nikelauncher.data.CORNER_SIZE_RANGE
import dev.nikelauncher.data.CornerAction
import dev.nikelauncher.data.CornerColorMode
import dev.nikelauncher.data.CornerIcon
import dev.nikelauncher.data.CornerShape
import dev.nikelauncher.ui.kit.settings.ChoiceChips
import dev.nikelauncher.ui.kit.settings.ColorPickerSheet
import dev.nikelauncher.ui.kit.settings.SettingsGroup
import dev.nikelauncher.ui.kit.settings.SettingsPickerSheet
import dev.nikelauncher.ui.kit.settings.SettingsSlider
import dev.nikelauncher.ui.kit.settings.block
import dev.nikelauncher.ui.kit.settings.item
import dev.nikelauncher.ui.kit.settings.switchItem
import kotlinx.coroutines.launch

private enum class Chooser { TAP, SWIPE, BACKGROUND, FOREGROUND }

/** Everything about the button: whether it is there, what it does, and how it looks. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CornerSettingsSheet(
    appLabel: String?,
    overWallpaper: Color,
    onPickApp: () -> Unit,
    onDismiss: () -> Unit,
) {
    val prefs = (LocalContext.current.applicationContext as NikeApp).prefs.corner
    val settings by prefs.settings.collectAsState(null)
    val current = settings ?: return
    val scope = rememberCoroutineScope()
    var chooser by remember { mutableStateOf<Chooser?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val appearance = current.appearance

    val enabledTitle = stringResource(R.string.corner_enabled)
    val tapTitle = stringResource(R.string.corner_tap)
    val swipeTitle = stringResource(R.string.corner_swipe_up)
    val appTitle = stringResource(R.string.corner_action_app)
    val chooseApp = stringResource(R.string.widget_settings_choose_app)
    val sizeTitle = stringResource(R.string.corner_size)
    val opacityTitle = stringResource(R.string.corner_background_opacity)
    val backgroundTitle = stringResource(R.string.corner_background_color)
    val autoIconTitle = stringResource(R.string.corner_auto_icon_color)
    val iconColorTitle = stringResource(R.string.corner_icon_color)
    val badgeTitle = stringResource(R.string.corner_badge)
    val tapLabel = stringResource(current.tap.label)
    val swipeLabel = stringResource(current.swipe.label)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().heightIn(max = LocalConfiguration.current.screenHeightDp.dp * .85f)) {
            CornerSettingsPreview(current, overWallpaper)
            Column(
                Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp).padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
            SettingsGroup {
                switchItem(enabledTitle, current.enabled, { v -> scope.launch { prefs.setEnabled(v) } }, icon = Icons.Outlined.TouchApp)
                item(tapTitle, { chooser = Chooser.TAP }, icon = current.tap.icon, value = tapLabel)
                item(swipeTitle, { chooser = Chooser.SWIPE }, icon = current.swipe.icon, value = swipeLabel)
                item(appTitle, { scope.launch { sheetState.hide(); onPickApp() } }, icon = Icons.Outlined.Apps, value = appLabel ?: chooseApp)
            }
            ChoiceChips(
                options = CornerShape.entries,
                selected = current.shape,
                label = {
                    stringResource(
                        when (it) {
                            CornerShape.CIRCLE -> R.string.corner_shape_circle
                            CornerShape.SQUIRCLE -> R.string.corner_shape_squircle
                            CornerShape.OUTLINE -> R.string.corner_shape_outline
                            CornerShape.MINIMAL -> R.string.corner_shape_minimal
                        },
                    )
                },
                onSelect = { scope.launch { prefs.setShape(it) } },
            )
            SettingsGroup {
                block(sizeTitle, value = "${current.sizeDp} dp") {
                    SettingsSlider(current.sizeDp.toFloat(), CORNER_SIZE_RANGE.first.toFloat()..CORNER_SIZE_RANGE.last.toFloat(), { v -> scope.launch { prefs.setSize(v.toInt()) } })
                }
            }
            SettingsGroup {
                switchItem(badgeTitle, appearance.icon == CornerIcon.BADGE, { v -> scope.launch { prefs.setIcon(if (v) CornerIcon.BADGE else CornerIcon.ACTION) } })
            }
            ChoiceChips(
                options = CornerColorMode.entries,
                selected = appearance.colorMode,
                label = {
                    stringResource(
                        when (it) {
                            CornerColorMode.CLASSIC -> R.string.corner_color_classic
                            CornerColorMode.MATERIAL -> R.string.corner_color_material
                            CornerColorMode.CUSTOM -> R.string.corner_color_custom
                        },
                    )
                },
                onSelect = { scope.launch { prefs.setColorMode(it) } },
            )
            SettingsGroup {
                if (appearance.colorMode == CornerColorMode.CUSTOM) {
                    item(backgroundTitle, { chooser = Chooser.BACKGROUND }, icon = Icons.Outlined.Palette, value = "#%06X".format(appearance.background and 0xFFFFFF))
                    switchItem(autoIconTitle, appearance.automaticForeground, { v -> scope.launch { prefs.setAutomaticForeground(v) } }, icon = Icons.Outlined.Contrast)
                    if (!appearance.automaticForeground) {
                        item(iconColorTitle, { chooser = Chooser.FOREGROUND }, icon = Icons.Outlined.FormatColorText, value = "#%06X".format(appearance.foreground and 0xFFFFFF))
                    }
                }
                block(opacityTitle, value = "${appearance.backgroundOpacity}%") {
                    SettingsSlider(appearance.backgroundOpacity.toFloat(), 0f..100f, { v -> scope.launch { prefs.setBackgroundOpacity(v.toInt()) } }, step = 5f)
                }
            }
            TextButton(onClick = { scope.launch { prefs.resetColors() } }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.corner_reset_colors))
            }
            }
        }
    }

    when (chooser) {
        Chooser.TAP, Chooser.SWIPE -> {
            val tap = chooser == Chooser.TAP
            val labels = CornerAction.entries.associateWith { stringResource(it.label) }
            SettingsPickerSheet(
                title = stringResource(if (tap) R.string.corner_tap else R.string.corner_swipe_up),
                items = CornerAction.entries.toList(),
                selected = if (tap) current.tap else current.swipe,
                itemLabel = { labels.getValue(it) },
                onSelect = { action -> scope.launch { if (tap) prefs.setTap(action) else prefs.setSwipe(action) } },
                onDismiss = { chooser = null },
            )
        }
        Chooser.BACKGROUND, Chooser.FOREGROUND -> {
            val background = chooser == Chooser.BACKGROUND
            ColorPickerSheet(
                initial = Color(if (background) appearance.background else appearance.foreground),
                wallpaperColors = MaterialTheme.colorScheme.let { listOf(it.primary, it.secondary, it.tertiary, it.primaryContainer) },
                onPick = { color -> scope.launch { if (background) prefs.setBackground(color.toArgb()) else prefs.setForeground(color.toArgb()) } },
                onDismiss = { chooser = null },
            )
        }
        null -> Unit
    }
}
