package ru.ytkab0bp.slicebeam.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

internal data class ProfileImportKey(val kind: Int, val index: Int)

internal data class ProfileImportOption(
    val key: ProfileImportKey,
    val name: String,
    val label: String,
    val alreadyExists: Boolean,
)

internal data class ProfileImportCategory(
    val title: String,
    val options: List<ProfileImportOption>,
)

private enum class ProfileImportPage { WARNINGS, SELECTION, CONFLICT }

/** One workspace-owned flow for profile warnings, selection, and overwrite/skip resolution. */
@Composable
internal fun ProfileConfigImportDialog(
    title: String,
    warningTitle: String,
    warningMessage: String,
    selectionTitle: String,
    conflictTitle: String,
    categories: List<ProfileImportCategory>,
    formatConflictMessage: (List<String>) -> String,
    continueLabel: String,
    importLabel: String,
    overwriteLabel: String,
    skipExistingLabel: String,
    cancelLabel: String,
    warnings: List<String>,
    onDismiss: () -> Unit,
    onImport: (selected: Set<ProfileImportKey>, replaceExisting: Boolean) -> Unit,
) {
    var page by remember(categories, warnings) {
        mutableStateOf(if (warnings.isEmpty()) ProfileImportPage.SELECTION else ProfileImportPage.WARNINGS)
    }
    val selected = remember(categories) {
        mutableStateListOf<ProfileImportKey>().apply {
            categories.flatMap(ProfileImportCategory::options).forEach { add(it.key) }
        }
    }
    val selectedKeys = selected.toSet()
    val selectedOptions = categories.flatMap(ProfileImportCategory::options)
        .filter { it.key in selectedKeys }
    val conflicts = selectedOptions.filter(ProfileImportOption::alreadyExists)
    val conflictNames = conflicts.map { option ->
        val category = categories.firstOrNull { option in it.options }?.title.orEmpty()
        "$category: ${option.name}"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when (page) {
                    ProfileImportPage.WARNINGS -> warningTitle
                    ProfileImportPage.SELECTION -> selectionTitle.ifBlank { title }
                    ProfileImportPage.CONFLICT -> conflictTitle
                },
            )
        },
        text = {
            when (page) {
                ProfileImportPage.WARNINGS -> Column(
                    modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                ) {
                    Text(warningMessage)
                }
                ProfileImportPage.SELECTION -> LazyColumn(
                    modifier = Modifier.heightIn(max = 480.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    categories.forEachIndexed { categoryIndex, category ->
                        if (category.options.isNotEmpty()) {
                            item(key = "category-$categoryIndex") {
                                Text(
                                    category.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 10.dp, bottom = 4.dp),
                                )
                            }
                            items(category.options, key = { "${it.key.kind}-${it.key.index}" }) { option ->
                                val checked = option.key in selected
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .toggleable(
                                            value = checked,
                                            role = Role.Checkbox,
                                            onValueChange = { value ->
                                                if (value) {
                                                    if (option.key !in selected) selected.add(option.key)
                                                } else selected.remove(option.key)
                                            },
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Checkbox(checked = checked, onCheckedChange = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(option.label, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
                ProfileImportPage.CONFLICT -> Column(
                    modifier = Modifier.heightIn(max = 460.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(formatConflictMessage(conflictNames))
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(conflictNames, key = { it }) { conflict ->
                            Text(conflict, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            when (page) {
                ProfileImportPage.WARNINGS -> TextButton(onClick = { page = ProfileImportPage.SELECTION }) {
                    Text(continueLabel)
                }
                ProfileImportPage.SELECTION -> TextButton(onClick = {
                    if (conflicts.isEmpty()) onImport(selectedKeys, false) else page = ProfileImportPage.CONFLICT
                }) {
                    Text(importLabel)
                }
                ProfileImportPage.CONFLICT -> Column(horizontalAlignment = Alignment.End) {
                    TextButton(onClick = { onImport(selectedKeys, true) }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(overwriteLabel)
                    }
                    TextButton(onClick = { onImport(selectedKeys, false) }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(skipExistingLabel)
                    }
                    TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(cancelLabel)
                    }
                }
            }
        },
        dismissButton = if (page == ProfileImportPage.CONFLICT) null else {
            {
                TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(cancelLabel)
                }
            }
        },
    )
}
