package ru.ytkab0bp.slicebeam.components.bed_menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

/** Three-step, scrollable profile picker that preserves the legacy export categories. */
@Composable
internal fun ProfileExportSelectionDialog(
    titles: List<String>,
    categories: List<List<String>>,
    cancelLabel: String,
    backLabel: String,
    nextLabel: String,
    exportLabel: String,
    onDismiss: () -> Unit,
    onComplete: (List<List<Int>>) -> Unit,
) {
    var step by remember(categories) { mutableIntStateOf(0) }
    val checked = remember(categories) {
        categories.map { entries -> mutableStateListOf<Boolean>().apply { repeat(entries.size) { add(true) } } }
    }
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.55f).toInt().coerceAtLeast(140).dp
    val index = step.coerceIn(categories.indices)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titles[index]) },
        text = {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = maxHeight).testTag("profile-export-category-list")) {
                itemsIndexed(categories[index]) { itemIndex, label ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Checkbox(
                            checked = checked[index][itemIndex],
                            onCheckedChange = { checked[index][itemIndex] = it },
                            modifier = Modifier.testTag("profile-export-selection-$index-$itemIndex"),
                        )
                        Text(label, Modifier.weight(1f))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (step < categories.lastIndex) step++
                    else onComplete(checked.map { selections -> selections.indices.filter { selections[it] } })
                },
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text(if (step == categories.lastIndex) exportLabel else nextLabel) }
        },
        dismissButton = {
            Row {
                if (step > 0) {
                    TextButton(onClick = { step-- }, modifier = Modifier.heightIn(min = 48.dp)) { Text(backLabel) }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text(cancelLabel) }
            }
        },
    )
}
