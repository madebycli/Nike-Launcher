// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.settings

import dev.nikelauncher.ui.kit.settings.SettingsRow

/**
 * One row of settings, as data rather than as a call in a section body, so the same row serves
 * someone walking a section and someone typing in the search box.
 *
 * [keywords] is what else the row answers to: the words someone would reach for who does not
 * already know what this screen calls the thing.
 */
class SettingsEntry(
    val section: SettingsSection,
    val row: SettingsRow,
    val keywords: String = "",
    val visible: Boolean = true,
) {
    fun matches(query: String): Boolean {
        val q = query.trim()
        return row.title.contains(q, ignoreCase = true) || keywords.contains(q, ignoreCase = true)
    }
}
