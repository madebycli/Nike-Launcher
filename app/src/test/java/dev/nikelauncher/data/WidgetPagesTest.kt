// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WidgetPagesTest {
    @Test
    fun `a grid round trips through json`() {
        val widgets = listOf(GridWidget(3, 1, 2), GridWidget(4, 2, 5, configuredHeightDp = 321))
        assertEquals(widgets, gridWidgetsFromJson(gridWidgetsToJson(widgets)))
    }

    @Test
    fun `a damaged or repeating list reads as what can be kept`() {
        assertEquals(emptyList<GridWidget>(), gridWidgetsFromJson("not json"))
        val raw = """[{"id":3},{"id":3,"columns":1},{"id":-1},{"id":4,"columns":9,"rows":99}]"""
        val read = gridWidgetsFromJson(raw)
        assertEquals(listOf(3, 4), read.map { it.id })
        assertEquals(2, read[1].columns)
        assertEquals(MAX_GRID_WIDGET_ROWS, read[1].rows)
    }

    @Test
    fun `moving inserts and leaves the others in order`() {
        val widgets = listOf(GridWidget(1), GridWidget(2), GridWidget(3))
        assertEquals(listOf(2, 3, 1), moveGridWidget(widgets, 1, 2).map { it.id })
        assertEquals(listOf(1, 2, 3), moveGridWidget(widgets, 9, 0).map { it.id })
    }

    @Test
    fun `resizing stays inside the grid and keeps identity`() {
        val resized = resizeGridWidget(GridWidget(7, 1, 2), columnsDelta = 5f, rowsDelta = -9f)
        assertEquals(7, resized.id)
        assertEquals(2, resized.columns)
        assertEquals(1, resized.rows)
    }

    @Test
    fun `a size hint decides the columns and a height that is not a whole number of rows`() {
        val narrow = configuredGridWidget(GridWidget(1), widthDp = 100, heightDp = 150, availableWidthDp = 360)
        assertEquals(1, narrow.columns)
        assertEquals(150, narrow.configuredHeightDp)
        assertNull(configuredGridWidget(GridWidget(1), 100, null, 360).configuredHeightDp)
    }

    @Test
    fun `the dim of a page is bounded and off unless asked for`() {
        assertEquals(0f, WidgetPagesConfig().wallpaperDimAlpha, 0f)
        assertEquals(0.85f, WidgetPagesConfig(dimWallpaper = true, dimAlpha = 5f).wallpaperDimAlpha, 0f)
    }
}
