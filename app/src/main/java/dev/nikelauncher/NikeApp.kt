// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher

import android.app.Application
import dev.nikelauncher.data.AppRepository
import dev.nikelauncher.data.IconPackRepository
import dev.nikelauncher.data.CrashLog
import dev.nikelauncher.data.Prefs
import dev.nikelauncher.widget.ClockWidgetBattery
import dev.nikelauncher.widget.NikeAppWidgetHost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class NikeApp : Application() {

    lateinit var prefs: Prefs
        private set
    lateinit var appRepository: AppRepository
        private set
    lateinit var iconPackRepository: IconPackRepository
        private set
    lateinit var widgetHost: NikeAppWidgetHost
        private set

    /** Outlives any screen, for work that must finish even as the launcher is left behind. */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // First, so a crash on the way up is still recorded.
        CrashLog.install(this)
        prefs = Prefs(this)
        appRepository = AppRepository(this, prefs, appScope)
        iconPackRepository = IconPackRepository(this)
        widgetHost = NikeAppWidgetHost(this, HOST_ID)
        // Nothing else keeps the clock widget's battery line current; see the object.
        ClockWidgetBattery.watch(this)
    }

    companion object {
        const val HOST_ID = 1024
    }
}