// SPDX-License-Identifier: GPL-3.0-or-later
// Adapted from AniSync (GPL-3.0-or-later), presentation/components/menu/Menu.kt.
package dev.nikelauncher.ui.kit.menu

import androidx.compose.ui.graphics.vector.ImageVector

/** Content of a [Menu]: items, dividers and gaps between groups. */
interface MenuScope {
    fun item(
        text: String,
        onClick: () -> Unit,
        leadingIcon: ImageVector? = null,
        trailingLabel: String? = null,
        supportingText: String? = null,
        destructive: Boolean = false,
        enabled: Boolean = true,
        selected: Boolean = false,
        maxLines: Int = Int.MAX_VALUE
    )

    fun divider()

    /** Splits subsequent items into a new visual group separated by 8dp of empty space. */
    fun gap()

    /**
     * Lets the current group scroll inside its own surface. It shows at most [maxVisibleItems] rows and
     * a sliver of the next, and fewer when the menu has less room, while the other groups stay in view.
     * One group per menu.
     */
    fun scrollable(maxVisibleItems: Int = Int.MAX_VALUE)
}

internal sealed interface MenuEntry {
    data class Item(
        val text: String,
        val onClick: () -> Unit,
        val leadingIcon: ImageVector?,
        val trailingLabel: String?,
        val supportingText: String?,
        val destructive: Boolean,
        val enabled: Boolean,
        val selected: Boolean,
        val maxLines: Int
    ) : MenuEntry

    data object Divider : MenuEntry
}

internal class ScrollingGroup(val entries: List<MenuEntry>, val maxVisibleItems: Int)

internal class MenuScopeImpl : MenuScope {
    private val groups = mutableListOf(mutableListOf<MenuEntry>())
    private var scrolling: ScrollingGroup? = null

    override fun item(
        text: String,
        onClick: () -> Unit,
        leadingIcon: ImageVector?,
        trailingLabel: String?,
        supportingText: String?,
        destructive: Boolean,
        enabled: Boolean,
        selected: Boolean,
        maxLines: Int
    ) {
        groups.last().add(
            MenuEntry.Item(
                text = text,
                onClick = onClick,
                leadingIcon = leadingIcon,
                trailingLabel = trailingLabel,
                supportingText = supportingText,
                destructive = destructive,
                enabled = enabled,
                selected = selected,
                maxLines = maxLines
            )
        )
    }

    override fun divider() {
        groups.last().add(MenuEntry.Divider)
    }

    override fun gap() {
        if (groups.last().isNotEmpty()) {
            groups.add(mutableListOf())
        }
    }

    override fun scrollable(maxVisibleItems: Int) {
        scrolling = ScrollingGroup(groups.last(), maxVisibleItems)
    }

    fun groups(): List<List<MenuEntry>> = groups.filter { it.isNotEmpty() }

    fun scrollingGroup(): ScrollingGroup? = scrolling?.takeIf { it.entries.isNotEmpty() }
}
