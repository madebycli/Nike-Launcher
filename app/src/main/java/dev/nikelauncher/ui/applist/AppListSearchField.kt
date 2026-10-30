// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.applist

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.nikelauncher.R
import dev.nikelauncher.data.EdgeSide

/**
 * The app list's own search box: Material's search bar used as a plain field. The list under it
 * filters as the text changes and the keyboard's action key launches the first match.
 */
@Composable
internal fun AppListSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    /** The edges the strip may occupy, so the field keeps clear of the same ones the list does. */
    edgeSide: EdgeSide,
    stripInset: Dp,
    onGo: () -> Unit,
    atBottom: Boolean,
    modifier: Modifier = Modifier,
) {
    SearchBar(
        expanded = false,
        onExpandedChange = {},
        inputField = {
            SearchBarDefaults.InputField(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = { onGo() },
                expanded = false,
                onExpandedChange = {},
                placeholder = { Text(stringResource(R.string.applist_search)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.icon_picker_clear_search))
                        }
                    }
                },
                modifier = modifier,
            )
        },
        modifier = Modifier
            // The overlay draws under both system bars, so without this the field sits behind the
            // clock at the top or the gesture pill at the bottom. At the bottom the column's own ime
            // padding has already cleared the keyboard, so the gesture bar is not doubled on top of it.
            .then(if (atBottom) Modifier.windowInsetsPadding(WindowInsets.navigationBars.exclude(WindowInsets.ime)) else Modifier)
            .fillMaxWidth()
            // Clears the A-Z strip on whichever side it occupies, and otherwise lines up with the names.
            .padding(
                start = if (edgeSide != EdgeSide.RIGHT) stripInset else 8.dp,
                end = if (edgeSide != EdgeSide.LEFT) stripInset else 8.dp,
                top = if (atBottom) 8.dp else 12.dp,
                bottom = if (atBottom) 12.dp else 8.dp,
            ),
        content = {},
    )
}
