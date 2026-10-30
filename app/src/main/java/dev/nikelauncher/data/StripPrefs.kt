// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

enum class EdgeSide { LEFT, RIGHT, BOTH }

/** How the A-Z strip moves under a fingertip. */
enum class ScrubStyle { BELL, RIPPLE }

/** The bubble that shows the letter under a finger on the A-Z strip, or none at all. */
enum class ScrubBubbleShape { ROUNDED, CIRCLE, SQUARE, HIDDEN }

/**
 * Tunables for the ripple strip's wave simulation.
 *
 * @property speed how fast ripples travel along the strip, in letters per second
 * @property damping how quickly they die out (1/s); low values ring for longer
 * @property stiffness how hard the strip is pulled toward the bell under the finger; low
 *   values lag and wobble, high values track tightly
 */
data class RippleParams(
    val speed: Float = 30f,
    val damping: Float = 20f,
    val stiffness: Float = 600f,
) {
    companion object {
        val SPEED_RANGE = 4f..60f
        val DAMPING_RANGE = 0.5f..40f
        val STIFFNESS_RANGE = 5f..1200f
    }
}

/** Rows a second a held letter walks the list at. */
val HOLD_SCROLL_RANGE = 1..12

/** Past about this the strip is being pushed off the edge it belongs to. */
val EDGE_ZONE_INSET_RANGE = 0..32

/** When the A-Z strip shows on the home screen itself. */
enum class AzStripVisibility { NEVER, LANDSCAPE, ALWAYS }

class StripPrefs internal constructor(private val store: DataStore<Preferences>) {

    private val data get() = store.data

    val hapticsEnabled: Flow<Boolean> = data.map { it[PrefKeys.HAPTICS_ENABLED] ?: true }.distinctUntilChanged()

    val edgeSide: Flow<EdgeSide> = data.map {
        runCatching { EdgeSide.valueOf(it[PrefKeys.EDGE_SIDE] ?: EdgeSide.RIGHT.name) }.getOrDefault(EdgeSide.RIGHT)
    }.distinctUntilChanged()

    val scrubStyle: Flow<ScrubStyle> = data.map {
        runCatching { ScrubStyle.valueOf(it[PrefKeys.SCRUB_STYLE] ?: ScrubStyle.BELL.name) }.getOrDefault(ScrubStyle.BELL)
    }.distinctUntilChanged()

    val scrubBubbleShape: Flow<ScrubBubbleShape> = data.map {
        runCatching { ScrubBubbleShape.valueOf(it[PrefKeys.SCRUB_BUBBLE_SHAPE] ?: ScrubBubbleShape.ROUNDED.name) }
            .getOrDefault(ScrubBubbleShape.ROUNDED)
    }.distinctUntilChanged()

    val rippleParams: Flow<RippleParams> = data.map {
        val d = RippleParams()
        RippleParams(
            speed = (it[PrefKeys.RIPPLE_SPEED] ?: d.speed).coerceIn(RippleParams.SPEED_RANGE),
            damping = (it[PrefKeys.RIPPLE_DAMPING] ?: d.damping).coerceIn(RippleParams.DAMPING_RANGE),
            stiffness = (it[PrefKeys.RIPPLE_STIFFNESS] ?: d.stiffness).coerceIn(RippleParams.STIFFNESS_RANGE),
        )
    }.distinctUntilChanged()

    /** Keep the A-Z strip on screen even when the app list is closed. */
    /**
     * When the strip sits on the home screen with no app list open.
     *
     * Reads the old on/off answer when this has not been set, so nobody's strip appears or
     * disappears on update. LANDSCAPE is for a phone in a car mount, where the strip earns
     * the room it takes and upright it does not.
     */
    val azStripVisibility: Flow<AzStripVisibility> = data.map { pref ->
        pref[PrefKeys.AZ_STRIP_VISIBILITY]?.let { runCatching { AzStripVisibility.valueOf(it) }.getOrNull() }
            ?: if (pref[PrefKeys.ALWAYS_SHOW_AZ] == true) AzStripVisibility.ALWAYS else AzStripVisibility.NEVER
    }.distinctUntilChanged()

    /** The A-Z strip inside the app list; the edge gesture still works without it. */
    val showAlphabet: Flow<Boolean> = data.map { it[PrefKeys.SHOW_ALPHABET] ?: true }.distinctUntilChanged()

    /** Width of the invisible strip at each screen edge that opens the app list. */
    val edgeZoneWidthDp: Flow<Int> = data.map { it[PrefKeys.EDGE_ZONE_WIDTH_DP] ?: 56 }.distinctUntilChanged()

    /**
     * How much of the very edge is left to the system rather than taken by the strip.
     *
     * Android's back gesture starts at the screen edge and so does this strip, so on a phone
     * with gesture navigation the two are reaching for the same pixels. Giving the outermost
     * few back means a swipe that starts right at the edge goes back and one that starts just
     * inside it scrubs.
     *
     * Zero by default: on a phone with buttons there is nothing to share with, and anyone the
     * conflict bothers can say so.
     */
    /**
     * Whether the room left for the back gesture follows the system's own gesture area.
     *
     * Android says exactly how far in from each edge its back gesture reaches, and says zero
     * when navigation is by buttons — so following it gives a phone with gesture navigation
     * the room it needs and changes nothing for one without. On unless someone has already
     * set the gap by hand, in which case their number stands.
     */
    val edgeZoneInsetAuto: Flow<Boolean> =
        data.map { it[PrefKeys.EDGE_ZONE_INSET_AUTO] ?: (it[PrefKeys.EDGE_ZONE_INSET_DP] == null) }
            .distinctUntilChanged()

