// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.screens

import dev.nikelauncher.data.*
import dev.nikelauncher.ui.home.*
import dev.nikelauncher.widget.WidgetSlotActions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Real preference writes with inert app-launch/provider boundaries. */
internal fun fakeHomeActions(prefs: Prefs, scope: CoroutineScope) = HomeActions(
    apps = AppActions({}, {}, {}, { _, _ -> }, {}, {}, {}),
    folders = FolderActions({}, { _, _ -> }, { _, _ -> }, {}, {}, {}, {}, null),
    layout = LayoutActions(
        onReorderFavorites = {}, onCommitPin = { b, p -> scope.launch { prefs.home.setHomePin(b, p) } },
        onNudgeX = { b, dp -> scope.launch { prefs.home.nudgeBlockOffset(b, dp) } },
        onNudgeEdge = { b, dp -> scope.launch { prefs.homeLayout.setEdges(setOf(b), delta = dp) } },
        onResetOffset = { b -> scope.launch { prefs.home.setBlockOffset(b, 0) } },
        onResizeNowPlaying = { dp -> scope.launch { prefs.home.setNowPlayingHeightDp(dp) } },
        onFavoritesBoundsChanged = { _, _ -> }, onEditScrubBand = {},
        onCommitPins = { pins -> scope.launch { prefs.homeLayout.setPins(pins) } },
        onSetBlockGap = { dp -> scope.launch { prefs.homeLayout.setGap(dp) } },
        onSetRowSpacing = { dp -> scope.launch { prefs.appearance.setItemSpacingDp(dp) } },
        onSetEdges = { b, dp -> scope.launch { prefs.homeLayout.setEdges(b, dp) } },
    ),
    gestures = GestureActions({ _, _ -> }, {}, {}, {}, {}),
    widgets = WidgetSlotActions({}, {}, {}, {}, {}, {}), onOpenSettings = {}, onManageFavorites = {},
    corner = CornerActions(null, false, {}, {}, {}, {}, {}),
)
