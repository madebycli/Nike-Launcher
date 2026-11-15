// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.notifications

import dev.nikelauncher.media.PreviewMessage

/** Consecutive messages by one author, drawn as one stack of bubbles. */
internal data class MessageGroup(
    val fromSelf: Boolean,
    val sender: String,
    val messages: List<PreviewMessage>,
    /** A time label is drawn above the group when a pause separates it from the previous one. */
    val stampAbove: Boolean,
) {
    val icon get() = messages.firstNotNullOfOrNull { it.senderIcon }
}

/** Authors keep one stack until somebody else speaks or the conversation pauses this long. */
internal const val GROUP_PAUSE_MILLIS = 15 * 60_000L

/**
 * Split a provider's message list into stacks. The provider's order is kept: it is already
 * the order the app wants shown, and times of 0 (unset) must not reorder or split anything.
 */
internal fun groupMessages(messages: List<PreviewMessage>): List<MessageGroup> {
    val groups = ArrayList<MessageGroup>()
    var current = ArrayList<PreviewMessage>()
    var previous: PreviewMessage? = null
    var stamp = true
    fun flush() {
        val first = current.firstOrNull() ?: return
        groups += MessageGroup(first.fromSelf, first.sender, current.toList(), stamp)
        current = ArrayList()
    }
    for (message in messages) {
        val before = previous
        val paused = before != null && message.time > 0 && before.time > 0 &&
            message.time - before.time > GROUP_PAUSE_MILLIS
        val changedAuthor = before != null &&
            (before.fromSelf != message.fromSelf || before.sender != message.sender)
        if (before == null || paused || changedAuthor) {
            flush()
            stamp = before == null || paused
        }
        current += message
        previous = message
    }
    flush()
    return groups
}
