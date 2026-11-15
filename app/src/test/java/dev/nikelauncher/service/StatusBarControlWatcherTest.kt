// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.service

import android.app.Activity
import android.view.WindowInsets
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class StatusBarControlWatcherTest {
    @Test fun onlyRestoresHiddenStateWhenStatusBarBecomesControllable() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        var wanted = true
        var requests = 0
        val watcher = StatusBarControlWatcher(activity.window, { wanted }, { requests++ })
        watcher.onControllableTypesChanged(WindowInsets.Type.navigationBars())
        assertEquals(0, requests)
        watcher.onControllableTypesChanged(WindowInsets.Type.statusBars())
        assertEquals(1, requests)
        wanted = false
        watcher.onControllableTypesChanged(WindowInsets.Type.statusBars())
        assertEquals(1, requests)
    }

    @Test fun ignoresReentrantCallbacksDuringRestore() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        var requests = 0
        lateinit var watcher: StatusBarControlWatcher
        watcher = StatusBarControlWatcher(activity.window, { true }, {
            requests++
            watcher.onControllableTypesChanged(WindowInsets.Type.statusBars())
        })
        watcher.onControllableTypesChanged(WindowInsets.Type.statusBars())
        assertEquals(1, requests)
    }
}
