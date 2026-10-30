// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.applist.rows

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun SectionHeader(text: String, style: ListRowStyle) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 28.dp), contentAlignment = style.alignment.contentAlignment()) {
        Text(
            text = text,
            color = style.contentColor,
            fontSize = (style.labelSizeSp + 2).sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 18.dp, bottom = 6.dp),
        )
    }
}
