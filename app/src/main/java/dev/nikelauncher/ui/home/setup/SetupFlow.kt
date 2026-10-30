// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.setup

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.nikelauncher.NikeApp
import dev.nikelauncher.R
import dev.nikelauncher.ui.apps.AppsState
import dev.nikelauncher.ui.kit.motion.AppMotion
import dev.nikelauncher.ui.kit.onboarding.SetupPrimaryButton
import dev.nikelauncher.ui.kit.onboarding.SetupStepHeader
import dev.nikelauncher.ui.settings.rememberBackupActions
import dev.nikelauncher.ui.state.LibraryState

private const val STEPS = 5
private const val LAST = STEPS - 1

/**
 * The first-run flow: greeting, what the launcher needs access to, how it looks, which apps are
 * favorites, done. A layer inside the launcher's own window rather than a destination or a dialog:
 * the HOME key unwinds the navigation stack and would dismiss a destination the moment it
 * appeared, and a dialog can be measured taller than an edge-to-edge window.
 */
@Composable
internal fun SetupFlow(
    app: NikeApp,
    apps: AppsState,
    library: LibraryState,
    iconSizeDp: Int,
    onDone: () -> Unit,
) {
    val backup = rememberBackupActions(app, apps.privateSpace)
    var step by rememberSaveable { mutableIntStateOf(0) }
    // Every access below is granted on a system screen, so the answer only arrives on resume.
    var resumed by remember { mutableIntStateOf(0) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) resumed++ }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    // Back walks the steps in reverse and is swallowed on the first, so leaving by accident cannot
    // drop anyone onto a half-configured launcher.
    BackHandler { if (step > 0) step-- }

    Surface(
        Modifier.fillMaxSize().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 24.dp)) {
            if (step > 0) SetupStepHeader(step, LAST, onBack = { step-- }, modifier = Modifier.padding(top = 12.dp))
            val offsetSpec = AppMotion.offset()
            val effectsSpec = AppMotion.effects()
            AnimatedContent(
                targetState = step,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                transitionSpec = {
                    val forward = targetState > initialState
                    (slideInHorizontally(offsetSpec) { if (forward) it / 3 else -it / 3 } + fadeIn(effectsSpec)) togetherWith
                        (slideOutHorizontally(offsetSpec) { if (forward) -it / 3 else it / 3 } + fadeOut(effectsSpec))
                },
                label = "setupStep",
            ) { page ->
                when (page) {
                    0 -> WelcomeStep(onRestore = backup.import)
                    1 -> AccessStep(resumed)
                    2 -> StyleStep(app)
                    3 -> FavoritesStep(app, apps, library, iconSizeDp)
                    else -> DoneStep()
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (step > 0) TextButton(onClick = { step-- }, modifier = Modifier.heightIn(min = 56.dp)) {
                    Text(stringResource(R.string.action_back))
                }
                SetupPrimaryButton(
                    text = stringResource(
                        when (step) {
                            0 -> R.string.setup_get_started
                            LAST -> R.string.action_done
                            else -> R.string.setup_next
                        },
                    ),
                    onClick = { if (step == LAST) onDone() else step++ },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
