// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.kit.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/** The rows a [SettingsGroup] is made of. Each call adds one row, in order. */
interface SettingsGroupScope {

    /** A row that does something when tapped. [value] is the current choice, shown under the title. */
    fun item(
        title: String,
        onClick: () -> Unit,
        icon: ImageVector? = null,
        value: String? = null,
        enabled: Boolean = true,
        trailing: (@Composable () -> Unit)? = null,
    )

    /** A row that flips a boolean; tapping anywhere on it toggles the switch. */
    fun switchItem(
        title: String,
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
        icon: ImageVector? = null,
        value: String? = null,
        enabled: Boolean = true,
    )

    /** A row that holds its own control, such as a slider, and is not tappable as a whole. */
    fun block(
        title: String,
        icon: ImageVector? = null,
        value: String? = null,
        control: @Composable () -> Unit,
    )
}

internal sealed interface SettingsRow {
    data class Item(
        val title: String,
        val onClick: () -> Unit,
        val icon: ImageVector?,
        val value: String?,
        val enabled: Boolean,
        val trailing: (@Composable () -> Unit)?,
    ) : SettingsRow

    data class Switch(
        val title: String,
        val checked: Boolean,
        val onCheckedChange: (Boolean) -> Unit,
        val icon: ImageVector?,
        val value: String?,
        val enabled: Boolean,
    ) : SettingsRow

    data class Block(
        val title: String,
        val icon: ImageVector?,
        val value: String?,
        val control: @Composable () -> Unit,
    ) : SettingsRow
}

internal class SettingsGroupScopeImpl : SettingsGroupScope {
    val rows = mutableListOf<SettingsRow>()

    override fun item(
        title: String,
        onClick: () -> Unit,
        icon: ImageVector?,
        value: String?,
        enabled: Boolean,
        trailing: (@Composable () -> Unit)?,
    ) {
        rows += SettingsRow.Item(title, onClick, icon, value, enabled, trailing)
    }

    override fun switchItem(
        title: String,
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
        icon: ImageVector?,
        value: String?,
        enabled: Boolean,
    ) {
        rows += SettingsRow.Switch(title, checked, onCheckedChange, icon, value, enabled)
    }

    override fun block(
        title: String,
        icon: ImageVector?,
        value: String?,
        control: @Composable () -> Unit,
    ) {
        rows += SettingsRow.Block(title, icon, value, control)
    }
}
