// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.widget

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.Constraints
import dev.nikelauncher.ui.kit.motion.AppMotion

/** Where the clock's block sits in a slot, and how tall the slot has to be to keep it there. */
internal data class AnchoredClockPlacement(val topPx: Int, val neededPx: Int)

/**
 * The clock and date stay where a slot of [configuredPx] would centre them with nothing else in
 * it; whatever hangs below them — [dynamicPx] of agenda, alarm, notification count — takes the
 * room under that. It only gives way when the slot cannot be made taller and there is truly no
 * room, and then by exactly as much as it has to.
 *
 * [neededPx] is the slot height that holds everything without that happening.
 */
internal fun placeAnchoredClock(
    anchorPx: Int,
    dynamicPx: Int,
    slotPx: Int,
    configuredPx: Int,
): AnchoredClockPlacement {
    val configured = if (configuredPx > 0) configuredPx else slotPx
    val centred = ((configured - anchorPx) / 2).coerceAtLeast(0)
    val needed = centred + anchorPx + dynamicPx
    val top = if (needed <= slotPx) centred else (slotPx - anchorPx - dynamicPx).coerceAtLeast(0)
    return AnchoredClockPlacement(top, needed)
}

/**
 * Lays the clock block ([anchor]) and the rows that come and go below it ([dynamic]).
 *
 * The rows below change size smoothly; the clock above never moves for them.
 */
@Composable
internal fun AnchoredClockLayout(
    modifier: Modifier,
    configuredHeightPx: Int,
    onNeededHeightPx: ((Int) -> Unit)?,
    anchor: @Composable () -> Unit,
    dynamic: @Composable () -> Unit,
) {
    val reported = remember { intArrayOf(-1) }
    Layout(
        content = {
            Box(Modifier.layoutId("anchor")) { anchor() }
            Box(Modifier.layoutId("dynamic").animateContentSize(AppMotion.spatial())) { dynamic() }
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val loose = Constraints(
            minWidth = 0,
            maxWidth = constraints.maxWidth,
            minHeight = 0,
            maxHeight = Constraints.Infinity,
        )
        val anchorPlaceable = measurables.first { it.layoutId == "anchor" }.measure(loose)
        val dynamicPlaceable = measurables.first { it.layoutId == "dynamic" }.measure(loose)
        val slot = if (constraints.hasBoundedHeight) constraints.maxHeight
        else anchorPlaceable.height + dynamicPlaceable.height
        val placement = placeAnchoredClock(anchorPlaceable.height, dynamicPlaceable.height, slot, configuredHeightPx)
        if (onNeededHeightPx != null && reported[0] != placement.neededPx) {
            reported[0] = placement.neededPx
            onNeededHeightPx(placement.neededPx)
        }
        layout(constraints.maxWidth, slot) {
            anchorPlaceable.placeRelative(0, placement.topPx)
            dynamicPlaceable.placeRelative(0, placement.topPx + anchorPlaceable.height)
        }
    }
}
