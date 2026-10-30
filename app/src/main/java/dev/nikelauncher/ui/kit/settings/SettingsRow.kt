// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.kit.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * One row of a [SettingsGroup], as data. Rows are built by the factory functions below so a
 * screen can hold them in a list (to filter by a search query, say) before anything is drawn.
 */
sealed interface SettingsRow {
    val title: String

    class Item(
        override val title: String,
        val onClick: () -> Unit,
        val icon: ImageVector?,
        val value: String?,
        val enabled: Boolean,
        val trailing: (@Composable () -> Unit)?,
    ) : SettingsRow

    class Switch(
        override val title: String,
        val checked: Boolean,
        val onCheckedChange: (Boolean) -> Unit,
        val icon: ImageVector?,
        val value: String?,
        val enabled: Boolean,
    ) : SettingsRow

    class Radio(
        override val title: String,
        val selected: Boolean,
        val onClick: () -> Unit,
        val value: String?,
    ) : SettingsRow

    class Block(
        override val title: String,
        val icon: ImageVector?,
        val value: String?,
        val control: @Composable () -> Unit,
    ) : SettingsRow
}

/** A row that does something when tapped. [value] is the current choice, shown under the title. */
fun settingsItem(
    title: String,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    value: String? = null,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
): SettingsRow = SettingsRow.Item(title, onClick, icon, value, enabled, trailing)

/** A row that flips a boolean; tapping anywhere on it toggles the switch. */
fun settingsSwitch(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector? = null,
    value: String? = null,
    enabled: Boolean = true,
): SettingsRow = SettingsRow.Switch(title, checked, onCheckedChange, icon, value, enabled)

/** [settingsSwitch] with the change handler last, so it can be written as a trailing lambda. */
fun settingsSwitch(
    title: String,
    checked: Boolean,
    icon: ImageVector? = null,
    value: String? = null,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
): SettingsRow = SettingsRow.Switch(title, checked, onCheckedChange, icon, value, enabled)

/** A row that is one option of a single choice. */
fun settingsRadio(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    value: String? = null,
): SettingsRow = SettingsRow.Radio(title, selected, onClick, value)

/** A row that holds its own control, such as a slider or a set of chips; it is not tappable as a whole. */
fun settingsBlock(
    title: String,
    icon: ImageVector? = null,
    value: String? = null,
    control: @Composable () -> Unit,
): SettingsRow = SettingsRow.Block(title, icon, value, control)

/** Adds the row built by [factory] to the group being written. */
fun SettingsRowCollector.item(
    title: String,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    value: String? = null,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) = add(settingsItem(title, onClick, icon, value, enabled, trailing))

fun SettingsRowCollector.switchItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector? = null,
    value: String? = null,
    enabled: Boolean = true,
) = add(settingsSwitch(title, checked, onCheckedChange, icon, value, enabled))

fun SettingsRowCollector.radioItem(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    value: String? = null,
) = add(settingsRadio(title, selected, onClick, value))

fun SettingsRowCollector.block(
    title: String,
    icon: ImageVector? = null,
    value: String? = null,
    control: @Composable () -> Unit,
) = add(settingsBlock(title, icon, value, control))
