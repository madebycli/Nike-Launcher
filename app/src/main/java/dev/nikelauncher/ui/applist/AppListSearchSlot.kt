// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.applist

import androidx.compose.animation.*
import androidx.compose.runtime.Composable
import dev.nikelauncher.ui.kit.motion.AppMotion

@Composable
internal fun AppListSearchSlot(visible: Boolean, bottom: Boolean, content: @Composable () -> Unit) {
    val spatial = AppMotion.offset()
    val effects = AppMotion.effects<Float>()
    AnimatedVisibility(
        visible,
        enter = slideInVertically(spatial) { if (bottom) it else -it } + fadeIn(effects),
        exit = slideOutVertically(spatial) { if (bottom) it else -it } + fadeOut(effects),
    ) {
        content()
    }
}
