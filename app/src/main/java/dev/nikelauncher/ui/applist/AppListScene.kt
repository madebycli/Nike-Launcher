// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.applist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nikelauncher.data.EdgeSide
import dev.nikelauncher.data.ScrubBubbleShape
import dev.nikelauncher.ui.common.EditAppDialog
import dev.nikelauncher.ui.theme.launcherTextShadow
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
internal fun AppListSession.Content() {
    CompositionLocalProvider(
        LocalTextStyle provides
            LocalTextStyle.current.copy(
                shadow = launcherTextShadow(textShadow, contentColor, textShadowStrength)
            )
    ) {
        Box(
            modifier =
                Modifier.fillMaxSize().nestedScroll(listConnection).pointerInput(listState) {
                    awaitEachGesture {
                        val down =
                            awaitFirstDown(
                                requireUnconsumed = false,
                                pass = PointerEventPass.Initial,
                            )
                        val start = down.position
                        var claimed = false
                        var moved = false
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Final)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (change.isConsumed) claimed = true
                            val delta = change.position - start
                            if (delta.getDistance() > viewConfiguration.touchSlop) moved = true

                            if (!change.pressed) break
                        }

                        val listOffset = if (searchEnabled && !searchAtBottom) searchHeightPx else 0
                        if (claimed || moved || isOnListContent(start.y - listOffset))
                            return@awaitEachGesture

                        currentDismiss()
                    }
                }
        ) {
            Box(
                modifier =
                    Modifier.fillMaxSize().graphicsLayer {
                        val pulled = collapseProvider()
                        val progress = (abs(pulled) / dismissPullPx).coerceIn(0f, 1f)
                        translationY = pulled * 0.6f
                        val scale = 1f - 0.12f * progress
                        scaleX = scale
                        scaleY = scale
                        alpha = 1f - 0.85f * progress
                    }
            ) {
                Column(
                    modifier =
                        Modifier.windowInsetsPadding(
                                WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)
                            )
                            .widthIn(max = MAX_LIST_WIDTH)
                            .align(
                                if (activeSide == EdgeSide.LEFT) Alignment.TopStart
                                else Alignment.TopEnd
                            )
                            .fillMaxSize()
                            .then(
                                if (statusBarHidden) {
                                    Modifier
                                } else {
                                    Modifier.windowInsetsPadding(
                                        WindowInsets.statusBarsIgnoringVisibility
                                    )
                                }
                            )
                            .imePadding()
                ) {
                    AppListSearchSlot(searchEnabled && !searchAtBottom, bottom = false) {
                        SearchField(
                            query = query,
                            onQueryChange = onQueryChange,
                            contentColor = contentColor,
                            edgeSide = edgeSide,
                            showAlphabet = showAlphabet,
                            stripInset = stripInset,
                            onGo = {
                                displayModel.rows
                                    .filterIsInstance<AppListRow.Entry>()
                                    .firstOrNull()
                                    ?.let { onLaunch(it.app) }
                            },
                            modifier =
                                Modifier.onSizeChanged { searchHeightPx = it.height }
                                    .focusRequester(searchFocus),
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
                            Rows()
                        }
                    }
                    AppListSearchSlot(searchEnabled && searchAtBottom, bottom = true) {
                        SearchField(
                            query = query,
                            onQueryChange = onQueryChange,
                            contentColor = contentColor,
                            edgeSide = edgeSide,
                            showAlphabet = showAlphabet,
                            stripInset = stripInset,
                            onGo = {
                                displayModel.rows
                                    .filterIsInstance<AppListRow.Entry>()
                                    .firstOrNull()
                                    ?.let { onLaunch(it.app) }
                            },
                            atBottom = true,
                            modifier =
                                Modifier.focusRequester(searchFocus)
                                    .smoothSearchImeJumps(enabled = true),
                        )
                    }
                }

                if (showAlphabet && !searching) {
                    EdgeScrubber(
                        letters = displayModel.letters,
                        scrubY = scrubY,
                        pullPx = pullPx,
                        band = band,
                        side = activeSide,
                        style = scrubStyle,
                        ripple = rippleParams,
                        modifier =
                            Modifier.align(
                                if (activeSide == EdgeSide.LEFT) Alignment.CenterStart
                                else Alignment.CenterEnd
                            ),
                    )
                }

                editDialogFor?.let { target ->
                    EditAppDialog(
                        currentName = displayName(target),
                        onConfirmName = { name ->
                            onSetName(target, name)
                            editDialogFor = null
                        },
                        onChangeIcon = { name ->
                            if (name.trim() != displayName(target)) onSetName(target, name.trim())
                            onChangeIcon(target)
                            editDialogFor = null
                        },
                        onDismiss = { editDialogFor = null },
                        app = target,
                    )
                }

                if (scrubLetter != null && scrubBubbleShape != ScrubBubbleShape.HIDDEN) {
                    val bubble = 72.dp
                    val halfPx = with(density) { (bubble / 2).toPx() }
                    val insetPx = with(density) { SCRUB_BUBBLE_INSET_DP.dp.toPx() }
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape =
                            when (scrubBubbleShape) {
                                ScrubBubbleShape.CIRCLE -> CircleShape
                                ScrubBubbleShape.SQUARE -> RectangleShape
                                else -> MaterialTheme.shapes.largeIncreased
                            },
                        modifier =
                            Modifier.align(
                                    if (activeSide == EdgeSide.LEFT) Alignment.TopStart
                                    else Alignment.TopEnd
                                )
                                .offset {
                                    val x = insetPx + pullPx()
                                    IntOffset(
                                        x =
                                            if (activeSide == EdgeSide.LEFT) x.roundToInt()
                                            else -x.roundToInt(),
                                        y = ((scrubY() ?: 0f) - halfPx).roundToInt(),
                                    )
                                }
                                .size(bubble),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                scrubLetter.toString(),
                                color = Color.White,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}
