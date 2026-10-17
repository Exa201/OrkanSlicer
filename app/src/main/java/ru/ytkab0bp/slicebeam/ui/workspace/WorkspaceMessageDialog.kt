package ru.ytkab0bp.slicebeam.ui.workspace

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = title?.let { { Text(it) } },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.heightIn(min = 48.dp)) {
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
