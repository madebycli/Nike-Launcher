// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.widget

import android.appwidget.AppWidgetHostView
import android.content.Context
import android.graphics.Outline
import android.graphics.Rect
import android.os.Build
import android.view.View
import android.view.ViewOutlineProvider

/** Apply Android's widget radius to the provider's actual background bounds. */
internal class RoundedWidgetHostView(context: Context) : AppWidgetHostView(context) {
    private val bounds = Rect()
    private val radius = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        resources.getDimension(android.R.dimen.system_app_widget_background_radius)
    } else 24f * resources.displayMetrics.density

    init {
        clipToOutline = true
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                val background = findViewById<View>(android.R.id.background)
                val content = background?.takeIf { it !== this@RoundedWidgetHostView && it.width > 0 && it.height > 0 }
                    ?: getChildAt(0)
                if (content != null && content.width > 0 && content.height > 0) {
                    content.getDrawingRect(bounds)
                    offsetDescendantRectToMyCoords(content, bounds)
                } else bounds.set(0, 0, width, height)
                outline.setRoundRect(bounds, radius.coerceAtMost(minOf(bounds.width(), bounds.height()) / 2f))
            }
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        invalidateOutline()
    }
}
