// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.common

import androidx.compose.ui.graphics.Color
import org.junit.Assert.*
import org.junit.Test

class PreviewContrastTest {
    @Test fun previewUsesTheDarkMaterialSurfaceForWhiteTextInEitherTheme() {
        val light = Color(0xffeeeeee); val dark = Color(0xff161616)
        assertEquals(dark, previewPlate(Color.White, listOf(light, dark)))
        assertEquals(dark, previewPlate(Color.White, listOf(dark, light)))
        assertEquals(light, previewPlate(Color.Black, listOf(dark, light)))
    }
}
