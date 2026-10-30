// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.notifications

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nikelauncher.R
import dev.nikelauncher.data.AppInfo
import dev.nikelauncher.data.IconSide
import dev.nikelauncher.media.NotificationPreview
import dev.nikelauncher.ui.common.AppIcon
import dev.nikelauncher.ui.common.LocalIconConfig
import kotlinx.coroutines.delay

/** Grid's flat app/header/message hierarchy, expressed with the stock Material ListItem. */
@Composable
internal fun NotificationAppContent(
    app: AppInfo,
    label: String,
    preview: NotificationPreview,
    color: Color,
    labelSizeSp: Int,
    iconSizeDp: Int,
    iconSide: IconSide,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit,
    onLaunch: () -> Unit,
    onLongClick: () -> Unit,
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(preview.key) {
        while (true) {
            now = System.currentTimeMillis()
            delay(60_000 - now % 60_000)
        }
    }
    val locale = LocalConfiguration.current.locales[0]
    val showIcons = LocalIconConfig.current.showIcons
    val icon: @Composable () -> Unit = {
        if (showIcons)
            Box(
                Modifier.combinedClickable(
                    onClickLabel = stringResource(R.string.notification_open),
                    onClick = onLaunch,
                    onLongClick = onLongClick,
                )
            ) {
                AppIcon(app, iconSizeDp)
            }
    }
    val expand: @Composable () -> Unit = {
        IconButton(onClick = onOpen) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                stringResource(R.string.notifications_title),
            )
        }
    }
    ListItem(
        modifier = modifier,
        onClick = onOpen,
        onLongClick = onLongClick,
        overlineContent = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    label,
                    Modifier.weight(1f)
                        .combinedClickable(onClick = onLaunch, onLongClick = onLongClick),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = labelSizeSp.sp,
                )
                Text(notificationAge(locale, preview.time, now), maxLines = 1)
            }
        },
        content = { Text(preview.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(
                preview.messages.lastOrNull()?.text ?: preview.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontSize = LocalIconConfig.current.notificationTextSp.sp,
            )
        },
        leadingContent = if (iconSide == IconSide.LEFT) icon else expand,
        trailingContent = if (iconSide == IconSide.RIGHT) icon else expand,
        colors =
            ListItemDefaults.colors(
                containerColor = Color.Transparent,
                headlineColor = color,
                overlineColor = color.copy(alpha = .8f),
                supportingColor = color.copy(alpha = .75f),
                leadingIconColor = color,
                trailingIconColor = color,
            ),
    )
}
