// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.data.backup

import dev.nikelauncher.data.PREF_FOLDERS
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupValuesTest {
    private val folders = """[{"id":"f","name":"F","apps":[],"widgets":[{"id":7,"h":180}]}]"""

    @Test
    fun `values survive an encode and decode with their types`() {
        val values = mapOf<String, Any>("a" to true, "b" to 3, "c" to 4L, "d" to 1.5f, "e" to "x", "f" to setOf("q", "p"))
        assertEquals(values, BackupValues.decode(BackupValues.encode(values)))
    }

    @Test
    fun `a value of the wrong type for a pinned name is refused`() {
        val json = BackupValues.encode(mapOf("size" to "big"))
        assertThrows(IllegalArgumentException::class.java) { BackupValues.decode(json, mapOf("size" to "int")) }
    }

    @Test
    fun `an unknown value type is refused`() {
        val json = JSONObject().put("x", JSONObject().put("type", "blob").put("value", 1))
        assertThrows(IllegalStateException::class.java) { BackupValues.decode(json) }
    }

    @Test
    fun `widget ids come from the home screen and from folders`() {
        val values = mapOf<String, Any>("widget_ids" to "3,4", PREF_FOLDERS to folders)
        assertEquals(listOf(3, 4, 7), BackupValues.widgetIds(values))
    }

    @Test
    fun `a widget listed twice is refused`() {
        val values = mapOf<String, Any>("widget_ids" to "7", PREF_FOLDERS to folders)
        assertThrows(IllegalArgumentException::class.java) { BackupValues.widgetIds(values) }
    }

    @Test
    fun `remapping points home and folder widgets at their new ids`() {
        val values = mutableMapOf<String, Any>("widget_ids" to "3,4", PREF_FOLDERS to folders)
        BackupValues.remapWidgets(values, mapOf(3 to 30, 4 to 40, 7 to 70))
        assertEquals("30,40", values["widget_ids"])
        assertEquals(30, values["widget_id"])
        assertEquals(listOf(30, 40, 70), BackupValues.widgetIds(values))
    }

    @Test
    fun `removing widgets leaves the others and clears an emptied list`() {
        val values = mutableMapOf<String, Any>("widget_ids" to "3", PREF_FOLDERS to folders)
        BackupValues.removeWidgets(values, setOf(3, 7))
        assertEquals("", values["widget_ids"])
        assertEquals(-1, values["widget_id"])
        assertEquals(emptyList<Int>(), BackupValues.widgetIds(values))
    }

    @Test
    fun `pending widgets round trip and a damaged list reads as empty`() {
        val items = listOf(PendingWidgetRestore(5, "a/b", "Label", mapOf("k" to 1)), PendingWidgetRestore(6, null, "None"))
        assertEquals(items, pendingWidgetsFromJson(pendingWidgetsToJson(items)))
        assertEquals(emptyList<PendingWidgetRestore>(), pendingWidgetsFromJson("not json"))
    }
}
