// SPDX-License-Identifier: GPL-3.0-or-later
// Adapted from Victoria Grid (GPL-3.0-or-later), ui/notifications/NotificationParts.kt.
package dev.nikelauncher.ui.notifications

import android.content.Context
import android.graphics.drawable.Icon
import android.net.Uri
import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.core.graphics.drawable.toBitmap
import dev.nikelauncher.media.NotificationPreviewParser
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val ICON_PX = 128

/** An icon from a notification, loaded off the main thread: photos can mean Binder or disk work. */
@Composable
internal fun notificationBitmap(icon: Icon?, maxSizePx: Int = ICON_PX): ImageBitmap? {
    val context = LocalContext.current.applicationContext
    // Keyed as well as the loader, so a replaced photo never draws its predecessor.
    val bitmap by key(context, icon, maxSizePx) {
        produceState<ImageBitmap?>(null) {
            value = withContext(Dispatchers.IO) {
                runCatching {
                    val drawable = icon?.loadDrawable(context) ?: return@runCatching null
                    val width = drawable.intrinsicWidth.coerceAtLeast(1)
                    val height = drawable.intrinsicHeight.coerceAtLeast(1)
                    val scale = minOf(1f, maxSizePx.toFloat() / maxOf(width, height))
                    drawable.toBitmap((width * scale).toInt().coerceAtLeast(1), (height * scale).toInt().coerceAtLeast(1)).asImageBitmap()
                }.getOrNull()
            }
        }
    }
    return bitmap
}

/** An image attached to a message, scaled down when it is read. */
@Composable
internal fun attachmentBitmap(uri: Uri?): ImageBitmap? {
    val context = LocalContext.current.applicationContext
    val bitmap by key(uri) {
        produceState<ImageBitmap?>(null) {
            value = uri?.let {
                withContext(Dispatchers.IO) { NotificationPreviewParser.imageAttachment(context, it)?.asImageBitmap() }
            }
        }
    }
    return bitmap
}

@Composable
private fun avatarColors(seed: String): Pair<Color, Color> {
    val colors = MaterialTheme.colorScheme
    // One person keeps one color throughout a chat.
    return when (Math.floorMod(seed.lowercase().hashCode(), 3)) {
        0 -> colors.primaryContainer to colors.onPrimaryContainer
        1 -> colors.tertiaryContainer to colors.onTertiaryContainer
        else -> colors.secondaryContainer to colors.onSecondaryContainer
    }
}

/** A person's photo when the notification has one, otherwise an initial. */
@Composable
internal fun SenderAvatar(name: String, icon: Icon?, size: Dp, modifier: Modifier = Modifier) {
    val (container, onContainer) = avatarColors(name)
    val photo = notificationBitmap(icon)
    Box(modifier.size(size).clip(CircleShape).background(container), contentAlignment = Alignment.Center) {
        val initial = name.trim().firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()
        when {
            photo != null -> Image(photo, contentDescription = null, modifier = Modifier.size(size), contentScale = ContentScale.Crop)
            initial != null -> Text(initial.toString(), color = onContainer, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            else -> Icon(Icons.Outlined.Notifications, null, Modifier.size(size * .5f), tint = onContainer)
        }
    }
}

/** "14:32" today, "Mon 14:32" this week, otherwise date and time. */
internal fun messageStamp(context: Context, time: Long, now: Long): String {
    val clock = DateFormat.getTimeFormat(context).format(Date(time))
    return when {
        DateUtils.isToday(time) -> clock
        now - time < 6 * DateUtils.DAY_IN_MILLIS ->
            DateUtils.formatDateTime(context, time, DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_WEEKDAY) + " " + clock
        else -> DateUtils.formatDateTime(context, time, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH) + " " + clock
    }
}

internal fun appLabel(context: Context, packageName: String): String = runCatching {
    context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(packageName, 0)).toString()
}.getOrDefault(packageName)
