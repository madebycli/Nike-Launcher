// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.widget

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.provider.CalendarContract
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.nikelauncher.R
import androidx.compose.material3.AlertDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class ClockAppKind { CLOCK, CALENDAR, WEATHER }

private data class AppChoice(val pkg: String, val label: String)

/** Only apps in this profile which can actually perform the relevant action are offered. */
@Suppress("DEPRECATION")
private fun choices(context: Context, kind: ClockAppKind): List<AppChoice> {
    val pm = context.packageManager
    val intents = when (kind) {
        ClockAppKind.CLOCK -> listOf(Intent(AlarmClock.ACTION_SHOW_ALARMS))
        ClockAppKind.CALENDAR -> listOf(
            Intent(Intent.ACTION_VIEW).setDataAndType(
                android.content.ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, 1),
                "vnd.android.cursor.item/event"),
            Intent(Intent.ACTION_INSERT).setDataAndType(CalendarContract.Events.CONTENT_URI,
                "vnd.android.cursor.dir/event"),
        )
        ClockAppKind.WEATHER -> listOf(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER))
    }
    return intents.flatMap { intent ->
        runCatching { pm.queryIntentActivities(intent, 0) }.getOrDefault(emptyList())
    }.mapNotNull { info ->
        val activity = info.activityInfo ?: return@mapNotNull null
        if (!activity.exported || activity.packageName == context.packageName) return@mapNotNull null
        val label = runCatching { info.loadLabel(pm).toString() }.getOrDefault(activity.packageName)
        AppChoice(activity.packageName, label)
    }.distinctBy { it.pkg }.sortedBy { it.label.lowercase(java.util.Locale.getDefault()) }
}

/** Shared by widget settings and first-run setup. Null means system default (or no weather app). */
@Composable
fun ClockAppPickerDialog(kind: ClockAppKind, selected: String?, onPick: (String?) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var apps by remember(kind) { mutableStateOf<List<AppChoice>?>(null) }
    LaunchedEffect(kind) { apps = withContext(Dispatchers.IO) { choices(context, kind) } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.widget_settings_choose_app)) },
        text = {
            val available = apps
            if (available == null) CircularProgressIndicator()
            else LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                item {
                    Row(Modifier.fillMaxWidth().selectable(selected == null, role = Role.RadioButton) {
                        onPick(null)
                    }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(if (kind == ClockAppKind.WEATHER) R.string.widget_settings_no_app
                            else R.string.widget_settings_system_app), Modifier.weight(1f))
                        RadioButton(selected = selected == null, onClick = null)
                    }
                }
                items(available, key = { it.pkg }) { app ->
                    Row(Modifier.fillMaxWidth().selectable(selected == app.pkg, role = Role.RadioButton) {
                        onPick(app.pkg)
                    }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(app.label, Modifier.weight(1f))
                        RadioButton(selected = selected == app.pkg, onClick = null)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.widget_settings_done)) } },
    )
}
