// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.kit.sheet

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** Shared calendar/notification viewport policy, using the stock Material sheet and drag handle. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ViewportSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState,
    panelTag: String,
    fillHeight: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        sheetMaxWidth = 640.dp,
        dragHandle = null,
        contentWindowInsets = {
            WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
        },
    ) {
        val density = LocalDensity.current
        val safeTop =
            with(density) {
                maxOf(WindowInsets.statusBars.getTop(this), WindowInsets.displayCutout.getTop(this))
                    .toDp()
            }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            Column(
                Modifier.fillMaxWidth()
                    .then(if (fillHeight) Modifier.height((maxHeight - safeTop).coerceAtLeast(0.dp))
                        else Modifier.heightIn(max = (maxHeight - safeTop).coerceAtLeast(0.dp)))
                    .testTag(panelTag).semantics { testTagsAsResourceId = true }
            ) {
                Box(Modifier.fillMaxWidth().height(24.dp), contentAlignment = Alignment.Center) {
                    BottomSheetDefaults.DragHandle(Modifier.wrapContentHeight(unbounded = true))
                }
                content()
            }
        }
    }
}
