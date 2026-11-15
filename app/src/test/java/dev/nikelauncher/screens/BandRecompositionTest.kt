// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.screens

import androidx.compose.material3.Text
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.junit4.createComposeRule
import dev.nikelauncher.ui.applist.ScrubBand
import dev.nikelauncher.ui.home.route.BandEditing
import dev.nikelauncher.ui.home.route.ObserveScrubBand
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Counts snapshot-driven compositions; this is not a frame-time benchmark. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BandRecompositionTest {
    @get:Rule val rule = createComposeRule()
    @Before fun optIn() = Assume.assumeTrue(System.getenv("NIKE_PERF") != null)
    @Test fun baselineBandRead() {
        val bands = BandEditing({ null }, { 2400 }, { _, _ -> })
        var route = 0
        rule.setContent {
            val band = bands.band
            SideEffect { route++ }
            Text(band.topPx.toString())
        }
        repeat(120) { frame ->
            rule.runOnIdle { bands.favoritesBand = ScrubBand(frame.toFloat(), 800f) }
            rule.waitForIdle()
        }
        println("Baseline route compositions over 120 band updates: $route")
        Assert.assertEquals(121, route)
    }
    @Test fun isolatedBandRead() {
        val bands = BandEditing({ null }, { 2400 }, { _, _ -> })
        var route = 0
        var consumer = 0
        rule.setContent {
            SideEffect { route++ }
            ObserveScrubBand(bands) { band ->
                SideEffect { consumer++ }
                Text(band.topPx.toString())
            }
        }
        repeat(120) { frame ->
            rule.runOnIdle { bands.favoritesBand = ScrubBand(frame.toFloat(), 800f) }
            rule.waitForIdle()
        }
        println("Isolated route/consumer compositions over 120 band updates: $route/$consumer")
        Assert.assertEquals(1, route)
        Assert.assertEquals(121, consumer)
    }
    @Test fun hiddenConsumerDoesNotObserveTheBand() {
        val bands = BandEditing({ null }, { 2400 }, { _, _ -> })
        var consumer = 0
        rule.setContent { ObserveScrubBand(bands, active = false) { SideEffect { consumer++ } } }
        repeat(120) { frame ->
            rule.runOnIdle { bands.favoritesBand = ScrubBand(frame.toFloat(), 800f) }
            rule.waitForIdle()
        }
        Assert.assertEquals(1, consumer)
    }
}
