// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.blocks

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.nikelauncher.R
import dev.nikelauncher.data.HomeAlignment
import dev.nikelauncher.ui.common.AppIcon
import dev.nikelauncher.ui.home.CornerActions

/**
 * A button in the corner nearest the thumb, for the one app reached often enough to deserve a
 * target rather than a gesture. Nothing is drawn without an app (or the search) chosen for it.
 */
@Composable
internal fun BoxScope.CornerButton(
    corner: CornerActions,
    alignment: HomeAlignment,
    iconSizeDp: Int,
    contentColor: Color,
) {
    if (corner.app == null && !corner.isSearch) return
    DisposableEffect(Unit) { onDispose { corner.onBounds(null) } }
    Surface(
        onClick = { if (corner.isSearch) corner.onOpenSearch() else corner.app?.let(corner.onOpenApp) },
        shape = CircleShape,
        color = Color.Transparent,
        modifier = Modifier
            .align(if (alignment == HomeAlignment.RIGHT) Alignment.BottomStart else Alignment.BottomEnd)
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 24.dp)
            // Reported so the swipe edge can stop short of it, or a tap on it opens the list instead.
            .onGloballyPositioned { corner.onBounds(it.boundsInWindow()) },
    ) {
        if (corner.isSearch) {
            Icon(
                Icons.Filled.Search,
                contentDescription = stringResource(R.string.applist_search),
                tint = contentColor,
                modifier = Modifier.padding(8.dp).size(iconSizeDp.dp),
            )
        } else {
            corner.app?.let { AppIcon(app = it, sizeDp = iconSizeDp, modifier = Modifier.padding(8.dp)) }
        }
    }
}
