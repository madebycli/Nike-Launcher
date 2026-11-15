// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.rows

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import dev.nikelauncher.data.HomeAlignment

internal fun HomeAlignment.boxAlignment() = when (this) {
    HomeAlignment.LEFT -> Alignment.CenterStart
    HomeAlignment.CENTER -> Alignment.Center
    HomeAlignment.RIGHT -> Alignment.CenterEnd
}

internal fun HomeAlignment.columnAlignment() = when (this) {
    HomeAlignment.LEFT -> Alignment.Start
    HomeAlignment.CENTER -> Alignment.CenterHorizontally
    HomeAlignment.RIGHT -> Alignment.End
}

internal fun HomeAlignment.textAlign() = when (this) {
    HomeAlignment.LEFT -> TextAlign.Start
    HomeAlignment.CENTER -> TextAlign.Center
    HomeAlignment.RIGHT -> TextAlign.End
}

internal fun HomeAlignment.arrangement() = when (this) {
    HomeAlignment.LEFT -> Arrangement.Start
    HomeAlignment.CENTER -> Arrangement.Center
    HomeAlignment.RIGHT -> Arrangement.End
}

/** Which side a width-capped stack sits against, so it stays where the rows are aligned. */
internal fun HomeAlignment.wideScreenAlignment(): Alignment = when (this) {
    HomeAlignment.LEFT -> Alignment.TopStart
    HomeAlignment.CENTER -> Alignment.TopCenter
    HomeAlignment.RIGHT -> Alignment.TopEnd
}
