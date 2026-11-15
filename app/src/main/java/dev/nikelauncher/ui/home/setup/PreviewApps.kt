// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.setup

/** Fill the compact preview from selected apps, then installed apps, without duplicate entries. */
internal fun previewAppKeys(selected: List<String>, installed: List<String>, count: Int = 2): List<String> =
    (selected.filter { it in installed } + installed).distinct().take(count)
