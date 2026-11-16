package ru.ytkab0bp.slicebeam.components.bed_menu

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.utils.RecentModelDocument

@Composable
internal fun RecentModelDocumentsDialog(
    documents: List<RecentModelDocument>,
    onOpen: (RecentModelDocument) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.RecentModelsTitle)) },
        text = {
            if (documents.isEmpty()) {
                Text(stringResource(R.string.RecentModelsEmpty))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)
                        .testTag("recent-model-documents-list"),
                ) {
                    itemsIndexed(documents.asReversed(), key = { _, item -> item.uri }) { index, item ->
                        ListItem(
                            headlineContent = { Text(item.displayName, maxLines = 2) },
                            supportingContent = {
                                Text(
                                    item.displayName.substringAfterLast('.', "").uppercase(),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                                .clickable(role = Role.Button) { onOpen(item) }
                                .testTag("recent-model-document-$index"),
                            tonalElevation = 0.dp,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}
