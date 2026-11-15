// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.media

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationSnapshotRefreshTest {
    @Test fun retriesWhenAnEventInvalidatesTheInitialSnapshot() = runBlocking {
        var revision = 0L
        var reads = 0
        var published: List<String> = emptyList()
        refreshNotificationSnapshot(
            canRefresh = { true }, revision = { revision },
            read = { reads++; if (reads == 1) { revision++; listOf("old") } else listOf("new", "existing") },
            publish = { expected, snapshot -> if (expected != revision) false else { published = snapshot; true } },
        )
        assertEquals(2, reads)
        assertEquals(listOf("new", "existing"), published)
    }

    @Test fun stopsRetryingAfterPrivacyOrPreferenceChanges() = runBlocking {
        var enabled = true
        var reads = 0
        refreshNotificationSnapshot(
            canRefresh = { enabled }, revision = { 0L },
            read = { reads++; enabled = false; "snapshot" },
            publish = { _, _ -> false },
        )
        assertEquals(1, reads)
    }
}
