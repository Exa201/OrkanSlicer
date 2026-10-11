package ru.ytkab0bp.slicebeam.ui.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.fragment.ProfileSettingsComposeItem
import ru.ytkab0bp.slicebeam.config.ProfileStringVectorCodec
import ru.ytkab0bp.slicebeam.config.ProfileOpenOptionResolver
import java.util.function.Consumer
import java.util.function.Supplier

private val profileStringVectorStateSaver = listSaver<MutableState<List<String>>, String>(
    save = { it.value },
    restore = { mutableStateOf(it) },
)

/** List editor for Orca's escaped, semicolon-separated string vectors, including multiline G-code entries. */
object ComposeProfileStringVectorOption {
    @Composable
    fun Content(
        title: String,
        initialValue: String,
        description: String,
        multiline: Boolean,
        preferredLines: Int,
        suggestionLabels: Array<String>?,
        suggestionValues: Array<String>?,
        onChanged: Consumer<String>,
        onLongClick: Runnable,
        onEditorStateChanged: (Boolean) -> Unit = {},
    ) = StringVectorOptionRow(
        title, initialValue, description, multiline, preferredLines,
        suggestionLabels, suggestionValues, onChanged, onLongClick, onEditorStateChanged,
    )

    @JvmStatic
    fun create(
        title: String,
        valueProvider: Supplier<String>,
        description: String,
        multiline: Boolean,
        preferredLines: Int,
        suggestionLabels: Array<String>?,
        suggestionValues: Array<String>?,
        onChanged: Consumer<String>,
        onLongClick: Runnable,
    ): ProfileSettingsComposeItem = StringVectorOptionItem(
        title, valueProvider, description, multiline, preferredLines, suggestionLabels, suggestionValues,
        onChanged, onLongClick,
    )

    private class StringVectorOptionItem(
        private val title: String,
        private val valueProvider: Supplier<String>,
        private val description: String,
        private val multiline: Boolean,
        private val preferredLines: Int,
        private val suggestionLabels: Array<String>?,
        private val suggestionValues: Array<String>?,
        private val onChanged: Consumer<String>,
        private val onLongClick: Runnable,
    ) : ProfileSettingsComposeItem {
        @Composable
        override fun Content(rowPosition: Int, onEditorStateChanged: (Boolean) -> Unit) {
            val initialValue = valueProvider.get().orEmpty()
            ComposeProfileStringVectorOption.Content(title, initialValue, description, multiline, preferredLines, suggestionLabels, suggestionValues, onChanged, onLongClick, onEditorStateChanged)
        }
    }

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun StringVectorOptionRow(
    title: String,
    initialValue: String,
    description: String,
    multiline: Boolean,
    preferredLines: Int,
    suggestionLabels: Array<String>?,
    suggestionValues: Array<String>?,
    onChanged: Consumer<String>,
    onLongClick: Runnable,
    onEditorStateChanged: (Boolean) -> Unit,
) {
    var currentValue by rememberSaveable(initialValue) { mutableStateOf(initialValue) }
    var dialogOpen by rememberSaveable { mutableStateOf(false) }
    var editedValues by rememberSaveable(initialValue, saver = profileStringVectorStateSaver) {
        mutableStateOf(ProfileStringVectorCodec.parse(initialValue).orEmpty())
    }
    fun closeDialog() {
        dialogOpen = false
        onEditorStateChanged(false)
    }
    val preview = ProfileStringVectorCodec.preview(currentValue)
    val removeEntryDescription = stringResource(R.string.ProfileStringVectorRemoveEntry)
    val chooseSuggestionDescription = stringResource(R.string.ProfileStringVectorChooseSuggestion)
    val editorMaxLines = if (multiline) preferredLines.coerceIn(4, 16) else 1
    val suggestions = ProfileOpenOptionResolver.choices(suggestionLabels, suggestionValues, false)

    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clip(RoundedCornerShape(12.dp))
                .combinedClickable(
                    onClick = {
                        val parsed = ProfileStringVectorCodec.parse(currentValue)
                        if (parsed != null) {
                            editedValues = parsed
                            dialogOpen = true
                            onEditorStateChanged(true)
                        }
                    },
                    onLongClick = onLongClick::run,
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = preview,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }

    if (dialogOpen) {
        AlertDialog(
            onDismissRequest = { closeDialog() },
            title = { Text(title) },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = if (multiline) 560.dp else 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (description.isNotBlank()) {
                        Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (multiline) {
                        Text(
                            stringResource(R.string.ProfileMultilineTextHint),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (editedValues.isEmpty()) {
                        Text(stringResource(R.string.ProfileStringVectorEmpty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    editedValues.forEachIndexed { index, value ->
                        var suggestionsExpanded by rememberSaveable(index) { mutableStateOf(false) }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                value = value,
                                onValueChange = { updated ->
                                    editedValues = editedValues.toMutableList().also { it[index] = updated }
                                },
                                modifier = Modifier.weight(1f),
                                label = { Text(stringResource(R.string.ProfileStringVectorEntryLabel, index + 1)) },
                                minLines = if (multiline) minOf(4, editorMaxLines) else 1,
                                maxLines = editorMaxLines,
                                textStyle = if (multiline) {
                                    TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                                } else {
                                    MaterialTheme.typography.bodyLarge
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            )
                            if (suggestions.isNotEmpty()) {
                                Box {
                                    IconButton(
                                        modifier = Modifier.semantics {
                                            contentDescription = chooseSuggestionDescription
                                        },
                                        onClick = { suggestionsExpanded = true },
                                    ) {
                                        Text("▾", color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
                                    }
                                    DropdownMenu(
                                        expanded = suggestionsExpanded,
                                        onDismissRequest = { suggestionsExpanded = false },
                                    ) {
                                        suggestions.forEach { choice ->
                                            DropdownMenuItem(
                                                text = { Text(choice.label) },
                                                onClick = {
                                                    editedValues = editedValues.toMutableList().also {
                                                        it[index] = choice.value
                                                    }
                                                    suggestionsExpanded = false
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                            IconButton(
                                modifier = Modifier.semantics { contentDescription = removeEntryDescription },
                                onClick = {
                                    editedValues = editedValues.toMutableList().also { it.removeAt(index) }
                                },
                            ) {
                                Text("−", color = MaterialTheme.colorScheme.error, fontSize = 22.sp)
                            }
                        }
                    }
                    TextButton(onClick = { editedValues = editedValues + "" }) {
                        Text(stringResource(R.string.ProfileStringVectorAddEntry))
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val serialized = ProfileStringVectorCodec.serialize(editedValues)
                        currentValue = serialized
                        closeDialog()
                        onChanged.accept(serialized)
                    },
                ) { Text(stringResource(R.string.ProfileEnumVectorSave)) }
            },
            dismissButton = {
                TextButton(onClick = { closeDialog() }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}
}
