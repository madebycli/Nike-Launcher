// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import dev.nikelauncher.ui.home.*
import dev.nikelauncher.ui.theme.NikeTheme
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Exercises the real draggable, cancellation and animation clock; no screenshot-only assertions. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en-w411dp-h923dp-420dpi")
class HomeScrollInteractionTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private lateinit var scroll: HomeScroll
    private var useService = false
    private var shade = 0
    private var upEnds = 0
    private var upDrags = 0
    private fun content(up: Boolean = false) {
        rule.setContent { NikeTheme {
            scroll = rememberHomeScroll(GestureActions({ _, _ -> upDrags++ }, { upEnds++ }, {}, {}, { shade++; if (useService) Assert.assertTrue(dev.nikelauncher.service.SystemUi.expandNotificationShade(rule.activity)) }), up, 0)
            Box(Modifier.fillMaxSize().onSizeChanged { scroll.viewportHeight = it.height }.then(scroll.dragModifier(true))) {
                Box(Modifier.fillMaxWidth().height(100.dp).onSizeChanged { scroll.contentHeight = it.height })
            }
        } }
        rule.waitForIdle(); rule.mainClock.autoAdvance = false
    }
    private fun settled() {
        rule.mainClock.advanceTimeBy(8000); rule.waitForIdle()
        rule.runOnIdle { Assert.assertEquals(0f, scroll.offsetY, .01f) }
    }
    @Test fun shortHomeReboundsAfterAnUpwardDragAndAfterCancellation() {
        content()
        rule.onRoot().performTouchInput { swipe(Offset(centerX, height * .7f), Offset(centerX, height * .4f), 500) }
        settled()
        rule.onRoot().performTouchInput { down(center); moveBy(Offset(0f, 300f)); cancel() }
        settled()
        Assert.assertEquals(0, shade)
    }
    @Test fun interruptedReboundStillReturnsHomeAndDeepPullRequestsTheShadeOnce() {
        val controller = org.robolectric.Robolectric.buildService(dev.nikelauncher.service.NikeAccessibilityService::class.java).create()
        val service = controller.get()
        org.robolectric.util.ReflectionHelpers.callInstanceMethod<Void>(service, "onServiceConnected")
        useService = true
        try {
        content()
        rule.onRoot().performTouchInput { swipe(center, Offset(centerX, centerY + 500f), 500) }
        rule.mainClock.advanceTimeBy(32)
        rule.onRoot().performTouchInput { swipe(center, Offset(centerX, centerY - 180f), 300) }
        settled()
        Assert.assertEquals(1, shade)
        Assert.assertEquals(listOf(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS),
            org.robolectric.Shadows.shadowOf(service).globalActionsPerformed)
        } finally { controller.destroy() }
    }
    @Test fun enabledSwipeUpFinishesItsGestureAndTheHomeRebounds() {
        content(up = true)
        rule.onRoot().performTouchInput { swipe(center, Offset(centerX, centerY - 450f), 500) }
        settled(); Assert.assertTrue(upDrags > 0); Assert.assertEquals(1, upEnds)
    }
}
