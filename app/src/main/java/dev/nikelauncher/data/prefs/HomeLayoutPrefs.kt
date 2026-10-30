// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import dev.nikelauncher.data.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class HomeLayoutPrefs internal constructor(private val store: DataStore<Preferences>) {
    val blockGapDp = store.data.map { (it[GAP] ?: 12).coerceIn(0, 64) }.distinctUntilChanged()
    val mediaSidePaddingDp = store.data.map { (it[MEDIA_EDGE] ?: it[PrefKeys.SIDE_PADDING_DP] ?: 20).coerceIn(0, 120) }.distinctUntilChanged()
    suspend fun setGap(dp: Int) { store.edit { it[GAP] = dp.coerceIn(0, 64) } }
    suspend fun setPins(pins: Map<HomeBlock, BlockPin?>) {
        store.edit { pref -> pins.forEach { (block, pin) ->
            val key = when (block) { HomeBlock.WIDGET -> PrefKeys.HOME_PIN_WIDGET; HomeBlock.NOW_PLAYING -> PrefKeys.HOME_PIN_NOW_PLAYING; HomeBlock.FAVORITES -> PrefKeys.HOME_PIN_FAVORITES }
            if (pin == null) pref.remove(key) else pref[key] = pin.encode()
        } }
    }
    suspend fun setEdges(blocks: Set<HomeBlock>, dp: Int? = null, delta: Int = 0) {
        store.edit { pref ->
            // Preserve inherited values for unselected fields before changing the shared fallback.
            val previous = pref[PrefKeys.SIDE_PADDING_DP] ?: 20
            if (HomeBlock.FAVORITES in blocks) {
                if (HomeBlock.WIDGET !in blocks && pref[PrefKeys.WIDGET_SIDE_PADDING_DP] == null) pref[PrefKeys.WIDGET_SIDE_PADDING_DP] = previous
                if (HomeBlock.NOW_PLAYING !in blocks && pref[MEDIA_EDGE] == null) pref[MEDIA_EDGE] = previous
            }
            blocks.forEach { block ->
            val key = when (block) { HomeBlock.WIDGET -> PrefKeys.WIDGET_SIDE_PADDING_DP; HomeBlock.NOW_PLAYING -> MEDIA_EDGE; HomeBlock.FAVORITES -> PrefKeys.SIDE_PADDING_DP }
            pref[key] = (dp ?: ((pref[key] ?: pref[PrefKeys.SIDE_PADDING_DP] ?: 20) + delta)).coerceIn(0, 120)
        } }
    }
    private companion object {
        val GAP = intPreferencesKey("home_block_gap_dp")
        val MEDIA_EDGE = intPreferencesKey("home_media_side_padding_dp")
    }
}
