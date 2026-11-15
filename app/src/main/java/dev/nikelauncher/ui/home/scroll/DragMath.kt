// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.scroll

/** Ordinary scrolling inside the content; resisted and bounded travel outside it. */
internal fun draggedOffset(position: Float, delta: Float, minimum: Float, elasticLimit: Float): Float {
    val next = position + delta
    val moved = when {
        next > 0f -> if (position > 0f) position + delta * .4f else next * .4f
        next < minimum -> if (position < minimum) position + delta * .4f else minimum + (next - minimum) * .4f
        else -> next
    }
    return moved.coerceIn(minimum - elasticLimit, elasticLimit)
}

/** An overscroll returns to its own boundary, regardless of release velocity. */
internal fun settleTarget(position: Float, minimum: Float, projected: Float): Float = when {
    position > 0f -> 0f
    position < minimum -> minimum
    else -> projected.coerceIn(minimum, 0f)
}
