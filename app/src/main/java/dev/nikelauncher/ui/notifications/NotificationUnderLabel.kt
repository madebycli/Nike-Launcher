// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.notifications

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nikelauncher.R
import dev.nikelauncher.data.HomeAlignment
import dev.nikelauncher.media.NotificationPreview
import dev.nikelauncher.ui.common.LocalIconConfig
import dev.nikelauncher.ui.home.rows.boxAlignment
import dev.nikelauncher.ui.home.stack.LocalBlockGrowth
import dev.nikelauncher.ui.home.rows.columnAlignment
import dev.nikelauncher.ui.home.rows.textAlign
import dev.nikelauncher.ui.theme.WidgetTextFamily
import kotlinx.coroutines.delay

/**
 * An app's name with its newest notification hanging underneath.
 *
 * The name sits in a line exactly as tall as the icon, so the row looks the same with and without
 * a notification and the icon never moves when one arrives or goes: the lines are only added below.
 * Rows that are laid out with this align their content to the top, not the centre, for that reason.
 */
@Composable
internal fun LabelWithNotification(
    iconSizeDp: Int,
    alignment: HomeAlignment,
    preview: NotificationPreview?,
    color: Color,
    labelSizeSp: Int,
    onOpenNotifications: () -> Unit,
    modifier: Modifier = Modifier,
    /** The row's own long press, so pressing on the lines still opens the app's menu. */
    onLongClick: (() -> Unit)? = null,
    label: @Composable () -> Unit,
) {
    val showIcons = LocalIconConfig.current.showIcons
    Column(modifier, horizontalAlignment = alignment.columnAlignment()) {
        Box(Modifier.heightIn(min = if (showIcons) iconSizeDp.dp else 0.dp), contentAlignment = alignment.boxAlignment()) {
            label()
        }
        if (preview != null) {
            val growth = LocalBlockGrowth.current
            val owner = remember { Any() }
            DisposableEffect(growth) { onDispose { growth?.forget(owner) } }
            NotificationSummary(
                preview, alignment, color, labelSizeSp, onOpenNotifications, onLongClick,
                Modifier.onSizeChanged { growth?.report(owner, it.height) },
            )
        }
    }
}

/** Who wrote and when, then what: two short lines, the second the newest message. */
@Composable
private fun NotificationSummary(
    preview: NotificationPreview,
    alignment: HomeAlignment,
    color: Color,
    labelSizeSp: Int,
    onOpen: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier,
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(preview.key) {
        while (true) {
            now = System.currentTimeMillis()
            delay(60_000 - now % 60_000)
        }
    }
    val locale = LocalConfiguration.current.locales[0]
    val size = (labelSizeSp * 14f / 16f).sp
    val age = notificationAge(locale, preview.time, now)
    val message = preview.messages.lastOrNull()?.text ?: preview.text
    Column(
        modifier.combinedClickable(
            onClickLabel = stringResource(R.string.notifications_title),
            onClick = onOpen,
            onLongClick = onLongClick,
        ),
        horizontalAlignment = alignment.columnAlignment(),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        // The same full colour as the name above, so the lines read as part of the row. Title and age share
        // the first line in semibold; the title is what gets cut, the age always stays whole.
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (preview.title.isNotBlank()) {
                Text(preview.title, Modifier.weight(1f, fill = false), color = color,
                    fontFamily = WidgetTextFamily, fontWeight = FontWeight.SemiBold, fontSize = size,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(stringResource(R.string.notification_age_separator, age), color = color,
                    fontFamily = WidgetTextFamily, fontSize = size, maxLines = 1)
            } else {
                Text(age, color = color, fontFamily = WidgetTextFamily, fontSize = size, maxLines = 1)
            }
        }
        if (message.isNotBlank()) {
            Text(message, color = color, fontFamily = WidgetTextFamily, fontSize = size,
                maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = alignment.textAlign())
        }
    }
}
