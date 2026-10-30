// SPDX-License-Identifier: GPL-3.0-or-later
// Adapted from AniSync (GPL-3.0-or-later), presentation/util/MotionConstants.kt.
package dev.nikelauncher.ui.kit.motion

/**
 * The few motion values the theme's motion scheme does not provide. Reach for [AppMotion] first.
 */
object MotionConstants {
    /** Delay between the items of a cascading entrance. */
    const val STAGGER_DELAY_MS = 40

    /** Scale of a pressed surface: a small shrink, not a gesture of its own. */
    const val PRESSED_SCALE = 0.95f
}
