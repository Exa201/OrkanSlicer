package ru.ytkab0bp.slicebeam.ui.workspace

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** Shared Material 3 confirmation/error dialog used by workspace actions. */
@Composable
internal fun WorkspaceMessageDialog(
    title: String?,
    message: String,
    confirmLabel: String,
    dismissLabel: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val messageMaxHeight = (LocalConfiguration.current.screenHeightDp * 0.38f).coerceAtLeast(120f).dp
    AlertDialog(
        onDismissRequest = onDismiss,
        title = title?.let { { Text(it) } },
        text = {
            Text(
                message,
                modifier = Modifier.heightIn(max = messageMaxHeight)
                    .testTag("workspace-message-scroll")
                    .verticalScroll(rememberScrollState()),
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.heightIn(min = 48.dp).testTag("workspace-message-confirm")) {
                Text(confirmLabel)
            }
        },
        dismissButton = dismissLabel?.let { label ->
            {
                TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(label)
                }
            }
        },
    )
}
