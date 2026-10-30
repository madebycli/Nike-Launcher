// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.applist

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import dev.nikelauncher.R

@Composable
internal fun rememberAppListSession(parameters: AppListParameters): AppListSession {
    val latest = rememberUpdatedState(parameters)
    val scope = rememberCoroutineScope()
    val session = remember { AppListSession(latest, scope) }
    with(session) {
        density = LocalDensity.current
        focusManager = LocalFocusManager.current
        workSuffix = stringResource(R.string.app_work_suffix)

        displayModel =
            remember(model, searchModel, query, searchEnabled) {
                if (!searching) {
                    model
                } else {
                    val term = query.trim()
                    searchModel
                        .filtered { app ->
                            displayName(app).contains(term, ignoreCase = true) ||
                                app.componentName.packageName.contains(term, ignoreCase = true)
                        }
                        .rankedBy {
                            searchRank(displayName(it), it.componentName.packageName, term)
                        }
                }
            }
        topFadeState =
            animateFloatAsState(
                if (listState.canScrollBackward) 1f else 0f,
                label = "topFade",
            )
        bottomFadeState =
            animateFloatAsState(
                if (listState.canScrollForward) 1f else 0f,
                label = "bottomFade",
            )

        LaunchedEffect(query) {
            if (query.isNotBlank()) listState.scrollToItem(0)
        }

        othersAlphaState =
            animateFloatAsState(
                targetValue =
                    if (scrubLetter != null && scrubbing && dimOthers && !scrub.holdScrolling) {
                        0f
                    } else {
                        1f
                    },
                animationSpec = tween(durationMillis = 180),
                label = "othersAlpha",
            )

        LaunchedEffect(searchFocusTick, searchEnabled, visible) {
            if (visible && searchFocusTick > 0 && searchEnabled) {
                withFrameNanos {}
                runCatching { searchFocus.requestFocus() }
            }
        }
        LaunchedEffect(visible, autoKeyboard, searchEnabled) {
            if (visible && autoKeyboard && searchEnabled) {
                withFrameNanos {}
                runCatching { searchFocus.requestFocus() }
            }
        }
        LaunchedEffect(visible) {
            if (!visible) {
                focusManager.clearFocus()
                highlightRange = IntRange.EMPTY
                overPull = 0f
                stretchPx = 0f
                collapsing = false
                scrub.clearPlacement()
                return@LaunchedEffect
            }
            if (scrub.placement == null) listState.scrollToItem(0)
        }

        folderById =
            remember(displayModel) {
                displayModel.rows.filterIsInstance<AppListRow.FolderRow>().associate {
                    it.folder.id to it.folder
                }
            }

        ObservePlacement()
        listConnection =
            remember(
                dismissPullPx,
                maxPullPx,
                maxStretchPx,
                fastDragPx,
                nearEndPx,
                viewportHeightPx,
                listState,
            ) {
                appListConnection(session)
            }
    }
    return session
}
