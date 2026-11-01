// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.service

import android.app.Activity
import android.view.WindowManager
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@Suppress("DEPRECATION")
class StatusBarFaderTest {
    @After fun cleanup() = StatusBarFader.release()

    @Test fun instantHideSetsFullscreenPolicyAndShowClearsIt() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val window = activity.window
        StatusBarFader.setVisible(window, visible = false, instant = true)
        assertNotEquals(0, window.attributes.flags and WindowManager.LayoutParams.FLAG_FULLSCREEN)
        StatusBarFader.setVisible(window, visible = true, instant = true)
        assertEquals(0, window.attributes.flags and WindowManager.LayoutParams.FLAG_FULLSCREEN)
    }

    @Test fun repeatedHiddenRequestKeepsFullscreenWindowPolicy() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val window = activity.window
        StatusBarFader.setVisible(window, visible = false, instant = true)
        StatusBarFader.setVisible(window, visible = false, instant = true)
        assertNotEquals(0, window.attributes.flags and WindowManager.LayoutParams.FLAG_FULLSCREEN)
    }

    @Test fun animatedRequestReleasesInstantFullscreenPolicy() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val window = activity.window
        StatusBarFader.setVisible(window, visible = false, instant = true)
        StatusBarFader.setVisible(window, visible = true, instant = false)
        assertEquals(0, window.attributes.flags and WindowManager.LayoutParams.FLAG_FULLSCREEN)
    }
}
