// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.edit

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.nikelauncher.R

/** Touch target of an edit-mode control. */
internal val EditControlSize = 40.dp

/** End padding a row needs so its label cannot run under the grab handle. */
internal val DragHandleReserve = EditControlSize + 12.dp

/** The grab handle that reorders a row in edit mode. */
@Composable
internal fun DragHandle(modifier: Modifier, tint: Color) {
    Icon(
        Icons.Filled.DragHandle,
        contentDescription = stringResource(R.string.home_drag_handle),
        tint = tint.copy(alpha = 0.7f),
        modifier = modifier.padding(end = 12.dp).size(EditControlSize).padding(8.dp),
    )
}
