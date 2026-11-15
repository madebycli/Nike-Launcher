// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import dev.nikelauncher.NikeApp
import dev.nikelauncher.R
import dev.nikelauncher.data.backup.PendingWidgetRestore
import dev.nikelauncher.data.backup.SettingsBackup
import dev.nikelauncher.ui.theme.NikeTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Finishes what a restore could not: each widget it left unbound is bound again, configured if it
 * has a screen for that, or swapped for another. One at a time, in a sheet, until none remain.
 */
@OptIn(ExperimentalMaterial3Api::class)
class WidgetRestoreActivity : ComponentActivity() {
    private val app get() = application as NikeApp
    private val manager get() = AppWidgetManager.getInstance(this)
    private var items by mutableStateOf<List<PendingWidgetRestore>?>(null)
    private var busy by mutableStateOf(false)
    private var feedback by mutableStateOf<Int?>(null)
    private var pendingId = -1

    private val bind = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        busy = false
        if (it.resultCode == RESULT_OK) afterBind() else feedback = R.string.backup_widget_retry
    }
    private val replace = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        busy = false
        val id = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
        if (result.resultCode == RESULT_OK && id > 0) lifecycleScope.launch {
            val target = app.prefs.pendingWidgetRestores.first().firstOrNull { it.id == pendingId }
            if (target != null) {
                app.prefs.completeWidgetRestore(target.id, id)
                runCatching { app.widgetHost.deleteAppWidgetId(target.id) }
            } else {
                runCatching { app.widgetHost.deleteAppWidgetId(id) }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingId = savedInstanceState?.getInt("pending", -1) ?: -1
        lifecycleScope.launch { app.prefs.pendingWidgetRestores.collect { items = it } }
        setContent {
            NikeTheme {
                val pending = items
                LaunchedEffect(pending) { if (pending != null && pending.isEmpty()) finish() }
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                val scope = rememberCoroutineScope()
                ModalBottomSheet(onDismissRequest = { finish() }, sheetState = sheetState) {
                    Column(Modifier.fillMaxWidth().heightIn(max = (LocalConfiguration.current.screenHeightDp * .83f).dp)) {
                        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.backup_widgets_title), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                            IconButton(onClick = { scope.launch { sheetState.hide(); finish() } }) {
                                Icon(Icons.Outlined.Close, stringResource(R.string.action_close))
                            }
                        }
                        if (pending == null) {
                            CircularProgressIndicator(Modifier.padding(20.dp))
                        } else pending.firstOrNull()?.let { widget ->
                            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                                Text(
                                    stringResource(R.string.backup_widgets_remaining, pending.size),
                                    Modifier.padding(top = 12.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(widget.label, Modifier.padding(vertical = 8.dp), style = MaterialTheme.typography.titleMedium)
                                feedback?.let {
                                    Text(stringResource(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                }
                                if (widget.provider != null) {
                                    TextButton(onClick = { chooseReplacement(widget) }, enabled = !busy) {
                                        Text(stringResource(R.string.backup_widget_choose_other))
                                    }
                                }
                            }
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                TextButton(onClick = { finish() }, enabled = !busy) { Text(stringResource(R.string.backup_widgets_later)) }
                                Spacer(Modifier.width(8.dp))
                                Button(onClick = { continueRestore(widget) }, enabled = !busy, modifier = Modifier.heightIn(min = 48.dp)) {
                                    Text(stringResource(if (widget.provider == null) R.string.backup_widget_choose else R.string.backup_widget_setup))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun chooseReplacement(widget: PendingWidgetRestore) {
        pendingId = widget.id
        feedback = null
        busy = true
        replace.launch(Intent(this, WidgetPickerActivity::class.java))
    }

    private fun continueRestore(widget: PendingWidgetRestore) {
        pendingId = widget.id
        feedback = null
        val provider = widget.provider?.let(ComponentName::unflattenFromString)
        if (provider == null) { chooseReplacement(widget); return }
        if (manager.getAppWidgetInfo(widget.id)?.provider == provider) { afterBind(); return }
        if (manager.installedProviders.none { it.provider == provider }) { feedback = R.string.backup_widget_missing; return }
        busy = true
        val granted = runCatching {
            manager.bindAppWidgetIdIfAllowed(widget.id, provider, SettingsBackup.optionsBundle(widget.options))
        }.getOrDefault(false)
        if (granted) {
            busy = false
            afterBind()
        } else {
            bind.launch(
                Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widget.id)
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider)
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS, SettingsBackup.optionsBundle(widget.options))
                },
            )
        }
    }

    private fun afterBind() {
        val widget = items?.firstOrNull { it.id == pendingId } ?: return
        val info = manager.getAppWidgetInfo(widget.id)
        if (info == null || info.provider != widget.provider?.let(ComponentName::unflattenFromString)) {
            feedback = R.string.backup_widget_retry
            return
        }
        manager.updateAppWidgetOptions(widget.id, SettingsBackup.optionsBundle(widget.options))
        if (info.configure != null && info.provider != ClockWidgetProvider.componentName(this)) {
            busy = true
            val started = runCatching { app.widgetHost.startAppWidgetConfigureActivityForResult(this, widget.id, 0, CONFIGURE_REQUEST, null); true }
                .getOrDefault(false)
            if (!started) {
                busy = false
                feedback = R.string.backup_widget_retry
            }
        } else {
            complete()
        }
    }

    private fun complete() {
        lifecycleScope.launch {
            busy = true
            app.prefs.completeWidgetRestore(pendingId)
            runCatching { ClockWidgetProvider.syncWeatherReceiver(this@WidgetRestoreActivity) }
            ClockWidgetProvider.renderAll(this@WidgetRestoreActivity)
            busy = false
            feedback = null
        }
    }

    @Deprecated("AppWidgetHost configuration uses request codes")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CONFIGURE_REQUEST) {
            busy = false
            if (resultCode == RESULT_OK) complete() else feedback = R.string.backup_widget_retry
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("pending", pendingId)
        super.onSaveInstanceState(outState)
    }

    private companion object {
        const val CONFIGURE_REQUEST = 701
    }
}
