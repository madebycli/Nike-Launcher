// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.notifications

import dev.nikelauncher.media.PreviewMessage

/**
 * A reply handed to the app, shown at once as the reader's own bubble until the app echoes it back into the
 * conversation. [echoesBefore] is how many own messages with the same text were already there when it was sent, so an
 * earlier identical reply does not count as its echo.
 */
internal data class OutgoingReply(val id: Long, val text: String, val echoesBefore: Int)

internal fun ownEchoes(messages: List<PreviewMessage>, text: String): Int =
    messages.count { it.fromSelf && it.text.trim() == text.trim() }

/** The replies still waiting for their echo. */
internal fun stillPending(pending: List<OutgoingReply>, messages: List<PreviewMessage>): List<OutgoingReply> =
    pending.filter { ownEchoes(messages, it.text) <= it.echoesBefore }
