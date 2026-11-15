// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** The pages of widgets either side of the home screen, and the grids of widgets on them. */
class WidgetPagePrefs internal constructor(private val store: DataStore<Preferences>) {

    val config: Flow<WidgetPagesConfig> = store.data.map {
        WidgetPagesConfig(
            leftEnabled = it[PrefKeys.WIDGET_PAGE_LEFT_ENABLED] ?: false,
            rightEnabled = it[PrefKeys.WIDGET_PAGE_RIGHT_ENABLED] ?: false,
            dimWallpaper = it[PrefKeys.WIDGET_PAGE_DIM_ENABLED] ?: false,
            dimAlpha = boundedWidgetPageDim(it[PrefKeys.WIDGET_PAGE_DIM_ALPHA] ?: DEFAULT_WIDGET_PAGE_DIM_ALPHA),
        )
    }.distinctUntilChanged()

    fun widgets(side: WidgetPageSide): Flow<List<GridWidget>> =
        store.data.map { gridWidgetsFromJson(it[gridKey(side)]) }.distinctUntilChanged()

    private fun gridKey(side: WidgetPageSide) =
        if (side == WidgetPageSide.LEFT) PrefKeys.WIDGET_PAGE_LEFT else PrefKeys.WIDGET_PAGE_RIGHT

    suspend fun setEnabled(side: WidgetPageSide, enabled: Boolean) {
        store.edit {
            it[if (side == WidgetPageSide.LEFT) PrefKeys.WIDGET_PAGE_LEFT_ENABLED else PrefKeys.WIDGET_PAGE_RIGHT_ENABLED] = enabled
        }
    }

    suspend fun setDimEnabled(enabled: Boolean) {
        store.edit { it[PrefKeys.WIDGET_PAGE_DIM_ENABLED] = enabled }
    }

    suspend fun setDimAlpha(alpha: Float) {
        store.edit { it[PrefKeys.WIDGET_PAGE_DIM_ALPHA] = boundedWidgetPageDim(alpha) }
    }

    suspend fun add(side: WidgetPageSide, widget: GridWidget) {
        store.edit {
            val key = gridKey(side)
            val widgets = gridWidgetsFromJson(it[key])
            if (widget.id > 0 && widgets.none { entry -> entry.id == widget.id }) {
                it[key] = gridWidgetsToJson(
                    widgets + widget.copy(columns = widget.columns.coerceIn(1, 2), rows = widget.rows.coerceIn(1, MAX_GRID_WIDGET_ROWS)),
                )
            }
        }
    }

    suspend fun remove(side: WidgetPageSide, id: Int) {
        store.edit {
            val key = gridKey(side)
            it[key] = gridWidgetsToJson(gridWidgetsFromJson(it[key]).filterNot { entry -> entry.id == id })
        }
    }

    suspend fun resize(side: WidgetPageSide, id: Int, columns: Int, rows: Int, configuredHeightDp: Int? = null) {
        store.edit {
            val key = gridKey(side)
            it[key] = gridWidgetsToJson(
                gridWidgetsFromJson(it[key]).map { entry ->
                    if (entry.id == id) {
                        entry.copy(
                            columns = columns.coerceIn(1, 2),
                            rows = rows.coerceIn(1, MAX_GRID_WIDGET_ROWS),
                            configuredHeightDp = configuredHeightDp?.coerceIn(40, MAX_GRID_WIDGET_ROWS * 100 - 12),
                        )
                    } else {
                        entry
                    }
                },
            )
        }
    }

    suspend fun move(side: WidgetPageSide, id: Int, index: Int) {
        store.edit {
            val key = gridKey(side)
            it[key] = gridWidgetsToJson(moveGridWidget(gridWidgetsFromJson(it[key]), id, index))
        }
    }
}
