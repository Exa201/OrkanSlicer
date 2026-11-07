package ru.ytkab0bp.slicebeam.components

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Explicit consent gate before an SVG is simplified into a separate cache file. */
@Composable
internal fun SvgSimplifyConsentDialog(
    title: String,
    message: String,
    canSimplify: Boolean,
    simplifyLabel: String,
    okLabel: String,
    cancelLabel: String,
    onDismiss: () -> Unit,
    onSimplify: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Text(
                message,
                modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()).padding(end = 4.dp),
            )
        },
        confirmButton = {
            TextButton(
                onClick = if (canSimplify) onSimplify else onDismiss,
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text(if (canSimplify) simplifyLabel else okLabel) }
        },
        dismissButton = if (canSimplify) {
            { TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text(cancelLabel) } }
        } else null,
    )
}
