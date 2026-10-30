// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.media

import android.app.ActivityOptions
import android.app.KeyguardManager
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Notification content stays in memory and is discarded on lock, opt-out or disconnect. */
data class PreviewMessage(val sender: String, val text: String, val time: Long,
    val senderIcon: android.graphics.drawable.Icon? = null,
    /** Written by the device owner, so it belongs on the sending side of a conversation. */
    val fromSelf: Boolean = false,
    /** Optional provider-granted image, held only in the in-memory notification snapshot. */
    val attachment: android.net.Uri? = null)

data class PreviewAction(
    val title: String,
    val intent: PendingIntent,
    val inputs: List<RemoteInput>,
    /** In-memory presentation metadata; never persisted or guessed from a translated label. */
    val icon: android.graphics.drawable.Icon? = null,
    val semanticAction: Int = 0,
)

data class NotificationPreview(
    val key: String,
    val packageName: String,
    val title: String,
    val text: String,
    val time: Long,
    val messages: List<PreviewMessage>,
    val actions: List<PreviewAction>,
    val contentIntent: PendingIntent?,
    val smallIcon: android.graphics.drawable.Icon? = null,
    /** Provider-supplied contact/conversation picture, kept separate from the app status icon. */
    val conversationIcon: android.graphics.drawable.Icon? = null,
    /** BigPictureStyle image; retained only while this notification is available. */
    val picture: android.graphics.drawable.Icon? = null,
)

/** Stable reply destination: never rely on the provider's current action-list position. */
data class PreviewReplyTarget(val intent: PendingIntent, val resultKey: String)

object NotificationPreviewBus {
    private val mutable = MutableStateFlow<List<NotificationPreview>>(emptyList())
    /** Invalidates background work when content changes or privacy clears it. Main-thread owned. */
    internal var revision: Long = 0
        private set
    val notifications: StateFlow<List<NotificationPreview>> = mutable
    // Posted callbacks are the authoritative stream during this unlocked listener epoch.
    // A Binder snapshot has no sequence token and may omit a key already posted to us.
    // Keys only (bounded with the public 100-item list); no second content/action cache.
    private val callbackKeys = HashSet<String>()
    private val removedKeys = LinkedHashSet<String>()
    private var dismiss: ((String) -> Unit)? = null
    private val connection = MutableStateFlow(false)
    val listenerConnected: StateFlow<Boolean> = connection

    fun connect(cancel: (String) -> Unit) {
        dismiss = cancel
        connection.value = true
    }

    fun disconnect() {
        dismiss = null
        connection.value = false
        clear()
    }

    fun clear() {
        HiddenConversationMessages.clear()
        revision++
        callbackKeys.clear()
        removedKeys.clear()
        mutable.value = emptyList()
    }

    fun dismiss(key: String): Boolean {
        if (mutable.value.none { it.key == key }) return false
        val cancel = dismiss ?: return false
        return runCatching { cancel(key); true }.getOrDefault(false)
    }

    fun dismiss(context: Context, key: String): Boolean {
        if (context.getSystemService(KeyguardManager::class.java)?.isDeviceLocked != false)
            return false
        return dismiss(key)
    }

    private fun unlocked(context: Context): Boolean =
        context.getSystemService(KeyguardManager::class.java)?.isDeviceLocked == false

    fun update(context: Context, active: Array<StatusBarNotification>) {
        if (!unlocked(context)) {
            clear()
            return
        }
        callbackKeys.clear()
        removedKeys.clear()
        publish(parseSnapshot(context, active))
    }

    /** Parse away from Main; applying the result must still recheck revision and device lock. */
    internal fun parseSnapshot(context: Context, active: Array<StatusBarNotification>) =
        active.asSequence().sortedByDescending { it.postTime }
            .mapNotNull { NotificationPreviewParser.parse(context, it) }.take(100).toList()

    internal fun updateSnapshotIfUnchanged(
        context: Context, expectedRevision: Long, previews: List<NotificationPreview>,
    ): Boolean {
        if (revision != expectedRevision) return false
        if (!unlocked(context)) {
            clear()
            return false
        }
        // A full Binder snapshot can be older even when its query starts after a callback.
        // Callback-owned content survives until an explicit removal or privacy/listener reset.
        // Initial/unlock snapshots start with an empty set and remain fully authoritative.
        val current = mutable.value.associateBy { it.key }
        val visibleSnapshot = previews.filterNot { it.key in removedKeys }
        val snapshotKeys = visibleSnapshot.mapTo(HashSet()) { it.key }
        val reconciled = visibleSnapshot.map { incoming ->
            val live = current[incoming.key]?.takeIf { it.key in callbackKeys }
            if (live != null) enrichPictures(live, incoming) else incoming
        }
        publish(reconciled + current.values.filter {
            it.key in callbackKeys && it.key !in snapshotKeys
        })
        return true
    }

