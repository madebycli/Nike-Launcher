// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import dev.nikelauncher.ui.theme.NikeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h924dp-xxhdpi")
class SmokeScreenTest {
    @get:Rule val rule = createComposeRule()

    @Test fun renders() {
        rule.setContent { NikeTheme { Column { Text("Nike Launcher", style = MaterialTheme.typography.displayMedium) } } }
        rule.shoot("smoke")
    }
}
