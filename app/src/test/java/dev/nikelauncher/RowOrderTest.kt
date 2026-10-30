// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher

import dev.nikelauncher.ui.settings.moveRow
import org.junit.Assert.*
import org.junit.Test

class RowOrderTest {
    @Test fun `moves preserve the other rows and every key`() {
        val rows = listOf("a", "b", "c")
        assertEquals(listOf("b", "c", "a"), moveRow(rows, 0, 2))
        assertEquals(listOf("c", "a", "b"), moveRow(rows, 2, 0))
        assertEquals(rows, moveRow(rows, 0, -1))
        assertEquals(rows, moveRow(rows, 3, 0))
        assertEquals(emptyList<String>(), moveRow(emptyList<String>(), 0, 1))
    }
}
