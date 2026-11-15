// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.widget

import android.graphics.Outline
import android.graphics.Rect
import android.view.View
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RoundedWidgetHostViewTest {
    @Test fun clipsToInsetProviderBackground() {
        val host = RoundedWidgetHostView(ApplicationProvider.getApplicationContext())
        val child = View(host.context).apply { id = android.R.id.background }
        host.addView(child)
        host.layout(0, 0, 400, 500)
        child.layout(20, 30, 380, 470)
        val outline = Outline()
        host.outlineProvider.getOutline(host, outline)
        val actual = Rect()
        assertTrue(host.clipToOutline)
        assertTrue(outline.getRect(actual))
        assertEquals(Rect(20, 30, 380, 470), actual)
        assertTrue(outline.radius > 0f)
    }
}
