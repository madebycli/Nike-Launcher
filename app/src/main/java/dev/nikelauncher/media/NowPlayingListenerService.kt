// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.media

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.Context
import android.provider.Settings
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.content.ContextCompat
import dev.nikelauncher.NikeApp
import dev.nikelauncher.widget.ClockWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NowPlayingListenerService : NotificationListenerService() {

    companion object {
        /**
         * Asks the system to bind us again, when it is willing to and has not.
         *
         * Replacing the app kills the binding but leaves the permission switched on, so the
         * Now Playing card quietly stops updating and the setting still reads as granted —
         * there is nothing to turn back on, which makes it look like the feature broke. That
         * happens on every update, not only a sideload. requestRebind is the sanctioned way
         * back and does nothing if the permission was never given.
         */
        fun rebindIfPermitted(context: Context) {
            val component = ComponentName(context, NowPlayingListenerService::class.java)
            val enabled = runCatching {
                Settings.Secure.getString(
                    context.contentResolver,
                    "enabled_notification_listeners",
                )
            }.getOrNull().orEmpty()
            if (!enabled.contains(component.flattenToString()) &&
                !enabled.contains(component.packageName + "/" + component.className)
            ) {
                return
            }
            runCatching { requestRebind(component) }
        }
    }

    private lateinit var sessionManager: MediaSessionManager
    private lateinit var componentName: ComponentName

    private var watched: MediaController? = null

    private val previewScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var previewsEnabled = false

    /** Notification content stays in memory only while the phone is unlocked and the setting is on. */
    private val lockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) NotificationPreviewBus.clear() else refreshPreviews()
        }
    }

    override fun onCreate() {
        super.onCreate()
        ContextCompat.registerReceiver(
            this,
            lockReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(Intent.ACTION_USER_UNLOCKED)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        previewScope.launch {
            (application as NikeApp).prefs.appList.notificationPreviews.collectLatest { enabled ->
                previewsEnabled = enabled
                if (enabled) refreshPreviews() else NotificationPreviewBus.clear()
            }
        }
    }

    override fun onDestroy() {
        previewScope.cancel()
        unregisterReceiver(lockReceiver)
        NotificationPreviewBus.disconnect()
        super.onDestroy()
    }

    /** Reads everything on the shade again, off the main thread: parsing every app's notification is not cheap. */
    private fun refreshPreviews() {
        if (!previewsEnabled) return
        val revision = NotificationPreviewBus.revision
        previewScope.launch {
            val active = withContext(Dispatchers.IO) { runCatching { activeNotifications }.getOrNull() } ?: return@launch
            val previews = withContext(Dispatchers.Default) { NotificationPreviewBus.parseSnapshot(this@NowPlayingListenerService, active) }
            NotificationPreviewBus.updateSnapshotIfUnchanged(this@NowPlayingListenerService, revision, previews)
        }
    }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = republish()
        override fun onPlaybackStateChanged(state: PlaybackState?) = republish()
        override fun onSessionDestroyed() {
            detach()
            republish()
        }
    }

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        attachTo(controllers)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        NotificationCountBus.canceller = { key -> cancelNotification(key) }
        NotificationPreviewBus.connect { key -> cancelNotification(key) }
        publishCounts()
        refreshPreviews()
        sessionManager = getSystemService(MediaSessionManager::class.java)
        componentName = ComponentName(this, NowPlayingListenerService::class.java)
        try {
            sessionManager.addOnActiveSessionsChangedListener(sessionsListener, componentName)
            attachTo(sessionManager.getActiveSessions(componentName))
        } catch (e: SecurityException) {
            NowPlayingBus.update(null)
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        NotificationCountBus.canceller = null
        NotificationPreviewBus.disconnect()
        runCatching { sessionManager.removeOnActiveSessionsChangedListener(sessionsListener) }
        detach()
        NowPlayingBus.update(null)
        NotificationCountBus.clear()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        // The posted payload is newer than anything a full query would return a moment later.
        if (sbn != null) {
            if (previewsEnabled) NotificationPreviewBus.posted(this, sbn) else NotificationPreviewBus.clear()
        }
        refresh()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        if (sbn != null) NotificationPreviewBus.removed(sbn.key)
        refresh()
    }

    private fun refresh() {
        if (::sessionManager.isInitialized) {
            runCatching { attachTo(sessionManager.getActiveSessions(componentName)) }
        }
        publishCounts()
    }

    /**
     * What is on the shade right now, counted by the app that put it there.
     *
     * Recounted from scratch rather than added to and subtracted from: a posted notification
     * can replace one already there without a removal ever arriving, and a count kept by
     * arithmetic drifts upward until something clears it.
     *
     * Ongoing notifications are left out — a music player or a running download is a state,
     * not something waiting to be read — and so are group summaries, which would count the
     * same messages a second time.
     */
    private fun publishCounts() {
        val active = runCatching { activeNotifications }.getOrNull()
        if (active == null) {
            NotificationCountBus.clear()
            ClockWidgetProvider.renderAll(this)
            return
        }
        val counts = HashMap<String, Int>()
        val shown = HashMap<String, MutableList<StatusBarNotification>>()
        active.forEach { sbn ->
            if (!sbn.isClearable) return@forEach
            val notification = sbn.notification ?: return@forEach
            if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return@forEach
            counts[sbn.packageName] = (counts[sbn.packageName] ?: 0) + 1
            if (notification.visibility == Notification.VISIBILITY_SECRET) return@forEach
            shown.getOrPut(sbn.packageName) { mutableListOf() }.add(sbn)
        }
        val previews = shown.mapValues { (_, list) ->
            list.sortedByDescending { it.postTime }.mapNotNull { sbn ->
                previewOf(sbn.notification)?.let { text ->
                    NotificationLine(
                        key = sbn.key,
                        text = text,
                        intent = sbn.notification.contentIntent,
                        autoCancel = sbn.notification.flags and Notification.FLAG_AUTO_CANCEL != 0,
                    )
                }
            }.take(MAX_NOTIFICATION_LINES)
        }.filterValues { it.isNotEmpty() }
        val before = NotificationCountBus.counts.value.values.sum()
        NotificationCountBus.update(counts, previews)
        // A widget is drawn only when something asks. The home screen follows the bus on its
        // own; a RemoteViews cannot, so the total changing is what has to ask for it.
        if (counts.values.sum() != before) ClockWidgetProvider.renderAll(this)
    }

    /** "Title: text" on one line, or whichever of the two the notification has. */
    private fun previewOf(notification: Notification): String? {
        val extras = notification.extras ?: return null
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        val line = when {
            title.isNotEmpty() && text.isNotEmpty() -> "$title: $text"
            else -> title + text
        }
        return line.replace('\n', ' ').takeIf { it.isNotEmpty() }
    }

    /** Follow whichever session is worth showing, and watch it for changes. */
    private fun attachTo(controllers: List<MediaController>?) {
        val next = controllers?.firstOrNull { isLive(it.playbackState) }
            ?: controllers?.firstOrNull { it.playbackState != null }

        if (next?.sessionToken != watched?.sessionToken) {
            detach()
            watched = next
            next?.registerCallback(controllerCallback)
        }
        republish()
    }

    private fun detach() {
        watched?.let { runCatching { it.unregisterCallback(controllerCallback) } }
        watched = null
    }

    private fun republish() {
        val controller = watched
        val state = controller?.playbackState
        if (controller == null || !isLive(state)) {
            NowPlayingBus.update(null)
            return
        }

        val metadata = controller.metadata
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty()
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST).orEmpty()
        if (title.isBlank() && artist.isBlank()) {
            NowPlayingBus.update(null)
            return
        }

        NowPlayingBus.update(
            NowPlaying(
                title = title,
                artist = artist,
                isPlaying = state?.state == PlaybackState.STATE_PLAYING,
                art = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                    ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART),
                controller = controller,
            )
        )
    }

    /** Playing, paused or buffering counts; stopped, errored or idle does not. */
    private fun isLive(state: PlaybackState?): Boolean = when (state?.state) {
        PlaybackState.STATE_PLAYING,
        PlaybackState.STATE_PAUSED,
        PlaybackState.STATE_BUFFERING,
        -> true

        else -> false
    }
}