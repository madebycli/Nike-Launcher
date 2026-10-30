// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.common

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes

/**
 * Short toast with a string resource, resolved when it is shown rather than captured earlier,
 * so a configuration change between composing and tapping cannot leave a stale locale behind.
 */
fun Context.toast(@StringRes message: Int) {
    Toast.makeText(this, getString(message), Toast.LENGTH_SHORT).show()
}
