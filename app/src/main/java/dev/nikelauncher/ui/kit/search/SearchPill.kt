// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.kit.search

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import dev.nikelauncher.R
import dev.nikelauncher.ui.kit.motion.AppMotion

/**
 * The app list's search box, drawn against the wallpaper rather than a surface.
 *
 * It comes in with the list instead of being there at the first frame, the outline and a faint
 * fill brighten while it has focus, the magnifier turns into a way back once typing starts, and
 * the clear button grows in with the first character. All of it on the theme's motion scheme.
 */
@Composable
fun SearchPill(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    contentColor: Color,
    onGo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager: FocusManager = LocalFocusManager.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val colorSpec = AppMotion.color()
    val border by animateColorAsState(contentColor.copy(alpha = if (focused) 0.85f else 0.3f), colorSpec, label = "searchBorder")
    val fill by animateColorAsState(contentColor.copy(alpha = if (focused) 0.1f else 0f), colorSpec, label = "searchFill")
    val engaged = focused || query.isNotEmpty()
    val appear = remember { MutableTransitionState(false).apply { targetState = true } }
    val spatial = AppMotion.size()
    val offset = AppMotion.offset()
    val effects = AppMotion.effects<Float>()
    val scale = AppMotion.fastSpatial<Float>()

    AnimatedVisibility(
        visibleState = appear,
        enter = fadeIn(effects) + slideInVertically(offset) { -it / 2 } + expandVertically(spatial),
        exit = fadeOut(effects) + shrinkVertically(spatial),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            interactionSource = interaction,
            // The key is already there and already means this; asking is the long way round to the
            // one app you have just finished spelling. Nothing happens when nothing matched.
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { onGo() }),
            placeholder = { Text(placeholder, color = contentColor.copy(alpha = 0.6f)) },
            leadingIcon = {
                AnimatedContent(
                    targetState = engaged,
                    transitionSpec = { (fadeIn(effects) + scaleIn(scale, 0.6f)) togetherWith (fadeOut(effects) + scaleOut(scale, 0.6f)) },
                    label = "searchLeading",
                ) { typing ->
                    if (typing) {
                        IconButton(onClick = { onQueryChange(""); focusManager.clearFocus() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back), tint = contentColor.copy(alpha = 0.85f))
                        }
                    } else {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = contentColor.copy(alpha = 0.7f))
                    }
                }
            },
            trailingIcon = {
                AnimatedVisibility(query.isNotEmpty(), enter = fadeIn(effects) + scaleIn(scale, 0.6f), exit = fadeOut(effects) + scaleOut(scale, 0.6f)) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Filled.Close, stringResource(R.string.icon_picker_clear_search), tint = contentColor.copy(alpha = 0.7f))
                    }
                }
            },
            shape = MaterialTheme.shapes.extraLarge,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = contentColor,
                unfocusedTextColor = contentColor,
                cursorColor = contentColor,
                focusedBorderColor = border,
                unfocusedBorderColor = border,
                focusedContainerColor = fill,
                unfocusedContainerColor = fill,
            ),
            modifier = modifier,
        )
    }
}
