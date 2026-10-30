// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.widget

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.content.res.Configuration
import android.util.SizeF
import android.widget.Toast
import kotlin.math.roundToInt
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dev.nikelauncher.NikeApp
import dev.nikelauncher.ui.theme.NikeTheme
import dev.nikelauncher.R
import dev.nikelauncher.data.responsiveWidgetSize
import androidx.compose.ui.res.stringResource

private const val REQUEST_CONFIGURE = 1

class WidgetPickerActivity : ComponentActivity() {

    private lateinit var appWidgetManager: AppWidgetManager
    private lateinit var widgetHost: NikeAppWidgetHost
    private var pendingWidgetId: Int = -1
    private var reconfiguring = false
    private var configurationOptionsBefore: Bundle? = null

    private val bindLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) proceedAfterBind(pendingWidgetId) else cancel()
        }

    private data class AppGroup(
        val label: String,
        val icon: Drawable?,
        val widgets: List<AppWidgetProviderInfo>,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appWidgetManager = AppWidgetManager.getInstance(this)
        widgetHost = (application as NikeApp).widgetHost
        pendingWidgetId = savedInstanceState?.getInt("pending_widget_id", -1) ?: -1
        reconfiguring = savedInstanceState?.getBoolean("reconfiguring")
            ?: (intent.getIntExtra(EXTRA_RECONFIGURE_WIDGET_ID, -1) > 0)
        configurationOptionsBefore = savedInstanceState?.getBundle("options_before")
        if (reconfiguring) {
            pendingWidgetId = savedInstanceState?.getInt("pending_widget_id", -1)
                ?: intent.getIntExtra(EXTRA_RECONFIGURE_WIDGET_ID, -1)
            // Existing IDs are never owned by this picker and must survive cancellation.
            if (savedInstanceState == null) {
                val info = appWidgetManager.getAppWidgetInfo(pendingWidgetId)
                if (info?.configure == null) { cancel(); return }
                configurationOptionsBefore = Bundle(appWidgetManager.getAppWidgetOptions(pendingWidgetId))
                launchConfiguration(pendingWidgetId)
            }
            return
        }

        setContent {
            NikeTheme {
                val providers = remember { appWidgetManager.installedProviders }
                val groups = remember(providers) {
                    providers.groupBy { it.provider.packageName }
                        .map { (pkg, widgets) ->
                            val appInfo = runCatching { packageManager.getApplicationInfo(pkg, 0) }.getOrNull()
                            AppGroup(
                                label = appInfo?.loadLabel(packageManager)?.toString() ?: pkg,
                                icon = appInfo?.loadIcon(packageManager),
                                widgets = widgets.sortedBy { it.loadLabel(packageManager) },
                            )
                        }
                        .sortedBy { it.label.lowercase() }
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        containerColor = MaterialTheme.colorScheme.surface,
                        topBar = {
                            TopAppBar(
                                title = { Text(stringResource(R.string.widget_add)) },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                ),
                            )
                        }
                    ) { padding ->
                        if (groups.isEmpty()) {
                            Text(
                                stringResource(R.string.widget_none_found),
                                modifier = Modifier.padding(padding).padding(20.dp),
                            )
                        } else {
                            LazyColumn(modifier = Modifier.padding(padding)) {
                                groups.forEach { group ->
                                    item {
                                        ListItem(
                                            headlineContent = { Text(group.label, style = MaterialTheme.typography.titleSmall) },
                                            leadingContent = { DrawableIcon(group.icon, 32.dp) },
                                        )
                                    }
                                    items(group.widgets) { info -> WidgetRow(info) { pick(info) } }
                                    item { HorizontalDivider() }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun WidgetRow(info: AppWidgetProviderInfo, onClick: () -> Unit) {
        val icon = remember(info) {
            runCatching { info.loadIcon(this, resources.configuration.densityDpi) }.getOrNull()
        }
        ListItem(
            headlineContent = { Text(info.loadLabel(packageManager)) },
            leadingContent = { DrawableIcon(icon, 40.dp) },
            modifier = Modifier.clickable(onClick = onClick),
        )
    }

    private fun pick(info: AppWidgetProviderInfo) {
        val id = widgetHost.allocateAppWidgetId()
        pendingWidgetId = id
        val bound = appWidgetManager.bindAppWidgetIdIfAllowed(id, info.provider)
        if (bound) {
            proceedAfterBind(id)
        } else {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
            }
            bindLauncher.launch(intent)
        }
    }

    private fun proceedAfterBind(id: Int) {
        val info = appWidgetManager.getAppWidgetInfo(id)
        if (info?.configure == null || configurationIsOptional(info)) {
            finishWithWidgetId(id)
            return
        }
        // Through the host rather than as a plain Intent of our own. A widget's configure
        // screen usually is not exported — it expects to be started by whoever holds the
        // widget id — so starting it directly is refused, and that refusal is a crash rather
        // than a missed widget. This is the call that carries the host's standing to open it.
        configurationOptionsBefore = Bundle(appWidgetManager.getAppWidgetOptions(id))
        launchConfiguration(id)
    }

    private fun launchConfiguration(id: Int) {
        val started = runCatching {
            widgetHost.startAppWidgetConfigureActivityForResult(this, id, 0, REQUEST_CONFIGURE, null)
            true
        }.getOrDefault(false)
        if (!started) {
            Toast.makeText(this, R.string.widget_unavailable, Toast.LENGTH_SHORT).show()
            if (reconfiguring) cancel() else finishWithWidgetId(id)
        }
    }

    /** Android 12 let a widget say its configure screen is optional; those can skip it. */
    private fun configurationIsOptional(info: AppWidgetProviderInfo): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            (info.widgetFeatures and AppWidgetProviderInfo.WIDGET_FEATURE_CONFIGURATION_OPTIONAL) != 0

    @Deprecated("Required by startAppWidgetConfigureActivityForResult, which is requestCode-based")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_CONFIGURE) return
        if (resultCode == RESULT_OK) finishWithWidgetId(pendingWidgetId) else cancel()
    }

    private fun finishWithWidgetId(id: Int) {
        val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            .putExtra(EXTRA_RECONFIGURED, reconfiguring)
        val afterOptions = appWidgetManager.getAppWidgetOptions(id)
        val legacyAfter = legacySize(afterOptions)
        // A half-width widget must select its own responsive variant. Screen width
        // would pick the full-width variant when the provider changes its size list.
        val referenceWidth = legacySize(configurationOptionsBefore)?.first
            ?: legacyAfter?.first ?: resources.configuration.screenWidthDp
        val modernAfter = responsiveSize(afterOptions, referenceWidth)
        val requested = if (!reconfiguring) {
            modernAfter ?: legacyAfter
        } else {
            // Either contract may be updated independently. Unchanged legacy dimensions
            // must not mask a new responsive size, or an old host size list a legacy change.
            modernAfter?.takeIf { it != responsiveSize(configurationOptionsBefore, referenceWidth) }
                ?: legacyAfter?.takeIf { it != legacySize(configurationOptionsBefore) }
        }
        if (requested != null) {
            // Option dimensions describe content; host dimensions also include framework
            // padding. Do not shrink a configured widget on every round trip.
            val info = appWidgetManager.getAppWidgetInfo(id)
            if (info != null) {
                val padding = AppWidgetHostView.getDefaultPaddingForWidget(this, info.provider, null)
                val density = resources.displayMetrics.density
                result.putExtra(EXTRA_CONFIGURED_WIDTH_DP,
                    (requested.first + (padding.left + padding.right) / density).roundToInt().coerceIn(40, 8192))
                result.putExtra(EXTRA_CONFIGURED_HEIGHT_DP,
                    (requested.second + (padding.top + padding.bottom) / density).roundToInt()
                        .coerceIn(40, dev.nikelauncher.data.MAX_GRID_WIDGET_ROWS * 100 - 12))
            }
        }
        setResult(RESULT_OK, result)
        pendingWidgetId = -1 // Ownership has passed to the launcher page.
        finish()
    }

    private fun cancel() {
        if (!reconfiguring && pendingWidgetId != -1) runCatching { widgetHost.deleteAppWidgetId(pendingWidgetId) }
        pendingWidgetId = -1
        setResult(RESULT_CANCELED)
        finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("pending_widget_id", pendingWidgetId)
        outState.putBoolean("reconfiguring", reconfiguring)
        outState.putBundle("options_before", configurationOptionsBefore)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        // Back from the picker must not leave an allocated widget broadcasting forever.
        // A rotation, or a provider's configuration activity opening, still owns the id.
        if (isFinishing && !reconfiguring && pendingWidgetId > 0) {
            runCatching { widgetHost.deleteAppWidgetId(pendingWidgetId) }
        }
        super.onDestroy()
    }
    /** Only actual provider options can request a size; preview-only UI cannot. */
    private fun legacySize(options: Bundle?): Pair<Int, Int>? {
        options ?: return null
        val landscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val width = options.getInt(if (landscape) AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH else AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val height = options.getInt(if (landscape) AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT else AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
        return if (width > 0 && height > 0) width to height else null
    }

    private fun responsiveSize(options: Bundle?, referenceWidthDp: Int): Pair<Int, Int>? {
        if (options == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        @Suppress("DEPRECATION")
        val sizes = options.getParcelableArrayList<SizeF>(AppWidgetManager.OPTION_APPWIDGET_SIZES).orEmpty()
        return responsiveWidgetSize(sizes.map { it.width to it.height }, referenceWidthDp)
    }

    companion object {
        const val EXTRA_RECONFIGURE_WIDGET_ID = "dev.nikelauncher.extra.RECONFIGURE_WIDGET_ID"
        const val EXTRA_RECONFIGURED = "dev.nikelauncher.extra.RECONFIGURED"
        const val EXTRA_CONFIGURED_WIDTH_DP = "dev.nikelauncher.extra.CONFIGURED_WIDTH_DP"
        const val EXTRA_CONFIGURED_HEIGHT_DP = "dev.nikelauncher.extra.CONFIGURED_HEIGHT_DP"
    }
}

@Composable
private fun DrawableIcon(drawable: Drawable?, size: androidx.compose.ui.unit.Dp) {
    AndroidView(
        modifier = Modifier.size(size),
        factory = { ctx -> ImageView(ctx) },
        update = { it.setImageDrawable(drawable) },
    )
}
