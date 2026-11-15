// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.screens

import android.graphics.Bitmap
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.graphics.asAndroidBitmap
import java.io.File

/** Where the rendered screens land; read by whoever wants to look at them. */
internal val SHOTS_DIR: File = File(System.getProperty("nike.shots") ?: "build/screenshots").also { it.mkdirs() }

internal fun ComposeTestRule.shoot(name: String) {
    // Draw every layer for each capture. Reusing the embedded hardware renderer can omit
    // unchanged RenderNodes after a previous PixelCopy capture in Robolectric.
    System.setProperty("robolectric.pixelCopyRenderMode", "software")
    waitForIdle()
    val bitmap = onRoot().captureToImage().asAndroidBitmap()
    File(SHOTS_DIR, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
}
