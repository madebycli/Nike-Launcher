// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.widget

import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.nikelauncher.R
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Stable saved style IDs; groups affect browsing only. */
internal val clockStyleGroups =
    listOf(
        R.string.widget_settings_style_type to
            listOf(
                ClockStyle.CLASSIC,
                ClockStyle.SPLIT,
                ClockStyle.SERIF,
                ClockStyle.FINE,
                ClockStyle.RAIL,
                ClockStyle.CAPSULE,
            ),
        R.string.widget_settings_style_graphic to
            listOf(
                ClockStyle.OUTLINE,
                ClockStyle.LINES,
                ClockStyle.PIXEL,
                ClockStyle.GLITCH,
                ClockStyle.DOT_MATRIX,
            ),
        R.string.widget_settings_style_layout to
            listOf(
                ClockStyle.VERTICAL,
                ClockStyle.FLIP,
                ClockStyle.OVERLAP,
                ClockStyle.WEEKDAY,
                ClockStyle.MINIMAL,
            ),
        R.string.widget_settings_style_analog to
            listOf(ClockStyle.ANALOG, ClockStyle.ORBIT, ClockStyle.RINGS, ClockStyle.NONE),
    )

@Composable
fun ClockStyleGallery(config: ClockWidgetConfig, onSelect: (ClockStyle) -> Unit) {
    val sample = remember {
        LocalDate.now().atTime(16, 0).atZone(ZoneId.systemDefault()).toInstant()
    }
    val onCard = MaterialTheme.colorScheme.onSurface
    val fontScale = LocalDensity.current.fontScale
    LazyVerticalGrid(
        columns = GridCells.Adaptive(140.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.padding(bottom = 8.dp)) {
                Text(
                    stringResource(R.string.widget_settings_style_count, ClockStyle.entries.size),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(20.dp))
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Column(Modifier.fillMaxWidth().padding(20.dp)) {
                        Box(
                            Modifier.fillMaxWidth().height((150 * fontScale).dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            ClockFace(
                                config.copy(
                                    timeSizeSp = 64,
                                    timeOpacity = 100,
                                    textColor = onCard.toArgb(),
                                ),
                                sample,
                            )
                        }
                        Text(
                            stringResource(config.style.label),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
        }
        clockStyleGroups.forEach { (title, styles) ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    stringResource(title),
                    Modifier.padding(start = 4.dp, top = 16.dp, bottom = 4.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(styles, key = { it.name }) { style ->
                val isSelected = style == config.style
                Surface(
                    onClick = { onSelect(style) },
                    shape = MaterialTheme.shapes.extraLarge,
                    color =
                        if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerLow,
                    border =
                        BorderStroke(
                            if (isSelected) 2.dp else 1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f),
                        ),
                    modifier = Modifier.fillMaxWidth().semantics { selected = isSelected },
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Box(
                            Modifier.fillMaxWidth().height((104 * fontScale).dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            if (style == ClockStyle.NONE)
                                Icon(
                                    Icons.Outlined.Remove,
                                    null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            else
                                ClockFace(
                                    config.copy(
                                        style = style,
                                        timeSizeSp = 38,
                                        textColor = onCard.toArgb(),
                                        timeOpacity = 100,
                                    ),
                                    sample,
                                )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                stringResource(style.label),
                                Modifier.weight(1f),
                                style = MaterialTheme.typography.labelLarge,
                            )
                            if (isSelected)
                                Icon(
                                    Icons.Outlined.CheckCircle,
                                    stringResource(R.string.widget_settings_selected),
                                    Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClockCalendarPicker(
    config: ClockWidgetConfig,
    onChange: (Set<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var calendars by remember { mutableStateOf<List<CalendarChoice>?>(null) }
    var excluded by remember { mutableStateOf(config.excludedCalendars) }
    LaunchedEffect(Unit) {
        calendars = withContext(Dispatchers.IO) { ClockWidgetAgenda.calendars(context) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.clock_calendars)) },
        confirmButton = {
            TextButton(
                onClick = {
                    onChange(excluded)
                    onDismiss()
                }
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
        text = {
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                if (calendars == null) item { Text(stringResource(R.string.clock_loading)) }
                else if (calendars!!.isEmpty())
                    item { Text(stringResource(R.string.clock_calendars_empty)) }
                calendars.orEmpty().forEach { calendar ->
                    item(calendar.id) {
                        val id = calendar.id.toString()
                        fun toggle() {
                            excluded = if (id in excluded) excluded - id else excluded + id
                        }
                        Row(
                            Modifier.fillMaxWidth()
                                .toggleable(
                                    id !in excluded,
                                    role = Role.Checkbox,
                                    onValueChange = { toggle() },
                                )
                                .heightIn(min = 48.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = id !in excluded, onCheckedChange = null)
                            Text(calendar.name)
                        }
                    }
                }
            }
        },
    )
}
