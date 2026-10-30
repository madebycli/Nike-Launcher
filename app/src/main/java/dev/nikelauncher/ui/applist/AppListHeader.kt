// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.applist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nikelauncher.data.HomeAlignment

@Composable
internal fun SectionHeader(
    text: String,
    labelSizeSp: Int,
    contentColor: Color,
    alignment: HomeAlignment,
) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
        contentAlignment =
            when (alignment) {
                HomeAlignment.LEFT -> Alignment.CenterStart
                HomeAlignment.CENTER -> Alignment.Center
                HomeAlignment.RIGHT -> Alignment.CenterEnd
            },
    ) {
        Text(
            text = text,
            color = contentColor,
            fontSize = (labelSizeSp + 2).sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 18.dp, bottom = 6.dp),
        )
    }
}
