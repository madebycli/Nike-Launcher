// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.kit.motion

import android.animation.ValueAnimator

/** False when Android has switched animator motion off; staggered entrances then do not wait. */
fun motionEnabled(): Boolean = ValueAnimator.areAnimatorsEnabled()
