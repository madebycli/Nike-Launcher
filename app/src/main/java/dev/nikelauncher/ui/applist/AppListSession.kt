// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.applist

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Folder
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import dev.nikelauncher.data.AppInfo
import dev.nikelauncher.data.Folder
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first

/** Retained interaction state; parameters remain fresh through the snapshot holder. */
@Stable
internal class AppListSession(
    private val source: State<AppListParameters>,
    val scope: CoroutineScope,
) {
    val model
        get() = source.value.model

    val nameOverrides
        get() = source.value.nameOverrides

    val scrub
        get() = source.value.scrub

    val iconSizeDp
        get() = source.value.iconSizeDp

    val labelSizeSp
        get() = source.value.labelSizeSp

    val band
        get() = source.value.band

    val viewportHeightPx
        get() = source.value.viewportHeightPx

    val visible
        get() = source.value.visible

    val favoriteKeys
        get() = source.value.favoriteKeys

    val onLaunch
        get() = source.value.onLaunch

    val onSetFavorite
        get() = source.value.onSetFavorite

    val onSetName
        get() = source.value.onSetName

    val onChangeIcon
        get() = source.value.onChangeIcon

    val onAppInfo
        get() = source.value.onAppInfo

    val onUnpinShortcut
        get() = source.value.onUnpinShortcut

    val onHideApp
        get() = source.value.onHideApp

    val hiddenApps
        get() = source.value.hiddenApps

    val onMoveToFolder
        get() = source.value.onMoveToFolder

    val onToggleFolder
        get() = source.value.onToggleFolder

    val onManageFolder
        get() = source.value.onManageFolder

    val onDeleteFolder
        get() = source.value.onDeleteFolder

    val onSetFolderOnHome
        get() = source.value.onSetFolderOnHome

    val onRemoveFromFolder
        get() = source.value.onRemoveFromFolder

    val onOpenSettings
        get() = source.value.onOpenSettings

    val onDismiss
        get() = source.value.onDismiss

    val contentColor
        get() = source.value.contentColor

    val showAlphabet
        get() = source.value.showAlphabet

    val edgeSide
        get() = source.value.edgeSide

    val scrubStyle
        get() = source.value.scrubStyle

    val scrubBubbleShape
        get() = source.value.scrubBubbleShape

    val rippleParams
        get() = source.value.rippleParams

    val listState
        get() = source.value.listState

    val enterPullPx
        get() = source.value.enterPullPx

    val statusBarHidden
        get() = source.value.statusBarHidden

    val searchModel
        get() = source.value.searchModel

    val shortcutSwipe
        get() = source.value.shortcutSwipe

    val favoritesEditable
        get() = source.value.favoritesEditable

    val showSettingsRow
        get() = source.value.showSettingsRow

    val autoKeyboard
        get() = source.value.autoKeyboard

    val searchFocusTick
        get() = source.value.searchFocusTick

    val holdScrollSpeed
        get() = source.value.holdScrollSpeed

    val dimOthers
        get() = source.value.dimOthers

    val markWorkApps
        get() = source.value.markWorkApps

    val edgeZoneWidthDp
        get() = source.value.edgeZoneWidthDp

    val textShadow
        get() = source.value.textShadow

    val textShadowStrength
        get() = source.value.textShadowStrength

    val webSearchFallback
        get() = source.value.webSearchFallback

    val onWebSearch
        get() = source.value.onWebSearch

    val lastLetterToLine
        get() = source.value.lastLetterToLine

    val searchEnabled
        get() = source.value.searchEnabled

    val searchAtBottom
        get() = source.value.searchAtBottom

    val sectionTopPercent
        get() = source.value.sectionTopPercent

    val query
        get() = source.value.query

    val onQueryChange
        get() = source.value.onQueryChange

    val alignment
        get() = source.value.alignment

    val iconSide
        get() = source.value.iconSide

    lateinit var density: Density
    lateinit var focusManager: FocusManager
    lateinit var displayModel: AppListModel
    lateinit var listConnection: NestedScrollConnection
    var workSuffix = ""
    var folderById = emptyMap<String, Folder>()
    var topFadeState: State<Float> = mutableFloatStateOf(0f)
    var bottomFadeState: State<Float> = mutableFloatStateOf(0f)
    var othersAlphaState: State<Float> = mutableFloatStateOf(1f)
    val topFade
        get() = topFadeState.value

    val bottomFade
        get() = bottomFadeState.value

    val othersAlpha
        get() = othersAlphaState.value

    val currentDismiss
        get() = onDismiss

    val scrubLetter
        get() = scrub.letter

    val scrubPlacement
        get() = scrub.placement

    val scrubbing
        get() = scrub.scrubbing

    val activeSide
        get() = scrub.side

    val scrubY: () -> Float? = { scrub.currentY() }
    val pullPx: () -> Float = { scrub.currentPull() }
    val searching
        get() = searchEnabled && query.isNotBlank()

    val stripInset
        get() = maxOf(STRIP_INSET, edgeZoneWidthDp.dp)

    val sectionTopPx
        get() = (viewportHeightPx * sectionTopPercent / 100f).roundToInt()

    val dismissPullPx
        get() = with(density) { 150.dp.toPx() }

    val dismissCommitPx
        get() = dismissPullPx * COMMIT_FRACTION

    val fastDragPx
        get() = with(density) { 12.dp.toPx() }

    val nearEndPx
        get() = with(density) { 48.dp.toPx() }

    val maxPullPx
        get() = with(density) { 320.dp.toPx() }

    val maxStretchPx
        get() = with(density) { MAX_EDGE_STRETCH.toPx() }

    val restingTopPadding
        get() = if (searchEnabled && !searchAtBottom) SEARCH_EDGE_PADDING else IDLE_TOP_PADDING

    val restingBottomPadding
        get() = if (searchEnabled && searchAtBottom) SEARCH_EDGE_PADDING else IDLE_BOTTOM_PADDING

    val idleTopPaddingPx
        get() = with(density) { restingTopPadding.roundToPx() }

    val idleBottomPaddingPx
        get() = with(density) { restingBottomPadding.roundToPx() }

    val scrubRowIndex
        get() =
            scrubPlacement?.letter?.let { letter ->
                displayModel.letterIndex.firstOrNull { it.first == letter }?.second
            } ?: -1

    var searchHeightPx by mutableIntStateOf(0)
    var highlightRange by mutableStateOf(IntRange.EMPTY)
    var overPull by mutableFloatStateOf(0f)
    val collapseAnim = Animatable(0f)
    var collapsing by mutableStateOf(false)
    var stretchPx by mutableFloatStateOf(0f)
    val stretchAnim = Animatable(0f)
    var stretchSettling by mutableStateOf(false)
    val collapseProvider: () -> Float = {
        if (collapsing) collapseAnim.value else if (enterPullPx > 0f) enterPullPx else overPull
    }
    val stretchProvider: () -> Float = {
        (if (stretchSettling) stretchAnim.value else stretchPx).coerceIn(
            -maxStretchPx,
            maxStretchPx,
        )
    }
    val searchFocus = FocusRequester()
    var gapCeilingPx by mutableFloatStateOf(Float.MAX_VALUE)
    var blankCeilingPx by mutableFloatStateOf(Float.MAX_VALUE)
    var bottomRoomLive by mutableStateOf(false)
    var userDragged by mutableStateOf(false)
    var menuForKey by mutableStateOf<String?>(null)
    var menuOffset by mutableStateOf(DpOffset.Zero)
    var editDialogFor by mutableStateOf<AppInfo?>(null)
    val touchPosition = mutableStateOf(Offset.Zero)

    fun displayName(app: AppInfo): String {
        val base = nameOverrides[app.key] ?: app.label
        return if (markWorkApps && app.work) base + workSuffix else base
    }

    fun topGapRemaining(): Float {
        val info = listState.layoutInfo
        val first = info.visibleItemsInfo.firstOrNull() ?: return 0f
        if (first.index > 0) return 0f
        return (first.offset - info.viewportStartOffset).toFloat() - idleTopPaddingPx
    }

    fun forwardRoom(): Float {
        val info = listState.layoutInfo
        val last = info.visibleItemsInfo.lastOrNull() ?: return Float.MAX_VALUE
        if (last.index < displayModel.rows.size) return Float.MAX_VALUE
        val bottom = (last.offset - info.viewportStartOffset + last.size).toFloat()
        return bottom + idleBottomPaddingPx - viewportHeightPx
    }

    fun placementSettled(): Boolean {
        val items = listState.layoutInfo.visibleItemsInfo
        val first = items.firstOrNull() ?: return true
        if (first.index == 0 && items.last().index >= displayModel.rows.size) return true
        return topGapRemaining() <= 0f
    }

    fun isOnListContent(y: Float): Boolean {
        val info = listState.layoutInfo
        val first = info.visibleItemsInfo.firstOrNull() ?: return false
        val last = info.visibleItemsInfo.last()
        return y >= first.offset - info.viewportStartOffset &&
            y < last.offset + last.size - info.viewportStartOffset
    }
}
