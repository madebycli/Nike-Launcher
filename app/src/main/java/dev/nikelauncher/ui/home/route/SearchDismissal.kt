// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.route

/** Temporary search closes only after a seen keyboard closes with no query. */
internal fun dismissEmptySearch(keyboardSeen: Boolean, keyboardVisible: Boolean, query: String): Boolean =
    keyboardSeen && !keyboardVisible && query.isBlank()
