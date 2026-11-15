// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.widget

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout
import kotlin.math.abs

/** Native widgets keep their tap/scroll stream; only a stationary long press opens editing. */
class LongPressFrameLayout(context: Context) : FrameLayout(context) {
    var onLongPress: ((x: Float, y: Float) -> Unit)? = null
    private var longPressFired = false
    private var downX = 0f
    private var downY = 0f
    private var scrollable = false
    private var claimed = false
    private val slop = ViewConfiguration.get(context).scaledTouchSlop / 2f
    private val detector =
        GestureDetector(
            context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onLongPress(e: MotionEvent) {
                    longPressFired = true
                    parent?.requestDisallowInterceptTouchEvent(false)
                    onLongPress?.invoke(e.x, e.y)
                }
            },
        )

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            longPressFired = false
            claimed = false
            downX = event.x
            downY = event.y
            scrollable = scrollableAt(this, event.x, event.y)
        } else if (event.actionMasked == MotionEvent.ACTION_MOVE && scrollable && !claimed) {
            val dx = abs(event.x - downX)
            val dy = abs(event.y - downY)
            if (dy > slop && dy > dx * 1.15f) {
                claimVerticalTouch()
            }
        }
        detector.onTouchEvent(event)
        return try {
            super.dispatchTouchEvent(event)
        } finally {
            if (
                event.actionMasked == MotionEvent.ACTION_UP ||
                    event.actionMasked == MotionEvent.ACTION_CANCEL
            ) {
                parent?.requestDisallowInterceptTouchEvent(false)
                claimed = false
            }
        }
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean = longPressFired

    override fun onTouchEvent(event: MotionEvent): Boolean = true

    override fun onDetachedFromWindow() {
        parent?.requestDisallowInterceptTouchEvent(false)
        super.onDetachedFromWindow()
    }

    internal fun hasScrollableContentAt(x: Float, y: Float) = scrollableAt(this, x, y)

    internal fun claimVerticalTouch() {
        claimed = true
        parent?.requestDisallowInterceptTouchEvent(true)
    }

    override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        // A child may request ownership on DOWN (e.g. an active fling). Wait for the axis
        // so a horizontal swipe can still turn the widget page.
        if (disallowIntercept && scrollable && !claimed) return
        super.requestDisallowInterceptTouchEvent(disallowIntercept)
    }

    /** Hit-test the touched branch, rather than claiming a widget header for a distant list. */
    private fun scrollableAt(view: View, x: Float, y: Float): Boolean {
        if (
            view.visibility != View.VISIBLE || x < 0 || y < 0 || x >= view.width || y >= view.height
        )
            return false
        if (view.canScrollVertically(1) || view.canScrollVertically(-1)) return true
        if (view is ViewGroup)
            for (index in view.childCount - 1 downTo 0) {
                val child = view.getChildAt(index)
                if (
                    scrollableAt(child, x + view.scrollX - child.left, y + view.scrollY - child.top)
                )
                    return true
            }
        return false
    }
}
