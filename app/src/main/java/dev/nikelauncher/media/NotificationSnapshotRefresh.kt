// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.media

/** Re-read a snapshot if a live event made the previous read obsolete. */
internal suspend fun <T> refreshNotificationSnapshot(
    canRefresh: () -> Boolean,
    revision: () -> Long,
    read: suspend () -> T?,
    publish: (Long, T) -> Boolean,
) {
    while (canRefresh()) {
        val expected = revision()
        val snapshot = read() ?: return
        if (publish(expected, snapshot)) return
    }
}
