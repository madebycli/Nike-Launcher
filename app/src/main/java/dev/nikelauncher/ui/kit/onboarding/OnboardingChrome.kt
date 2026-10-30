// SPDX-License-Identifier: GPL-3.0-or-later
// Adapted from AniSync (GPL-3.0-or-later), app/src/main/java/com/anisync/android/presentation/onboarding/components/OnboardingChrome.kt.
// The segmented progress bar became Material's progress indicator, and the button keeps Material's own shape.
package dev.nikelauncher.ui.kit.onboarding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nikelauncher.R
import dev.nikelauncher.ui.kit.motion.AppMotion

/** The progress through the steps, with the way back when there is one. */
@Composable
fun SetupStepHeader(step: Int, total: Int, onBack: (() -> Unit)?, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back))
            }
            Spacer(Modifier.width(4.dp))
        }
        val fill by animateFloatAsState(step.toFloat() / total, AppMotion.spatial(), label = "setupProgress")
        LinearProgressIndicator(progress = { fill }, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Text(
            stringResource(R.string.setup_step_of, step, total),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun SetupHeadline(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.ExtraBold,
        lineHeight = 40.sp,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    )
}

/** The one full-width action at the bottom of a step. */
@Composable
fun SetupPrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(onClick = onClick, modifier = modifier.heightIn(min = 56.dp)) {
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

/** A glyph on a scalloped plate: a tinted shape at low opacity with the icon at full strength over it. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun IconBlob(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .matchParentSize()
                .clip(MaterialShapes.Cookie12Sided.toShape())
                .background(tint.copy(alpha = 0.22f)),
        )
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
    }
}
