// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.screens

import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Icon
import androidx.compose.runtime.mutableStateListOf
import dev.nikelauncher.media.*
import dev.nikelauncher.ui.notifications.NotificationController
import org.robolectric.Shadows.shadowOf
import android.net.Uri
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

internal fun fakeNotifications(context: Context): List<NotificationPreview> {
    val now = System.currentTimeMillis()
    val pending = PendingIntent.getBroadcast(context, 10, Intent("fake.reply"), PendingIntent.FLAG_IMMUTABLE)
    val reply = RemoteInput.Builder("reply").setLabel("Reply").setChoices(arrayOf("See you soon", "Thanks")).build()
    val messages = listOf(
        PreviewMessage("Alex", "Are we still meeting this afternoon?", now - 8 * 60_000),
        PreviewMessage("Alex", "I can bring the calendar notes.", now - 7 * 60_000),
        PreviewMessage("Me", "Yes, the usual place works.", now - 6 * 60_000, fromSelf = true),
        PreviewMessage("Sam", "The train is running a little late, but I will be there at four.", now - 2 * 60_000),
        PreviewMessage("Alex", "Great. See you both there!", now - 60_000),
    )
    val picture = Bitmap.createBitmap(640, 320, Bitmap.Config.ARGB_8888).apply {
        val canvas = Canvas(this)
        canvas.drawColor(0xFF264653.toInt())
        val paint = Paint().apply { color = 0xFFE9C46A.toInt() }
        canvas.drawCircle(490f, 80f, 42f, paint)
        paint.color = 0xFF2A9D8F.toInt()
        canvas.drawRect(0f, 210f, 640f, 320f, paint)
    }
    val imageBytes = ByteArrayOutputStream().also { picture.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
    val attachment = Uri.parse("content://fake.messages/attachment")
    shadowOf(context.contentResolver).registerInputStreamSupplier(attachment) { ByteArrayInputStream(imageBytes) }
    return listOf(
        NotificationPreview("chat", "fake.messages", "Weekend plans", "", now,
            messages + PreviewMessage("Alex", "The view from the platform.", now - 30_000, attachment = attachment),
            listOf(PreviewAction("Reply", pending, listOf(reply)), PreviewAction("Mark as read", pending, emptyList())), pending),
        NotificationPreview("picture", "fake.gallery", "Alex shared a photo", "The view from the train.", now - 120_000,
            emptyList(), emptyList(), pending, picture = Icon.createWithBitmap(picture)),
        NotificationPreview("plain", "fake.calendar", "Planning meeting for next week", "Bring the updated schedule and the notes from our last meeting.", now - 180_000,
            emptyList(), emptyList(), pending),
    )
}

internal class FakeNotificationController(initial: List<NotificationPreview>) : NotificationController {
    val items = mutableStateListOf<NotificationPreview>().apply { addAll(initial) }
    val replies = mutableListOf<String>()
    override fun open(preview: NotificationPreview, intent: PendingIntent) = true
    override fun launchApp(packageName: String) = true
    override fun dismiss(preview: NotificationPreview): Boolean = items.removeAll { it.key == preview.key }
    override fun reply(preview: NotificationPreview, target: PreviewReplyTarget, text: String): Boolean { replies += text; return true }
    override fun run(preview: NotificationPreview, intent: PendingIntent) = true
}
