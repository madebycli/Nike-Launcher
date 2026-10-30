// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import dev.nikelauncher.NikeApp
import dev.nikelauncher.data.WidgetPagesConfig
import dev.nikelauncher.ui.home.pages.PageGestureExclusions
import dev.nikelauncher.ui.home.pages.WidgetHomePages

/**
 * The home screen as the middle of up to three pages: widgets on either side, swiped between.
 * Owns the one writer of the system gesture exclusions that the side pages need.
 */
@Composable
internal fun HomePagesHost(
    app: NikeApp,
    homeIntentTick: Int,
    gesturesEnabled: Boolean,
    config: WidgetPagesConfig,
    onSidePageChanged: (Boolean) -> Unit,
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    val exclusions = remember(view) { PageGestureExclusions(view) }
    DisposableEffect(exclusions) { onDispose { exclusions.dispose() } }
    WidgetHomePages(
        app = app,
        homeIntentTick = homeIntentTick,
        gesturesEnabled = gesturesEnabled,
        config = config,
        gestureExclusions = exclusions,
        onSidePageChanged = { onSide ->
            exclusions.setWidgetPageVisible(onSide)
            onSidePageChanged(onSide)
        },
        home = { content() },
    )
}
