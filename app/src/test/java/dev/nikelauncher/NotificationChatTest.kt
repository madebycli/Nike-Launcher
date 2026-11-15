// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher

import dev.nikelauncher.media.PreviewMessage
import dev.nikelauncher.ui.notifications.groupMessages
import dev.nikelauncher.ui.notifications.GROUP_PAUSE_MILLIS
import org.junit.Assert.*
import org.junit.Test

class NotificationChatTest {
    @Test fun `consecutive messages share a stack until the author changes`() {
        val messages = listOf(PreviewMessage("Alex", "One", 1), PreviewMessage("Alex", "Two", 2),
            PreviewMessage("Me", "Three", 3, fromSelf = true))
        val groups = groupMessages(messages)
        assertEquals(listOf(2, 1), groups.map { it.messages.size })
        assertTrue(groups.first().stampAbove)
        assertFalse(groups.last().stampAbove)
        assertTrue(groups.last().fromSelf)
    }
    @Test fun `a pause adds a timestamp but missing provider times preserve order`() {
        val messages = listOf(PreviewMessage("Alex", "One", 1),
            PreviewMessage("Alex", "Two", GROUP_PAUSE_MILLIS + 2), PreviewMessage("Alex", "Three", 0))
        val groups = groupMessages(messages)
        assertEquals(listOf(1, 2), groups.map { it.messages.size })
        assertTrue(groups.last().stampAbove)
        assertEquals(messages, groups.flatMap { it.messages })
        assertTrue(groupMessages(emptyList()).isEmpty())
    }
}
