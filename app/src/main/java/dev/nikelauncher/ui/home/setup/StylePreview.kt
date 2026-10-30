// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import dev.nikelauncher.ui.common.LocalIconConfig
import dev.nikelauncher.ui.home.rows.AlignedIconLabel
import dev.nikelauncher.ui.home.rows.arrangement
import dev.nikelauncher.ui.home.rows.textAlign
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nikelauncher.NikeApp
import dev.nikelauncher.R
import dev.nikelauncher.data.AppFont
import dev.nikelauncher.data.AppInfo
import dev.nikelauncher.data.HomeAlignment
import dev.nikelauncher.data.IconSide
import dev.nikelauncher.ui.common.AppIcon
import dev.nikelauncher.ui.theme.fontFamilyOf

/** How many apps the preview shows: enough to see the gap between rows, few enough to stay small. */
private const val PREVIEW_ROWS = 3

/**
 * The favorites as they are being styled, drawn from the same preferences the home screen reads, so
 * what moves here is what will move there. It sits above the controls and stays put while they scroll.
 */
@Composable
internal fun StylePreview(app: NikeApp, apps: List<AppInfo>, favorites: List<AppInfo>, modifier: Modifier = Modifier) {
    val a = app.prefs.appearance
    val iconSize by a.iconSizeDp.collectAsState(56)
    val labelSize by a.labelSizeSp.collectAsState(16)
    val spacing by a.itemSpacingDp.collectAsState(10)
    val showIcons by a.showFavoriteIcons.collectAsState(true)
    val showLabels by a.showFavoriteLabels.collectAsState(true)
    val alignment by a.alignment.collectAsState(HomeAlignment.LEFT)
    val iconSide by a.iconSide.collectAsState(IconSide.LEFT)
    val font by a.font.collectAsState(AppFont.GOOGLE_SANS_FLEX)
    val fontFile by a.fontFile.collectAsState(null)
    val family = fontFamilyOf(font, fontFile)

    val shown = (favorites.ifEmpty { apps }).take(PREVIEW_ROWS)
    // A fixed viewport measures every row at its requested size; overflow scrolls inside the preview.
    Surface(modifier.fillMaxWidth().height(200.dp).testTag("setup-style-preview"), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(spacing.dp),
            horizontalAlignment = when (alignment) {
                HomeAlignment.LEFT -> Alignment.Start
                HomeAlignment.CENTER -> Alignment.CenterHorizontally
                HomeAlignment.RIGHT -> Alignment.End
            },
        ) {
            val iconConfig = LocalIconConfig.current
            CompositionLocalProvider(LocalIconConfig provides iconConfig.copy(showIcons = showIcons)) {
                shown.forEach { entry ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = alignment.arrangement()) {
                        AlignedIconLabel(alignment, iconSide, showLabels || !showIcons, iconSize.dp,
                            icon = { if (showIcons) AppIcon(entry, iconSize, Modifier.testTag("setup-preview-icon-${entry.key}")) },
                        ) { labelModifier ->
                            Text(entry.label, labelModifier, fontSize = labelSize.sp, fontFamily = family,
                                textAlign = alignment.textAlign(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            if (shown.isEmpty()) Text(stringResource(R.string.settings_preview_sample), fontSize = labelSize.sp, fontFamily = family)
        }
    }
}