    val edgeZoneInsetDp: Flow<Int> =
        data.map { (it[PrefKeys.EDGE_ZONE_INSET_DP] ?: 0).coerceIn(EDGE_ZONE_INSET_RANGE) }
            .distinctUntilChanged()

    /**
     * Whether the edge that opens the app list is only as tall as the letters it shows.
     *
     * Off by default. The whole edge has always been live, and quietly taking that away is a
     * louder complaint than the one it answers — but on a home screen with a widget in reach of
     * the edge, a strip down the whole side is in the way of the widget.
     */
    val edgeZoneBandOnly: Flow<Boolean> =
        data.map { it[PrefKeys.EDGE_ZONE_BAND_ONLY] ?: false }.distinctUntilChanged()

    /**
     * Whether the rest of the list fades while a finger travels the alphabet.
     *
     * On, as it has always been — the fade is what makes the letter you are on findable. Off
     * for anyone who would rather keep sight of what is around it.
     */
    val dimOthersScrubbing: Flow<Boolean> =
        data.map { it[PrefKeys.DIM_OTHERS_SCRUBBING] ?: true }.distinctUntilChanged()

    /** Whether resting a finger on a letter walks the list on through that letter's apps. */
    val holdScroll: Flow<Boolean> =
        data.map { it[PrefKeys.HOLD_SCROLL] ?: false }.distinctUntilChanged()

    /** Rows a second while it does. */
    val holdScrollSpeed: Flow<Int> =
        data.map { (it[PrefKeys.HOLD_SCROLL_SPEED] ?: 4).coerceIn(HOLD_SCROLL_RANGE) }
            .distinctUntilChanged()

    /** Null while the A-Z strip follows the favorites; a set range is a fraction of the viewport. */
    val scrubBand: Flow<Pair<Float, Float>?> = data.map { pref ->
        val top = pref[PrefKeys.AZ_BAND_TOP_FRACTION]
        val height = pref[PrefKeys.AZ_BAND_HEIGHT_FRACTION]
        if (top != null && height != null) top to height else null
    }.distinctUntilChanged()

    suspend fun setHapticsEnabled(v: Boolean) {
        store.edit { it[PrefKeys.HAPTICS_ENABLED] = v }
    }

    suspend fun setShowAlphabet(v: Boolean) {
        store.edit { it[PrefKeys.SHOW_ALPHABET] = v }
    }

    suspend fun setAzStripVisibility(v: AzStripVisibility) {
        store.edit {
            it[PrefKeys.AZ_STRIP_VISIBILITY] = v.name
            // Kept in step so an older build reads something sensible if one is installed.
            it[PrefKeys.ALWAYS_SHOW_AZ] = v == AzStripVisibility.ALWAYS
        }
    }

    suspend fun setEdgeSide(v: EdgeSide) {
        store.edit { it[PrefKeys.EDGE_SIDE] = v.name }
    }

    suspend fun setRippleParams(v: RippleParams) {
        store.edit {
            it[PrefKeys.RIPPLE_SPEED] = v.speed
            it[PrefKeys.RIPPLE_DAMPING] = v.damping
            it[PrefKeys.RIPPLE_STIFFNESS] = v.stiffness
        }
    }

    suspend fun setScrubStyle(v: ScrubStyle) {
        store.edit { it[PrefKeys.SCRUB_STYLE] = v.name }
    }

    suspend fun setScrubBubbleShape(v: ScrubBubbleShape) {
        store.edit { it[PrefKeys.SCRUB_BUBBLE_SHAPE] = v.name }
    }

    suspend fun setEdgeZoneWidthDp(v: Int) {
        store.edit { it[PrefKeys.EDGE_ZONE_WIDTH_DP] = v }
    }

    suspend fun setEdgeZoneInsetAuto(v: Boolean) {
        store.edit { it[PrefKeys.EDGE_ZONE_INSET_AUTO] = v }
    }

    suspend fun setEdgeZoneInsetDp(v: Int) {
        store.edit { it[PrefKeys.EDGE_ZONE_INSET_DP] = v.coerceIn(EDGE_ZONE_INSET_RANGE) }
    }

    suspend fun setDimOthersScrubbing(v: Boolean) {
        store.edit { it[PrefKeys.DIM_OTHERS_SCRUBBING] = v }
    }

    suspend fun setHoldScroll(v: Boolean) {
        store.edit { it[PrefKeys.HOLD_SCROLL] = v }
    }

    suspend fun setHoldScrollSpeed(v: Int) {
        store.edit { it[PrefKeys.HOLD_SCROLL_SPEED] = v.coerceIn(HOLD_SCROLL_RANGE) }
    }

    suspend fun setEdgeZoneBandOnly(v: Boolean) {
        store.edit { it[PrefKeys.EDGE_ZONE_BAND_ONLY] = v }
    }

    suspend fun setScrubBand(topFraction: Float, heightFraction: Float) {
        store.edit {
            it[PrefKeys.AZ_BAND_TOP_FRACTION] = topFraction
            it[PrefKeys.AZ_BAND_HEIGHT_FRACTION] = heightFraction
        }
    }

    /** Hands the strip back to following the favorites list. */
    suspend fun clearScrubBand() {
        store.edit {
            it.remove(PrefKeys.AZ_BAND_TOP_FRACTION)
            it.remove(PrefKeys.AZ_BAND_HEIGHT_FRACTION)
        }
    }
}
