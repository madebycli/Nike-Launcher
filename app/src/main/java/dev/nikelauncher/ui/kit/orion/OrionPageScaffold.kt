// SPDX-License-Identifier: GPL-3.0-or-later
/* Adapted from iris-android (GPL-3.0), presentation/settings/SettingsComponents.kt SettingsScreenScaffold, itself laid out
 * after Orion Store ui/Settings.kt Page (RookieEnough and contributors, GPL-3.0, e561e91).
 */
package dev.nikelauncher.ui.kit.orion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import dev.nikelauncher.R

/** The accent of the page on screen; section labels take their tone from it (Orion's LocalAccent). */
val LocalPageAccent = compositionLocalOf<Color?> { null }

/**
 * A settings page as Orion and iris lay it out: it opens on a tinted [OrionPageHero] with back above the icon, and
 * everything scrolls together, hero included. [content] follows inside a 16dp gutter.
 */
@Composable
fun OrionPageScaffold(
    title: String,
    desc: String,
    icon: ImageVector,
    accent: Color,
    shape: RoundedPolygon,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: LazyListScope.() -> Unit,
) {
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navigation = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    CompositionLocalProvider(LocalPageAccent provides accent) {
        LazyColumn(
            modifier.fillMaxSize().background(containerColor).imePadding(),
            contentPadding = PaddingValues(bottom = navigation + 40.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "hero", contentType = "hero") {
                OrionPageHero(icon, title, desc, accent, shape, background = containerColor) {
                    Spacer(Modifier.height(statusBar))
                    Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.action_back))
                        }
                    }
                }
            }
            content()
        }
    }
}
