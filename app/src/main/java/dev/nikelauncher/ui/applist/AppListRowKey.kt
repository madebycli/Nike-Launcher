// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.applist

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

internal fun rowKey(row: AppListRow): String =
    when (row) {
        is AppListRow.Header -> "header:${row.text}"
        is AppListRow.FolderRow -> "folderrow:${row.folder.id}"
        is AppListRow.Entry -> row.section?.let { "$it:${row.app.key}" } ?: row.app.key
    }
