// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.widget

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.nikelauncher.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
