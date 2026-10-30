// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.pages

import android.graphics.Rect
import android.view.View
import androidx.core.view.ViewCompat
import dev.nikelauncher.data.WidgetPageSide
import kotlin.math.roundToInt

/** One writer for the root View: home A–Z strips and widget grips must not overwrite each other. */
internal class PageGestureExclusions(private val view: View) {
    private var homeRects: List<Rect> = emptyList()
    private var widgetRects: List<Rect> = emptyList()
    private var widgetOwner: WidgetPageSide? = null
    private var widgetPageVisible = false
    private var applied: List<Rect> = emptyList()

    fun setHomeRects(rects: List<Rect>) { homeRects = rects; apply() }
    fun setWidgetPageVisible(visible: Boolean) { widgetPageVisible = visible; apply() }

    fun setWidgetBounds(owner: WidgetPageSide, left: Float, top: Float, width: Int, height: Int) {
        val location = IntArray(2)
        view.getLocationInWindow(location)
        val centerY = top - location[1] + height / 2f
        val density = view.resources.displayMetrics.density
        val touchSize = (48 * density).roundToInt()
        val stripWidth = (64 * density).roundToInt().coerceAtMost(view.width)
        // The grips are fully inside the widget. Exclude only their 48dp vertical band,
        // well below Android's 200dp-per-edge limit. Back remains usable elsewhere.
        val band = resizeGripExclusionBand(centerY, view.height, touchSize)
        widgetOwner = owner
        widgetRects = if (band != null && width > 0 && stripWidth > 0 && left.isFinite()) listOf(
            Rect(0, band.top, stripWidth, band.bottom),
            Rect(view.width - stripWidth, band.top, view.width, band.bottom),
        ) else emptyList()
        apply()
    }

    fun clearWidgetBounds(owner: WidgetPageSide) {
        if (widgetOwner == owner) {
            widgetOwner = null
            widgetRects = emptyList()
            apply()
        }
    }

    fun dispose() {
        homeRects = emptyList(); widgetRects = emptyList()
        ViewCompat.setSystemGestureExclusionRects(view, emptyList())
        applied = emptyList()
    }

    private fun apply() {
        val rects = if (widgetPageVisible) widgetRects else homeRects
        if (applied != rects) {
            ViewCompat.setSystemGestureExclusionRects(view, rects)
            applied = rects
        }
    }
}
