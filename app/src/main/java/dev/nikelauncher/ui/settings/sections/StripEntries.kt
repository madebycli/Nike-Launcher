// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.settings.sections

import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import dev.nikelauncher.R
import dev.nikelauncher.data.AzStripVisibility
import dev.nikelauncher.data.EDGE_ZONE_INSET_RANGE
import dev.nikelauncher.data.EdgeSide
import dev.nikelauncher.data.HOLD_SCROLL_RANGE
import dev.nikelauncher.data.RippleParams
import dev.nikelauncher.data.ScrubBubbleShape
import dev.nikelauncher.data.ScrubStyle
import dev.nikelauncher.ui.kit.settings.ChoiceChips
import dev.nikelauncher.ui.kit.settings.SettingsSlider
import dev.nikelauncher.ui.kit.settings.settingsBlock
import dev.nikelauncher.ui.kit.settings.settingsSwitch
import dev.nikelauncher.ui.settings.SettingsEntry
import dev.nikelauncher.ui.settings.SettingsEnv
import dev.nikelauncher.ui.settings.SettingsSection
import dev.nikelauncher.ui.settings.labelRes
import kotlin.math.roundToInt

private val SECTION = SettingsSection.STRIP

@Composable
internal fun stripEntries(env: SettingsEnv): List<SettingsEntry> {
    val strip = env.prefs.strip
    val visibility by strip.azStripVisibility.collectAsState(AzStripVisibility.NEVER)
    val showAlphabet by strip.showAlphabet.collectAsState(true)
    val side by strip.edgeSide.collectAsState(EdgeSide.RIGHT)
    val style by strip.scrubStyle.collectAsState(ScrubStyle.BELL)
    val bubble by strip.scrubBubbleShape.collectAsState(ScrubBubbleShape.ROUNDED)
    val ripple by strip.rippleParams.collectAsState(RippleParams())
    val dimOthers by strip.dimOthersScrubbing.collectAsState(true)
    val holdScroll by strip.holdScroll.collectAsState(false)
    val holdSpeed by strip.holdScrollSpeed.collectAsState(4)
    val bandOnly by strip.edgeZoneBandOnly.collectAsState(false)
    val insetAuto by strip.edgeZoneInsetAuto.collectAsState(true)
    val inset by strip.edgeZoneInsetDp.collectAsState(0)
    val width by strip.edgeZoneWidthDp.collectAsState(56)

    val isRipple = style == ScrubStyle.RIPPLE
    return listOf(
        SettingsEntry(SECTION, settingsBlock(stringResource(R.string.settings_az_visibility)) {
            ChoiceChips(AzStripVisibility.entries, visibility, { stringResource(it.labelRes()) }, { env.write { strip.setAzStripVisibility(it) } })
        }, "alphabet strip a-z"),
        SettingsEntry(SECTION, settingsSwitch(stringResource(R.string.settings_show_alphabet), showAlphabet) { env.write { strip.setShowAlphabet(it) } }, "letters a-z strip"),
        SettingsEntry(SECTION, settingsBlock(stringResource(R.string.settings_edge_side)) {
            ChoiceChips(EdgeSide.entries, side, { stringResource(it.labelRes()) }, {
                env.previews.showEdge()
                env.write { strip.setEdgeSide(it) }
            })
        }, "a-z strip left right"),
        SettingsEntry(SECTION, settingsBlock(stringResource(R.string.settings_scrub_style)) {
            ChoiceChips(ScrubStyle.entries, style, { stringResource(it.labelRes()) }, { env.write { strip.setScrubStyle(it) } })
        }, "a-z strip animation ripple wave bell"),
        SettingsEntry(SECTION, settingsBlock(stringResource(R.string.settings_scrub_bubble)) {
            ChoiceChips(ScrubBubbleShape.entries, bubble, { stringResource(it.labelRes()) }, { env.write { strip.setScrubBubbleShape(it) } })
        }, "a-z strip letter bubble shape circle square hide"),
        SettingsEntry(SECTION, settingsBlock(stringResource(R.string.settings_ripple_speed), value = stringResource(R.string.settings_ripple_speed_value, ripple.speed)) {
            SettingsSlider(ripple.speed, RippleParams.SPEED_RANGE, { env.write { strip.setRippleParams(ripple.copy(speed = it)) } })
        }, "a-z strip ripple wave speed", visible = isRipple),
        SettingsEntry(SECTION, settingsBlock(stringResource(R.string.settings_ripple_damping), value = "%.1f".format(ripple.damping)) {
            SettingsSlider(ripple.damping, RippleParams.DAMPING_RANGE, { env.write { strip.setRippleParams(ripple.copy(damping = it)) } }, step = 0.5f)
        }, "a-z strip ripple wave damping decay", visible = isRipple),
        SettingsEntry(SECTION, settingsBlock(stringResource(R.string.settings_ripple_stiffness), value = "%.0f".format(ripple.stiffness)) {
            SettingsSlider(ripple.stiffness, RippleParams.STIFFNESS_RANGE, { env.write { strip.setRippleParams(ripple.copy(stiffness = it)) } }, step = 10f)
        }, "a-z strip ripple wave stiffness follow spring", visible = isRipple),
        SettingsEntry(SECTION, settingsBlock(stringResource(R.string.settings_ripple_reset)) {
            TextButton(onClick = { env.write { strip.setRippleParams(RippleParams()) } }) { Text(stringResource(R.string.settings_ripple_reset)) }
        }, "a-z strip ripple defaults", visible = isRipple && ripple != RippleParams()),
        SettingsEntry(SECTION, settingsSwitch(stringResource(R.string.settings_dim_others), dimOthers, { env.write { strip.setDimOthersScrubbing(it) } }, value = stringResource(R.string.settings_dim_others_detail)), "dim fade a-z scrub"),
        SettingsEntry(SECTION, settingsSwitch(stringResource(R.string.settings_hold_scroll), holdScroll, { env.write { strip.setHoldScroll(it) } }, value = stringResource(R.string.settings_hold_scroll_detail)), "a-z hold scroll"),
        SettingsEntry(SECTION, settingsBlock(stringResource(R.string.settings_hold_scroll_speed), value = holdSpeed.toString()) {
            SettingsSlider(holdSpeed.toFloat(), HOLD_SCROLL_RANGE.first.toFloat()..HOLD_SCROLL_RANGE.last.toFloat(), { env.write { strip.setHoldScrollSpeed(it.toInt()) } })
        }, "a-z hold scroll", visible = holdScroll),
        SettingsEntry(SECTION, settingsSwitch(stringResource(R.string.settings_edge_band_only), bandOnly, { env.write { strip.setEdgeZoneBandOnly(it) } }, value = stringResource(R.string.settings_edge_band_only_detail)), "a-z strip height touch"),
        SettingsEntry(SECTION, settingsSwitch(stringResource(R.string.settings_edge_inset_auto), insetAuto, { env.write { strip.setEdgeZoneInsetAuto(it) } }, value = stringResource(R.string.settings_edge_inset_auto_detail)), "back gesture edge conflict automatic"),
        SettingsEntry(SECTION, settingsBlock(stringResource(R.string.settings_edge_inset), value = inset.toString()) {
            SettingsSlider(inset.toFloat(), EDGE_ZONE_INSET_RANGE.first.toFloat()..EDGE_ZONE_INSET_RANGE.last.toFloat(), { env.write { strip.setEdgeZoneInsetDp(it.toInt()) } })
        }, "back gesture edge conflict", visible = !insetAuto),
        SettingsEntry(SECTION, settingsBlock(stringResource(R.string.settings_edge_zone_width), value = "${width}dp") {
            SettingsSlider(width.toFloat(), 32f..96f, {
                env.previews.showEdge()
                env.write { strip.setEdgeZoneWidthDp(it.roundToInt()) }
            })
        }, "a-z strip width"),
    )
}
