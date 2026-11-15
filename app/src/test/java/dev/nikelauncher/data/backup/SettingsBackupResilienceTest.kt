// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.data.backup

import androidx.test.core.app.ApplicationProvider
import dev.nikelauncher.NikeApp
import java.io.File
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A backup still goes through when one of the pictures it points at can no longer be read. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SettingsBackupResilienceTest {
    @Test fun `an unreadable icon is left out and everything else is kept`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<NikeApp>()
        val prefs = app.prefs
        val readable = File(app.filesDir, "icon.png").apply {
            // Smallest valid PNG: a 1x1 transparent pixel.
            writeBytes(android.util.Base64.decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==", 0))
        }
        val icons = JSONObject()
            .put("fake.ok/.Main", android.net.Uri.fromFile(readable).toString())
            .put("fake.gone/.Main", "content://fake.provider.that.is.gone/icon/1")
        prefs.replaceBackupValues(mapOf("icon_overrides_json" to icons.toString(), "icon_size_dp" to 61))

        val backup = prefs.exportJson(true).json
        prefs.replaceBackupValues(emptyMap())
        assertNotNull(prefs.importJson(backup))

        val restored = prefs.storedValues()
        assertEquals(61, restored["icon_size_dp"])
        val restoredIcons = JSONObject(restored["icon_overrides_json"] as String)
        assertTrue(restoredIcons.has("fake.ok/.Main"))
        assertFalse(restoredIcons.has("fake.gone/.Main"))
    }

    // The settings store lives for the whole test process; leave it empty for the next class.
    @After fun clear() = runBlocking {
        ApplicationProvider.getApplicationContext<NikeApp>().prefs.replaceBackupValues(emptyMap())
    }
}
