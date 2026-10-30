// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.home.route

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.nikelauncher.ui.applist.ScrubState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** How long a launch is given to take us off screen before the overlay closes itself. */
private const val LAUNCH_CLOSE_TIMEOUT_MS = 2000L

/** How long a private space unlock has to put the system's lock screen up before giving up. */
private const val PRIVATE_UNLOCK_PROMPT_MS = 1500L

/**
 * The app list overlay's open/closed state and the ways it is opened and put away.
 *
 * The overlay stays composed while hidden, so what it holds (a query, a scroll position) is
 * cleared here on close; otherwise the next opening would still be filtering by the last one.
 */
@Stable
internal class AppListController(
    private val scope: CoroutineScope,
    private val scrub: ScrubState,
    val openAnim: Animatable<Float, *>,
    val openDistancePx: Float,
    val listState: LazyListState,
    private val flingDecay: DecayAnimationSpec<Float>,
    private val swipeOpensSearch: () -> Boolean,
) {
    var visible by mutableStateOf(false)
        private set

    /** The home screen fades back in when we put the list away ourselves, but not when the system did. */
    var snapHome by mutableStateOf(false)
        private set

    var query by mutableStateOf("")

    /** Bumped to ask the list for the cursor. A count because the same request can come twice. */
    var searchFocusTick by mutableIntStateOf(0)
        private set

    private var swipeScrolledList = false
    private var launchClose: Job? = null
    private var keepThroughPause = false

    /** Opened the whole way, as tapping the edge does. */
    fun openFully(focusSearch: Boolean = false) {
        visible = true
        scope.launch { openAnim.snapTo(openDistancePx) }
        if (focusSearch) {
            query = ""
            searchFocusTick++
        }
    }

    fun close(snap: Boolean = false) {
        query = ""
        scope.launch { openAnim.snapTo(0f) }
        launchClose?.cancel()
        launchClose = null
        snapHome = snap
        visible = false
        scrub.cancel()
    }

    /**
     * Closing the overlay the moment an app is launched would put the home screen on screen for the
     * few frames before that app's window arrives. Leave it up; it goes when we are backgrounded.
     * If nothing ever comes to the foreground, put it away the ordinary animated way.
     */
    fun closeAfterLaunch() {
        launchClose?.cancel()
        launchClose = scope.launch {
            delay(LAUNCH_CLOSE_TIMEOUT_MS)
            launchClose = null
            close()
        }
    }

    /** Keeps the list open through the pause the system's lock screen causes during a private unlock. */
    fun keepThroughNextPause() {
        keepThroughPause = true
        // A phone with no screen lock shows nothing, so no pause comes to use this up. Left
        // standing it would excuse the next real departure.
        scope.launch {
            delay(PRIVATE_UNLOCK_PROMPT_MS)
            keepThroughPause = false
        }
    }

    fun onPause() {
        if (keepThroughPause) keepThroughPause = false else close(snap = true)
    }

    fun onResume() {
        keepThroughPause = false
    }

    fun onSwipeUpDrag(total: Float, delta: Float) {
        visible = true
        scope.launch { openAnim.snapTo(total.coerceAtMost(openDistancePx)) }
        // Once it is all the way in the finger is usually still moving, so the rest of the drag
        // goes to the list rather than stopping dead against it.
        if (total > openDistancePx) {
            swipeScrolledList = true
            listState.dispatchRawDelta(delta)
        }
    }

    fun onSwipeUpEnd(velocity: Float) {
        scope.launch {
            val settled = openAnim.value > openDistancePx * 0.4f || velocity < -1200f
            if (settled) {
                openAnim.animateTo(
                    targetValue = openDistancePx,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
                )
                // Asked for once it has settled, not on the way: a keyboard arriving
                // mid-animation fights the list for the same space.
                if (swipeOpensSearch() && !swipeScrolledList) searchFocusTick++
            } else {
                openAnim.animateTo(0f, tween(160, easing = FastOutLinearInEasing))
                close()
            }
        }
        // The scrolling half of the gesture was fed the drag raw, which carries no momentum of its
        // own, so a flick that starts after the list is already moving has to be handed over.
        if (swipeScrolledList && velocity < 0f) {
            scope.launch {
                listState.scroll {
                    var travelled = 0f
                    AnimationState(initialValue = 0f, initialVelocity = -velocity).animateDecay(flingDecay) {
                        scrollBy(value - travelled)
                        travelled = value
                    }
                }
            }
        }
        swipeScrolledList = false
    }
}
