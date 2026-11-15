// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.corner

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.ui.graphics.Shape
import dev.nikelauncher.data.CornerShape

/**
 * The outline a corner button is drawn in. The face and its press ripple both clip to it, so the
 * ripple never spills into the square around a round button.
 */
@Composable
internal fun CornerShape.outline(): Shape =
    if (this == CornerShape.SQUIRCLE) MaterialShapes.Square.toShape() else CircleShape
