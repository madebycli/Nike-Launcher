// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Rocket
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.nikelauncher.R
import dev.nikelauncher.ui.kit.onboarding.IconBlob
import dev.nikelauncher.ui.kit.onboarding.SetupHeadline

@Composable
internal fun WelcomeStep(onRestore: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Hero(Icons.Filled.Rocket, R.string.app_name, headlineSize = true)
        TextButton(onClick = onRestore, modifier = Modifier.align(Alignment.BottomCenter)) {
            Text(stringResource(R.string.setup_restore))
        }
    }
}

@Composable
internal fun DoneStep() = Hero(Icons.Filled.CheckCircle, R.string.setup_ready, headlineSize = false)

@Composable
private fun Hero(icon: ImageVector, title: Int, headlineSize: Boolean) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // The drawing takes what the text leaves, so the greeting is always on screen, however
        // small the display or large the type.
        val drawingHeight = (maxHeight - 190.dp).coerceIn(0.dp, 400.dp)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (drawingHeight >= 120.dp) {
                SetupIllustration(settled = !headlineSize, modifier = Modifier.height(drawingHeight))
            } else {
                IconBlob(icon, size = 112.dp, iconSize = 56.dp)
            }
            Spacer(Modifier.height(24.dp))
            Text(
                stringResource(title),
                style = if (headlineSize) MaterialTheme.typography.displayMedium else MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** A page with its headline over a scrolling column. */
@Composable
internal fun SetupPage(title: Int, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SetupHeadline(stringResource(title), Modifier.padding(top = 20.dp))
        content()
    }
}
