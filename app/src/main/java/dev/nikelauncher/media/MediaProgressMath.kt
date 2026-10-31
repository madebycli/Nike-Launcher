// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.media

/** Null means that a provider has not published a usable duration or position. */
internal fun mediaProgress(position: Long, duration: Long, elapsed: Long, speed: Float): Float? {
    if (position < 0 || duration <= 0 || !speed.isFinite()) return null
    return ((position.toDouble() + elapsed.coerceAtLeast(0) * speed.toDouble()) / duration)
        .coerceIn(0.0, 1.0).toFloat()
}
