// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.widget

/** Last completed calendar results, in memory only, across widget/view reattachment. */
internal class CalendarSnapshotCache(private val capacity: Int = 8) {
    private data class Snapshot(val excluded: Set<String>, val result: CalendarLoad.Ready)
    private val entries = LinkedHashMap<Int, Snapshot>(capacity, .75f, true)

    init { require(capacity > 0) }

    @Synchronized
    fun read(widgetId: Int, excluded: Set<String>, hasPermission: Boolean): CalendarLoad.Ready? {
        if (!hasPermission) {
            entries.clear()
            return null
        }
        val saved = entries[widgetId] ?: return null
        if (saved.excluded != excluded) {
            entries.remove(widgetId)
            return null
        }
        return saved.result
    }

    @Synchronized
    fun complete(widgetId: Int, excluded: Set<String>, hasPermission: Boolean,
        result: CalendarLoad): CalendarLoad {
        if (!hasPermission || result == CalendarLoad.PermissionRequired) {
            entries.clear()
            return CalendarLoad.PermissionRequired
        }
        if (result is CalendarLoad.Ready) {
            val saved = CalendarLoad.Ready(result.events.toList())
            entries[widgetId] = Snapshot(excluded.toSet(), saved)
            while (entries.size > capacity) entries.remove(entries.keys.first())
            return saved
        }
        // A temporarily busy provider must not blank a previously loaded widget.
        return read(widgetId, excluded, true) ?: result
    }

    @Synchronized
    fun forget(widgetId: Int) { entries.remove(widgetId) }

    companion object { val shared = CalendarSnapshotCache() }
}