    private fun enrichPictures(live: NotificationPreview, snapshot: NotificationPreview) =
        live.copy(
            conversationIcon = live.conversationIcon ?: snapshot.conversationIcon.takeIf {
                live.packageName == snapshot.packageName && live.title == snapshot.title &&
                    live.contentIntent == snapshot.contentIntent
            },
            messages = live.messages.map { message ->
                val matching = snapshot.messages.lastOrNull { it.sender == message.sender &&
                        it.fromSelf == message.fromSelf && it.text == message.text &&
                        it.time == message.time }
                message.copy(senderIcon = message.senderIcon ?: matching?.senderIcon,
                    attachment = message.attachment ?: matching?.attachment)
            },
        )

    /** The posted payload is newer than a potentially stale getActiveNotifications snapshot. */
    fun posted(context: Context, notification: StatusBarNotification) {
        if (!unlocked(context)) {
            clear()
            return
        }
        val current = mutable.value.filterNot { it.key == notification.key }
        val preview = NotificationPreviewParser.parse(context, notification)
        if (preview == null) suppress(notification.key)
        else {
            removedKeys.remove(notification.key)
            callbackKeys.add(notification.key)
        }
        publish(if (preview == null) current else current + preview)
    }

    fun removed(key: String) {
        revision++
        suppress(key)
        mutable.value = mutable.value.filterNot { it.key == key }
    }

    private fun suppress(key: String) {
        HiddenConversationMessages.remove(key)
        callbackKeys.remove(key)
        removedKeys.remove(key)
        removedKeys.add(key)
        // Only identifiers, never message bodies. Bound churn during a long unlocked session.
        while (removedKeys.size > 2048) removedKeys.remove(removedKeys.first())
    }

    private fun publish(previews: List<NotificationPreview>) {
        revision++
        mutable.value = previews.sortedByDescending { it.time }.take(100)
        callbackKeys.retainAll(mutable.value.mapTo(HashSet()) { it.key })
    }

    /** User-initiated open: use this notification's exact live destination, never an app launch. */
    fun open(context: Context, key: String, expectedIntent: PendingIntent): Boolean {
        if (!unlocked(context)) return false
        val pending = mutable.value.firstOrNull { it.key == key }?.contentIntent
            ?.takeIf { it == expectedIntent } ?: return false
        return runCatching {
            val options = ActivityOptions.makeBasic()
            if (android.os.Build.VERSION.SDK_INT >= 34)
                options.setPendingIntentBackgroundActivityStartMode(
                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
            pending.send(context, 0, null, null, null, null, options.toBundle())
            true
        }.getOrDefault(false)
    }

    /** Compatibility for callers that choose an action at the instant of submission. */
    fun sendReply(context: Context, key: String, actionIndex: Int, text: String): Boolean {
        val action = mutable.value.firstOrNull { it.key == key }?.actions?.getOrNull(actionIndex)
            ?: return false
        val input = action.inputs.firstOrNull {
            it.allowFreeFormInput || it.choices?.any { choice -> choice.toString() == text } == true
        } ?: return false
        return sendReply(context, key, PreviewReplyTarget(action.intent, input.resultKey), text)
    }

    /** Re-resolve the exact live intent/input; replaced notifications cannot retarget a draft. */
    fun sendReply(context: Context, key: String, target: PreviewReplyTarget, text: String): Boolean {
        if (text.isBlank() || text.length > 4096 ||
            context.getSystemService(KeyguardManager::class.java)?.isDeviceLocked != false)
            return false
        val action = mutable.value.firstOrNull { it.key == key }?.actions
            ?.firstOrNull { candidate ->
                candidate.intent == target.intent &&
                    candidate.inputs.any { it.resultKey == target.resultKey }
            } ?: return false
        val input = action.inputs.firstOrNull { it.resultKey == target.resultKey } ?: return false
        if (!input.allowFreeFormInput &&
            input.choices?.any { it.toString() == text } != true) return false
        val fillIn = Intent()
        RemoteInput.addResultsToIntent(
            arrayOf(input),
            fillIn,
            Bundle().apply { putCharSequence(input.resultKey, text) },
        )
        if (android.os.Build.VERSION.SDK_INT >= 28)
            RemoteInput.setResultsSource(fillIn,
                if (input.allowFreeFormInput) RemoteInput.SOURCE_FREE_FORM_INPUT
                else RemoteInput.SOURCE_CHOICE)
        return runCatching { action.intent.send(context, 0, fillIn); true }.getOrDefault(false)
    }

    fun runAction(context: Context, key: String, actionIndex: Int): Boolean {
        val action = mutable.value.firstOrNull { it.key == key }?.actions
            ?.getOrNull(actionIndex) ?: return false
        if (action.inputs.isNotEmpty()) return false
        return runAction(context, key, action.intent)
    }

    fun runAction(context: Context, key: String, expectedIntent: PendingIntent): Boolean {
        if (context.getSystemService(KeyguardManager::class.java)?.isDeviceLocked != false)
            return false
        val action = mutable.value.firstOrNull { it.key == key }?.actions
            ?.firstOrNull { it.intent == expectedIntent && it.inputs.isEmpty() } ?: return false
        return runCatching { action.intent.send(); true }.getOrDefault(false)
    }
}
