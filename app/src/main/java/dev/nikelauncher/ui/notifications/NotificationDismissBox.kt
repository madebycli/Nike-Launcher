// SPDX-License-Identifier: GPL-3.0-or-later
package dev.nikelauncher.ui.notifications

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import dev.nikelauncher.R
import dev.nikelauncher.media.NotificationPreview
import dev.nikelauncher.ui.kit.orion.OrionSwipeAction
import dev.nikelauncher.ui.kit.orion.OrionSwipeRow

/**
 * A swipe either way clears the notification: the row slides out and folds its height away, so the list and the sheet
 * around it shrink with it instead of jumping. A notification the app does not let go of comes back.
 */
@Composable
internal fun NotificationDismissBox(
    preview: NotificationPreview,
    onDismiss: (NotificationPreview) -> Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val current by rememberUpdatedState(preview)
    val dismiss by rememberUpdatedState(onDismiss)
    val cs = MaterialTheme.colorScheme
    val action = OrionSwipeAction(
        icon = Icons.Outlined.DeleteOutline,
        label = stringResource(R.string.notification_dismiss),
        bg = cs.errorContainer,
        fg = cs.onErrorContainer,
        removes = true,
        run = { dismiss(current) },
    )
    OrionSwipeRow(MaterialTheme.shapes.large, modifier.testTag("notification-dismiss-${preview.key}"), right = action, left = action) {
        content()
    }
}
