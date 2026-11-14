// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.kit.orion

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.nikelauncher.R
import dev.nikelauncher.ui.kit.motion.AppMotion

/**
 * A settings page that opens on an [OrionPageHero]. The hero scrolls away with the page; once it has, a small Material
 * top bar fades its surface and the page title in, so back and the title stay in reach. [hero] is the page's first item,
 * runs edge to edge and is told how far down the bar reaches; [content] follows inside the usual 16dp gutter.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrionPageScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    hero: @Composable (topInset: Dp) -> Unit,
    content: LazyListScope.() -> Unit,
) {
    val list = rememberLazyListState()
    val scrolled by remember { derivedStateOf { list.firstVisibleItemIndex > 0 } }
    val bar by animateColorAsState(if (scrolled) MaterialTheme.colorScheme.surfaceContainer else Color.Transparent, AppMotion.color(), label = "bar")
    val titleAlpha by animateFloatAsState(if (scrolled) 1f else 0f, AppMotion.effects(), label = "title")
    val status = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val nav = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Box(modifier.fillMaxSize().background(containerColor)) {
        LazyColumn(
            Modifier.fillMaxSize(),
            state = list,
            contentPadding = PaddingValues(bottom = nav + 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "hero", contentType = "hero") { hero(status + TopAppBarDefaults.TopAppBarExpandedHeight) }
            content()
        }
        TopAppBar(
            title = { Text(title, Modifier.graphicsLayer { alpha = titleAlpha }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = bar, scrolledContainerColor = bar),
        )
    }
}
