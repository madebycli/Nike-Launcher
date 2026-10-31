// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.service

import android.os.Build
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.annotation.RequiresApi

/** Reassert the user's hidden home state when the system returns control after keyguard. */
@RequiresApi(Build.VERSION_CODES.R)
internal class StatusBarControlWatcher(
    private val window: Window,
    private val shouldReassert: () -> Boolean,
    private val reassert: () -> Unit,
) {
    private var registered: WindowInsetsController? = null
    private var applying = false
    private val listener = WindowInsetsController.OnControllableInsetsChangedListener { _, types ->
        onControllableTypesChanged(types)
    }

    fun attach() {
        val current = window.insetsController ?: return
        if (registered === current) return
        detach()
        registered = current
        current.addOnControllableInsetsChangedListener(listener)
    }

    fun detach() {
        registered?.removeOnControllableInsetsChangedListener(listener)
        registered = null
    }

    internal fun onControllableTypesChanged(types: Int) {
        if (applying || types and WindowInsets.Type.statusBars() == 0 || !shouldReassert()) return
        applying = true
        try { reassert() } finally { applying = false }
    }
}
