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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nikelauncher.R
import dev.nikelauncher.data.AppInfo
import dev.nikelauncher.data.IconSide
import dev.nikelauncher.media.NotificationPreview
import dev.nikelauncher.ui.common.AppIcon
import dev.nikelauncher.ui.common.LocalIconConfig
import dev.nikelauncher.ui.theme.WidgetTextFamily
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
                Modifier.padding(start = if (iconSide == IconSide.RIGHT) 8.dp else 0.dp,
                    end = if (iconSide == IconSide.LEFT) 8.dp else 0.dp).combinedClickable(
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
            Text(
                stringResource(R.string.notification_app_age, label, notificationAge(locale, preview.time, now)),
                Modifier.combinedClickable(onClick = onLaunch, onLongClick = onLongClick),
                maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = labelSizeSp.sp,
                fontWeight = FontWeight.SemiBold, fontFamily = WidgetTextFamily,
            )
        },
        content = {
            if (preview.title.isNotBlank()) Text(preview.title, maxLines = 1,
                overflow = TextOverflow.Ellipsis, fontFamily = WidgetTextFamily, fontSize = (labelSizeSp * 14f / 16f).sp)
        },
        supportingContent = {
            val message = preview.messages.lastOrNull()?.text ?: preview.text
            if (message.isNotBlank()) Text(message, maxLines = 1,
                overflow = TextOverflow.Ellipsis, fontFamily = WidgetTextFamily, fontSize = (labelSizeSp * 14f / 16f).sp)
        },
        leadingContent = if (iconSide == IconSide.LEFT) icon else expand,
        trailingContent = if (iconSide == IconSide.RIGHT) icon else expand,
        colors =
            ListItemDefaults.colors(
                containerColor = Color.Transparent,
                headlineColor = color,
                overlineColor = color,
                supportingColor = color,
                leadingIconColor = color,
                trailingIconColor = color,
            ),
    )
}
