// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.settings

/** Move a single row, keeping all other keys and their relative order. */
internal fun <T> moveRow(rows: List<T>, from: Int, to: Int): List<T> =
    if (from !in rows.indices || to !in rows.indices || from == to) rows
    else rows.toMutableList().apply { add(to, removeAt(from)) }
