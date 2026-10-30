// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.media

import android.app.Notification
import android.app.RemoteInput
import android.content.Context
import android.os.Build
import android.os.Process
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat

/** Read the provider's actual actions, including companion-device/automotive reply actions. */
internal object NotificationPreviewParser {
    fun parse(context: Context, sbn: StatusBarNotification): NotificationPreview? {
        if (sbn.user != Process.myUserHandle() || !sbn.isClearable ||
            sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0 ||
            sbn.notification.visibility == Notification.VISIBILITY_SECRET) return null
        return runCatching {
            val n = sbn.notification
            fun text(key: String, max: Int = 4096) =
                n.extras.getCharSequence(key)?.toString().orEmpty().take(max)
            val style = NotificationCompat.MessagingStyle
                .extractMessagingStyleFromNotification(n)
            val owner = style?.user
            val messages = style?.messages.orEmpty().takeLast(20)
                .map { message ->
                    val person = message.person
                    // A message with no author is the owner's own in the platform's model.
                    val own = if (person == null) message.sender.isNullOrBlank()
                        else owner != null && (
                            (person.key != null && person.key == owner.key) ||
                            (person.key == null && owner.key == null &&
                                person.name?.toString() == owner.name?.toString()))
                    val attachment = message.dataUri?.takeIf {
                        it.scheme == "content" && message.dataMimeType?.startsWith("image/") == true
                    }
                    PreviewMessage(message.sender?.toString().orEmpty().take(128),
                        message.text?.toString().orEmpty().take(4096), message.timestamp,
                        runCatching { person?.icon?.toIcon(context) }.getOrNull(), own, attachment)
                }.filter { it.text.isNotBlank() || it.attachment != null }
            val body = text(Notification.EXTRA_BIG_TEXT).ifBlank { text(Notification.EXTRA_TEXT) }
                .ifBlank {
                    n.extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES).orEmpty()
                        .takeLast(4).joinToString("\n").take(4096)
                }
            val explicitTitle = text(Notification.EXTRA_CONVERSATION_TITLE, 256)
            val suppliedTitle = text(Notification.EXTRA_TITLE, 256)
            // Some providers replace EXTRA_TITLE with the owner's name after a direct reply.
            // Use the actual remote Person for a one-to-one chat, never a translated "Me" guess.
            val incoming = messages.lastOrNull { !it.fromSelf }
            val title = explicitTitle.ifBlank {
                if (style != null && !style.isGroupConversation &&
                    suppliedTitle == owner?.name?.toString())
                    incoming?.sender?.takeIf { it.isNotBlank() } ?: suppliedTitle
                else suppliedTitle
            }
            val conversationIcon = runCatching { n.getLargeIcon() }.getOrNull()
                ?: incoming?.senderIcon
            @Suppress("DEPRECATION")
            val suppliedPicture = n.extras.getParcelable<android.os.Parcelable>(Notification.EXTRA_PICTURE)
                ?: if (Build.VERSION.SDK_INT >= 31)
                    n.extras.getParcelable<android.os.Parcelable>(Notification.EXTRA_PICTURE_ICON) else null
            val picture = when (val supplied = suppliedPicture) {
                is android.graphics.Bitmap -> android.graphics.drawable.Icon.createWithBitmap(supplied)
                is android.graphics.drawable.Icon -> supplied
                else -> null
            }
            // Some providers post a title first and enrich the same key a moment later.
            if (title.isBlank() && body.isBlank() && messages.isEmpty() && picture == null) return@runCatching null
            NotificationPreview(sbn.key, sbn.packageName, title, body, sbn.postTime,
                messages, actions(context, n), n.contentIntent, n.smallIcon, conversationIcon, picture)
        }.getOrNull()
    }

    internal fun imageAttachment(context: Context, uri: android.net.Uri): android.graphics.Bitmap? {
        if (uri.scheme != "content") return null
        return runCatching {
            val resolver = context.contentResolver
            val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0 ||
                bounds.outWidth.toLong() * bounds.outHeight > 40_000_000L) return null
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 256) sample *= 2
            val options = android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }
            resolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, options) }
        }.getOrNull()
    }

    private fun actions(context: Context, notification: Notification): List<PreviewAction> {
        val supplied = ArrayList<NotificationCompat.Action>()
        repeat(minOf(NotificationCompat.getActionCount(notification), 32)) { index ->
            runCatching { NotificationCompat.getAction(notification, index) }.getOrNull()
                ?.let { supplied.add(it) }
        }
        runCatching { NotificationCompat.WearableExtender(notification).actions }.getOrNull()
            ?.take(16)?.let { supplied.addAll(it) }
        runCatching { NotificationCompat.getInvisibleActions(notification) }.getOrNull()
            ?.take(16)?.let { supplied.addAll(it) }
        return supplied.mapNotNull { action ->
            val pending = action.actionIntent ?: return@mapNotNull null
            val inputs = action.remoteInputs.orEmpty()
                .filter { it.allowFreeFormInput || !it.choices.isNullOrEmpty() }
                .take(8).map { input ->
                    RemoteInput.Builder(input.resultKey).setLabel(input.label)
                        .setChoices(input.choices).setAllowFreeFormInput(input.allowFreeFormInput)
                        .addExtras(input.extras).apply {
                            if (Build.VERSION.SDK_INT >= 29) {
                                when (input.editChoicesBeforeSending) {
                                    androidx.core.app.RemoteInput.EDIT_CHOICES_BEFORE_SENDING_ENABLED ->
                                        setEditChoicesBeforeSending(RemoteInput.EDIT_CHOICES_BEFORE_SENDING_ENABLED)
                                    androidx.core.app.RemoteInput.EDIT_CHOICES_BEFORE_SENDING_DISABLED ->
                                        setEditChoicesBeforeSending(RemoteInput.EDIT_CHOICES_BEFORE_SENDING_DISABLED)
                                    else -> setEditChoicesBeforeSending(RemoteInput.EDIT_CHOICES_BEFORE_SENDING_AUTO)
                                }
                            }
                        }.build()
                }
            PreviewAction(
                action.title?.toString().orEmpty().take(128), pending, inputs,
                icon = runCatching { action.iconCompat?.toIcon(context) }.getOrNull(),
                semanticAction = action.semanticAction,
            )
        }
            .sortedByDescending { action -> action.inputs.any { it.allowFreeFormInput } }
            .distinctBy { it.intent to it.inputs.map { input -> input.resultKey } }
            .take(24)
    }
}
