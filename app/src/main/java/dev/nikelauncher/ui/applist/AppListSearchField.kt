// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.applist

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.nikelauncher.R
import dev.nikelauncher.data.EdgeSide
import dev.nikelauncher.ui.kit.search.SearchPill

@Composable
internal fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    contentColor: Color,
    /** The edges the strip may occupy, so the field keeps clear of the same ones the list does. */
    edgeSide: EdgeSide,
    showAlphabet: Boolean,
    stripInset: Dp,
    onGo: () -> Unit,
    atBottom: Boolean = false,
    modifier: Modifier = Modifier,
) {
    SearchPill(
        query = query,
        onQueryChange = onQueryChange,
        placeholder = stringResource(R.string.applist_search),
        contentColor = contentColor,
        onGo = onGo,
        modifier =
            modifier
                // The overlay draws under both system bars, so without this the field sits behind
                // the clock at the top, or the gesture pill at the bottom.
                .then(
                    if (atBottom) {
                        // The column's own ime padding has already cleared the keyboard, so
                        // adding the gesture bar on top of it would double the gap.
                        Modifier.windowInsetsPadding(
                            WindowInsets.navigationBars.exclude(WindowInsets.ime)
                        )
                    } else {
                        // The status bar is the column's problem now, so that the rows clear it
                        // too.
                        Modifier
                    }
                )
                .fillMaxWidth()
                .padding(
                    // Lines up with the rows' own inset instead of hugging the screen edge, and
                    // clears the A-Z strip on whichever side it occupies.
                    // Only what the strip actually occupies. The extra 20dp a side matched the
                    // rows, but a field is not a row: it left the box noticeably narrower than the
                    // names under it, which is what reads as the search bar being off to one side.
                    start = if (edgeSide != EdgeSide.RIGHT) stripInset else 8.dp,
                    end = if (edgeSide != EdgeSide.LEFT) stripInset else 8.dp,
                    top = if (atBottom) 8.dp else 12.dp,
                    bottom = if (atBottom) 12.dp else 8.dp,
                ),
    )
}
