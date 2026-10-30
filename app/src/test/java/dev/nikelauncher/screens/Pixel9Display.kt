// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.screens

import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowDisplay
import android.app.Activity

/** Pixel 9 at its default 420 dpi: 1080 x 2424 pixels, about 411 x 923 dp. */
internal fun pixel9Display(activity: Activity) {
    shadowOf(ShadowDisplay.getDefaultDisplay()).apply {
        setDensityDpi(420)
        setWidth(1080)
        setHeight(2424)
        setRealWidth(1080)
        setRealHeight(2424)
    }
    activity.resources.displayMetrics.apply {
        widthPixels = 1080
        heightPixels = 2424
    }
    activity.window.setLayout(1080, 2424)
}
