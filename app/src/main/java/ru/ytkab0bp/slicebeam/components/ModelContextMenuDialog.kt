package ru.ytkab0bp.slicebeam.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

/** Accessible, scrollable action list shown for a model in the workspace. */
@Composable
internal fun ModelContextMenuDialog(
    title: String,
    cancelLabel: String,
    actions: List<String>,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    val listMaxHeight = (LocalConfiguration.current.screenHeightDp * 0.56f)
        .toInt()
        .coerceAtLeast(120)
        .dp
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = listMaxHeight)) {
                itemsIndexed(actions) { index, label ->
                    TextButton(
                        onClick = { onSelect(index) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        Text(label, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(cancelLabel)
            }
        },
    )
}
