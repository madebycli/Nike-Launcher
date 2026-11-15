// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.data

/** What a press, or a swipe up, on the corner button does. */
enum class CornerAction {
    MENU,
    SEARCH,
    APP,
    SHORTCUTS,
    CALENDAR,
    WEATHER,
    NOTIFICATIONS,
    SETTINGS,
    ASSISTANT,
    SHADE,
    LOCK,
}

enum class CornerShape { CIRCLE, SQUIRCLE, OUTLINE, MINIMAL }

enum class CornerColorMode { CLASSIC, MATERIAL, CUSTOM }

/** The icon can follow the action or keep the launcher's own mark whatever the action is. */
enum class CornerIcon { ACTION, BADGE }

data class CornerAppearance(
    val colorMode: CornerColorMode = CornerColorMode.CLASSIC,
    val background: Int = 0xFFFFFFFF.toInt(),
    val foreground: Int = 0xFF153E49.toInt(),
    val automaticForeground: Boolean = true,
    val backgroundOpacity: Int = 100,
    val icon: CornerIcon = CornerIcon.ACTION,
)

/** Everything the corner button is set to, read as one so it never draws half of a change. */
data class CornerSettings(
    val enabled: Boolean,
    val tap: CornerAction,
    val swipe: CornerAction,
    val shape: CornerShape,
    val sizeDp: Int,
    val appearance: CornerAppearance,
    val position: CornerPosition? = null,
)

val CORNER_SIZE_RANGE = 48..72
