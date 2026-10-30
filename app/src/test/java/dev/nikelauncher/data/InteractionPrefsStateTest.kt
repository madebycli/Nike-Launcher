// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.data

import androidx.test.core.app.ApplicationProvider
import dev.nikelauncher.NikeApp
import dev.nikelauncher.data.prefs.SearchDisplayMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class InteractionPrefsStateTest {
    @Test fun `search choice is exclusive and survives new preference owners without renaming legacy keys`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<NikeApp>()
        val prefs = context.prefs
        prefs.replaceBackupValues(emptyMap())
        assertEquals(ShortcutSwipe.OFF, prefs.home.shortcutSwipe.first())
        prefs.replaceBackupValues(mapOf("swipe_for_shortcuts" to true))
        assertEquals(ShortcutSwipe.RIGHT, prefs.home.shortcutSwipe.first())
        SearchDisplayMode.entries.forEach { mode ->
            prefs.search.setMode(mode)
            assertEquals(mode, Prefs(context).search.mode.first())
            assertEquals(mode == SearchDisplayMode.BAR, prefs.appList.appListSearchEnabled.first())
            assertEquals(mode == SearchDisplayMode.BUTTON, prefs.corner.floatingSearch.first())
        }
        prefs.setup.setStep(1)
        assertEquals(1, Prefs(context).setup.step.first())
        prefs.search.setButtonAtBottom(false)
        prefs.corner.setPosition(CornerPosition(.25f, .75f))
        assertFalse(Prefs(context).search.buttonAtBottom.first())
        assertEquals(CornerPosition(.25f, .75f), Prefs(context).corner.settings.first().position)
        prefs.corner.setEnabled(false)
        assertFalse(prefs.corner.settings.first().enabled)
        prefs.corner.setEnabled(true)
        assertTrue(prefs.corner.settings.first().enabled)
        prefs.corner.setPosition(null)
        assertNull(prefs.corner.settings.first().position)
    }
}
