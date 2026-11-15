// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.screens

import android.graphics.BitmapFactory
import android.graphics.Color
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dev.nikelauncher.widget.RoundedWidgetHostView
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "en-w411dp-h923dp-420dpi")
class RoundedWidgetScreensTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    @Before fun optIn() { Assume.assumeTrue(System.getenv("NIKE_SCREENS") != null); pixel9Display(rule.activity) }
    @Test fun squareWidgetUsesSystemRadius() {
        rule.setContent { Fixture() }
        rule.shoot("square-widget-rounded")
        val image = BitmapFactory.decodeFile(java.io.File(SHOTS_DIR, "square-widget-rounded.png").path)
        Assert.assertEquals(Color.BLACK, image.getPixel(70, 925))
        Assert.assertEquals(Color.LTGRAY, image.getPixel(540, 1212))
    }
    @Composable private fun Fixture() {
        Box(Modifier.fillMaxSize().background(ComposeColor.Black), contentAlignment = Alignment.Center) {
            AndroidView(factory = { context ->
                RoundedWidgetHostView(context).apply {
                    addView(View(context).apply { id = android.R.id.background; setBackgroundColor(Color.LTGRAY) })
                }
            }, modifier = Modifier.width(360.dp).height(220.dp).clip(MaterialTheme.shapes.extraLarge))
        }
    }
}
