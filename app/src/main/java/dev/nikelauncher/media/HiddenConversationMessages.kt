// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.media

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** In-memory presentation only: this never deletes a message from its source app. */
internal data class HiddenMessageId(
    val sender: String, val text: String, val time: Long, val self: Boolean,
    val attachment: String?, val occurrence: Int,
)

internal fun messageIds(messages: List<PreviewMessage>): List<HiddenMessageId> {
    val counts = mutableMapOf<HiddenMessageId, Int>()
    return messages.map { message ->
        val base = HiddenMessageId(message.sender, message.text, message.time, message.fromSelf,
            message.attachment?.toString(), 0)
        val occurrence = counts.getOrDefault(base, 0)
        counts[base] = occurrence + 1
        base.copy(occurrence = occurrence)
    }
}

internal object HiddenConversationMessages {
    private val mutable = MutableStateFlow<Map<String, Set<HiddenMessageId>>>(emptyMap())
    val values: StateFlow<Map<String, Set<HiddenMessageId>>> = mutable
    fun hide(key: String, message: HiddenMessageId) {
        val current = mutable.value
        val bounded = if (key !in current && current.size >= 100) current - current.keys.first() else current
        mutable.value = bounded + (key to (bounded[key].orEmpty() + message).toList().takeLast(20).toSet())
    }
    fun remove(key: String) { mutable.value = mutable.value - key }
    fun clear() { mutable.value = emptyMap() }
}
