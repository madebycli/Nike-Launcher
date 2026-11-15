// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.data

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt
import kotlin.math.ceil
import kotlin.math.abs

const val MAX_GRID_WIDGET_ROWS = 24

const val DEFAULT_WIDGET_PAGE_DIM_ALPHA = 0.35f

enum class WidgetPageSide { LEFT, RIGHT }

/** A two-column grid. Heights use 88dp rows separated by the grid's 12dp gap. */
data class GridWidget(
    val id: Int,
    val columns: Int = 2,
    val rows: Int = 2,
    /** Exact outer height from configuration, adjusted relative to itself by resize handles. */
    val configuredHeightDp: Int? = null,
) {
    val heightDp: Int get() = configuredHeightDp ?: rows * 100 - 12
}

data class WidgetPagesConfig(
    val leftEnabled: Boolean = false,
    val rightEnabled: Boolean = false,
    val dimWallpaper: Boolean = false,
    val dimAlpha: Float = DEFAULT_WIDGET_PAGE_DIM_ALPHA,
) {
    fun enabled(side: WidgetPageSide) = if (side == WidgetPageSide.LEFT) leftEnabled else rightEnabled

    /** An absent setting is transparent, even when the home screen or app list is dimmed. */
    val wallpaperDimAlpha: Float
        get() = if (dimWallpaper) boundedWidgetPageDim(dimAlpha) else 0f
}

fun boundedWidgetPageDim(alpha: Float): Float =
    if (alpha.isFinite()) alpha.coerceIn(0f, 0.85f) else DEFAULT_WIDGET_PAGE_DIM_ALPHA

/** Resize in grid-cell units; preserve identity and clamp before persisting a drag. */
fun resizeGridWidget(widget: GridWidget, columnsDelta: Float = 0f, rowsDelta: Float = 0f): GridWidget {
    // A configured height may sit between rows. Keep dragging relative to that real height
    // instead of jumping to the next grid boundary on the first pixel of movement.
    val configuredHeight = widget.configuredHeightDp?.let { height ->
        if (rowsDelta.isFinite()) {
            (height + rowsDelta.coerceIn(-MAX_GRID_WIDGET_ROWS.toFloat(), MAX_GRID_WIDGET_ROWS.toFloat()) * 100f)
                .roundToInt().coerceIn(40, MAX_GRID_WIDGET_ROWS * 100 - 12)
        } else height
    }
    val rows = configuredHeight?.let { ceil((it + 12) / 100f).toInt().coerceIn(1, MAX_GRID_WIDGET_ROWS) }
        ?: if (rowsDelta.isFinite()) (widget.rows + rowsDelta.coerceIn(-MAX_GRID_WIDGET_ROWS.toFloat(), MAX_GRID_WIDGET_ROWS.toFloat())).roundToInt().coerceIn(1, MAX_GRID_WIDGET_ROWS) else widget.rows
    return widget.copy(
        configuredHeightDp = configuredHeight,
        columns = if (columnsDelta.isFinite()) (widget.columns + columnsDelta.coerceIn(-2f, 2f)).roundToInt().coerceIn(1, 2) else widget.columns,
        rows = rows,
    )
}

fun gridWidgetsToJson(widgets: List<GridWidget>): String = JSONArray().apply {
    widgets.forEach { widget ->
        put(JSONObject().put("id", widget.id).put("columns", widget.columns).put("rows", widget.rows).apply {
            widget.configuredHeightDp?.let { put("heightDp", it.coerceIn(40, MAX_GRID_WIDGET_ROWS * 100 - 12)) }
        })
    }
}.toString()

/** Tolerate an interrupted/old import without duplicate host views or unbounded sizes. */
fun gridWidgetsFromJson(raw: String?): List<GridWidget> = runCatching {
    val array = JSONArray(raw ?: "[]")
    buildList {
        val seen = mutableSetOf<Int>()
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val id = item.optInt("id", -1)
            if (id <= 0 || !seen.add(id)) continue
            add(GridWidget(
                id, item.optInt("columns", 2).coerceIn(1, 2), item.optInt("rows", 2).coerceIn(1, MAX_GRID_WIDGET_ROWS),
                item.optInt("heightDp", 0).takeIf { it > 0 }?.coerceIn(40, MAX_GRID_WIDGET_ROWS * 100 - 12),
            ))
        }
    }
}.getOrDefault(emptyList())

/** Insertion rather than swapping keeps the other widgets in their existing order. */
fun moveGridWidget(widgets: List<GridWidget>, id: Int, targetIndex: Int): List<GridWidget> {
    val from = widgets.indexOfFirst { it.id == id }
    if (from == -1) return widgets
    return widgets.toMutableList().apply { add(targetIndex.coerceIn(0, lastIndex), removeAt(from)) }
}

/** Apply an explicit provider size hint, without quantising its height to whole rows. */
fun configuredGridWidget(
    widget: GridWidget,
    widthDp: Int?,
    heightDp: Int?,
    availableWidthDp: Int,
): GridWidget {
    if (heightDp == null || heightDp <= 0) return widget
    val height = heightDp.coerceIn(40, MAX_GRID_WIDGET_ROWS * 100 - 12)
    val halfWidth = ((availableWidthDp.coerceAtLeast(1) - 12) / 2f).coerceAtLeast(1f)
    return widget.copy(
        columns = widthDp?.takeIf { it > 0 }?.let { if (it <= halfWidth + 1f) 1 else 2 } ?: widget.columns,
        rows = ceil((height + 12) / 100f).toInt().coerceIn(1, MAX_GRID_WIDGET_ROWS),
        configuredHeightDp = height,
    )
}

/** Size lists describe several responsive variants, not a request to fill the screen. */
internal fun responsiveWidgetSize(
    sizes: List<Pair<Float, Float>>,
    referenceWidthDp: Int,
): Pair<Int, Int>? = sizes
    .filter { (width, height) -> width.isFinite() && height.isFinite() && width > 0 && height > 0 }
    .minByOrNull { (width, _) -> abs(width - referenceWidthDp) }
    ?.let { (width, height) -> width.roundToInt() to height.roundToInt() }
