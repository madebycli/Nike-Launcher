// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.kit.menu

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

/**
 * A [Menu] that opens at the finger rather than beneath whatever it happens to sit in.
 *
 * A menu popup anchors to the bounds of its parent layout node, not to a point, so on something
 * tall, like a 300 dp widget, it would land most of a screen below the press. Giving it a
 * zero-size parent at the touch point makes those bounds the fingertip: the menu opens against
 * the finger and flips around it when there is no room below.
 */
@Composable
fun TouchMenu(
    expanded: Boolean,
    offset: DpOffset,
    onDismissRequest: () -> Unit,
    content: @Composable MenuScope.() -> Unit,
) {
    Box(Modifier.offset(x = offset.x, y = offset.y).size(0.dp)) {
        Menu(expanded = expanded, onDismissRequest = onDismissRequest, content = content)
    }
}
