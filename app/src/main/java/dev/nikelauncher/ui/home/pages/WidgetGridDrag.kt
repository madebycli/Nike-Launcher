// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.pages

import dev.nikelauncher.data.GridWidget

/** Apply an optimistic order without retaining stale widget sizes or removed host IDs. */
internal fun orderedGridWidgets(widgets: List<GridWidget>, order: List<Int>?): List<GridWidget> {
    if (order == null) return widgets
    val remaining = widgets.associateByTo(linkedMapOf()) { it.id }
    return buildList {
        order.forEach { id -> remaining.remove(id)?.let(::add) }
        addAll(remaining.values)
    }
}

internal data class GridDragItem(
    val id: Int, val index: Int,
    val left: Float, val top: Float, val width: Float, val height: Float,
) {
    val centerX get() = left + width / 2
    val centerY get() = top + height / 2
    fun contains(x: Float, y: Float) = x >= left && x < left + width && y >= top && y < top + height

    fun hitsResizeHandle(x: Float, y: Float, touchSize: Float): Boolean {
        val half = touchSize / 2
        return contains(x, y) && (
            (kotlin.math.abs(x - centerX) <= half && (y < top + touchSize || y >= top + height - touchSize)) ||
                (kotlin.math.abs(y - centerY) <= half && (x < left + touchSize || x >= left + width - touchSize))
            )
    }
}

/** Cross a neighbour's midpoint before inserting. Reflow alone must not undo a move. */
internal fun gridDragTarget(items: List<GridDragItem>, draggedId: Int, x: Float, y: Float): Int? {
    val source = items.firstOrNull { it.id == draggedId } ?: return null
    return items.firstOrNull { target ->
        if (target.id == draggedId || !target.contains(x, y)) false
        else if (target.top == source.top) {
            // Physical geometry also works when half-width items are laid out in RTL.
            if (target.centerX > source.centerX) x >= target.centerX else x <= target.centerX
        } else if (target.index > source.index) y >= target.centerY else y <= target.centerY
    }?.index
}

/** Pixels/second, proportional to penetration into either viewport edge. */
internal fun widgetDragScrollSpeed(y: Float, top: Float, bottom: Float, edge: Float, maxSpeed: Float): Float {
    if (bottom <= top || edge <= 0f || !y.isFinite()) return 0f
    val zone = edge.coerceAtMost((bottom - top) / 2)
    return when {
        y < top + zone -> -maxSpeed * ((top + zone - y) / zone).coerceIn(0f, 1f)
        y > bottom - zone -> maxSpeed * ((y - bottom + zone) / zone).coerceIn(0f, 1f)
        else -> 0f
    }
}
