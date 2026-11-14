// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.nikelauncher.data.EdgeSide
import dev.nikelauncher.ui.kit.motion.AppMotion
import dev.nikelauncher.ui.kit.orion.OrionSharedScreen
import dev.nikelauncher.ui.settings.sections.aboutEntries
import dev.nikelauncher.ui.settings.sections.appListEntries
import dev.nikelauncher.ui.settings.sections.appearanceEntries
import dev.nikelauncher.ui.settings.sections.appsEntries
import dev.nikelauncher.ui.settings.sections.backgroundEntries
import dev.nikelauncher.ui.settings.sections.backupEntries
import dev.nikelauncher.ui.settings.sections.gesturesEntries
import dev.nikelauncher.ui.settings.sections.homeEntries
import dev.nikelauncher.ui.settings.sections.notificationEntries
import dev.nikelauncher.ui.settings.sections.searchButtonEntries
import dev.nikelauncher.ui.settings.sections.stripEntries
import dev.nikelauncher.ui.settings.sections.widgetPageEntries

/**
 * The settings: a tile per section, one section's page at a time, and a search box over all of them.
 *
 * The open section is saved, not just remembered: settings that change the window (the rotation
 * lock) rebuild the activity, and a plain remember would drop whoever was three rows into a section
 * back at the top menu.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SettingsScreen(env: SettingsEnv, onBack: () -> Unit, modifier: Modifier = Modifier) {
    var openName by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    val open = openName?.let { name -> SettingsSection.entries.firstOrNull { it.name == name } }
    BackHandler(enabled = open != null || query.isNotBlank()) {
        if (open != null) openName = null else query = ""
    }

    val entries =
        (appearanceEntries(env) +
                homeEntries(env) +
                widgetPageEntries(env) +
                appListEntries(env) +
                backgroundEntries(env) +
                searchButtonEntries(env) +
                stripEntries(env) +
                gesturesEntries(env) +
                notificationEntries(env) +
                appsEntries(env) +
                backupEntries(env) +
                aboutEntries(env))
            .filter { it.visible }

    val dimReveal = env.previews.dimReveal()
    val dimColor by env.prefs.appearance.dimColor.collectAsState(0xFF000000.toInt())
    val edgeSide by env.prefs.strip.edgeSide.collectAsState(EdgeSide.RIGHT)
    val edgeWidth by env.prefs.strip.edgeZoneWidthDp.collectAsState(56)
    val sectionTop by env.prefs.appList.sectionTopPercent.collectAsState(26)

    Box(modifier.fillMaxSize()) {
        DimPreview(env.previews, Color(dimColor), dimReveal)
        val container = MaterialTheme.colorScheme.surface.copy(alpha = 1f - dimReveal)
        val results = remember(entries, query) {
            if (query.isBlank()) emptyList() else entries.filter { it.matches(query) }.groupBy { it.section }.toList()
        }
        val effects = AppMotion.effects<Float>()
        SharedTransitionLayout {
            AnimatedContent(
                targetState = open,
                // The tile and its page share bounds (see orionSharedBounds); the rest only cross-fades.
                transitionSpec = { fadeIn(effects) togetherWith fadeOut(effects) },
                label = "settingsSection",
            ) { target ->
                OrionSharedScreen(this@SharedTransitionLayout, this@AnimatedContent) {
                    if (target == null) {
                        SettingsHub(query, { query = it }, results, { openName = it.name }, onBack, container)
                    } else {
                        val rows = remember(entries, target) { entries.filter { it.section == target } }
                        SettingsSectionPage(target, rows, { openName = null }, container)
                    }
                }
            }
        }
        PreviewOverlays(env.previews, edgeSide, edgeWidth, sectionTop)
    }
}
