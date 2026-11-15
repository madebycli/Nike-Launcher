// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Progress survives a new HOME task and process death while Android changes the home role. */
class SetupPrefs internal constructor(private val store: DataStore<Preferences>) {
    val step = store.data.map { setupStep(it[STEP] ?: 0) }.distinctUntilChanged()
    suspend fun setStep(value: Int) { store.edit { it[STEP] = setupStep(value) } }
    private companion object { val STEP = intPreferencesKey("setup_step") }
}

internal fun setupStep(value: Int): Int = value.coerceIn(0, 4)
