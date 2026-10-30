// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [Prefs.library.forgetEntry] and [Prefs.library.removeFavorite] against the real DataStore, which is the only
 * way to exercise either: both operate on `Context.dataStore`, and this module has no
 * Robolectric, so a JVM test cannot construct a working [Prefs] at all.
 *
 * Regression coverage for CR-1 (Manage Favorites unticking a row must not wipe its rename,
 * icon, folder, hidden flag or launch count — only [Prefs.library.forgetEntry] may do that, and only
 * for a row that is gone for good) and CR-5 (forgetting a row must also clear any quick-launch
 * slot pointing at it, in the same transaction).
 */
@RunWith(AndroidJUnit4::class)
class PrefsForgetTest {

    private val prefs: Prefs
        get() = Prefs(InstrumentationRegistry.getInstrumentation().targetContext.applicationContext)

    // Keys that cannot collide with anything actually installed on the test device.
    private val keyA = "zz.pinned.shortcuts.test.cr1/FakeActivityA"
    private val keyB = "zz.pinned.shortcuts.test.cr5/FakeActivityB"
    private val keyOther = "zz.pinned.shortcuts.test.cr5/UnrelatedActivity"

    @After
    fun tearDown() = runBlocking {
        // forgetEntry wipes everything this test could have left behind under each key,
        // including the quick-launch slots, so it doubles as the cleanup.
        prefs.library.forgetEntry(keyA)
        prefs.library.forgetEntry(keyB)
        prefs.library.forgetEntry(keyOther)
    }

    @Test
    fun untickingAFavoriteLeavesEverythingElseWaitingForWhenItComesBack() = runBlocking {
        prefs.library.addFavorite(keyA)
        prefs.library.setNameOverride(keyA, "Renamed row")
        prefs.library.setIconOverride(keyA, "content://example/icon")
        prefs.library.setHidden(keyA, true)
        prefs.library.incrementLaunchCount(keyA)

        // This is the exact call ManageFavoritesScreen's untick now makes (CR-1), not forgetEntry.
        prefs.library.removeFavorite(keyA)

        assertFalse("expected the favorite itself to be gone", keyA in prefs.library.favorites.first())
        assertEquals(
            "a locked profile or paused storage can make a row disappear temporarily; its " +
                "rename must still be there when it comes back",
            "Renamed row",
            prefs.library.nameOverrides.first()[keyA],
        )
        assertEquals("content://example/icon", prefs.library.iconOverrides.first()[keyA])
        assertTrue("the hidden flag must survive an untick too", keyA in prefs.library.hiddenApps.first())
        assertEquals(1, prefs.library.launchCounts.first()[keyA])
    }

    @Test
    fun forgettingAnEntryClearsQuickLaunchSlotsThatPointToIt() = runBlocking {
        prefs.library.setQuickLaunch(QuickLaunchSlot.LEFT, keyB)
        prefs.library.setQuickLaunch(QuickLaunchSlot.RIGHT, keyB)

        prefs.library.forgetEntry(keyB)

        assertNull("a swipe must not still point at a row that is gone for good", prefs.library.quickLaunchLeft.first())
        assertNull(prefs.library.quickLaunchRight.first())
    }

    @Test
    fun forgettingAnEntryLeavesAnUnrelatedQuickLaunchSlotAlone() = runBlocking {
        prefs.library.setQuickLaunch(QuickLaunchSlot.LEFT, keyOther)

        prefs.library.forgetEntry(keyB)

        assertEquals(keyOther, prefs.library.quickLaunchLeft.first())
        prefs.library.setQuickLaunch(QuickLaunchSlot.LEFT, null)
    }
}
