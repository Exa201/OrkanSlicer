package ru.ytkab0bp.slicebeam.ui.settings

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.fragment.ProfileSettingsComposeItem
import ru.ytkab0bp.slicebeam.config.ProfileOpenOptionResolver
import java.util.function.Consumer
import java.util.function.Supplier

/** Compose editor for Orca scalar strings, including the multi-line G-code options. */
object ComposeProfileStringOption {
    @Composable
    fun Content(
        title: String,
        initialValue: String,
        description: String,
        password: Boolean,
        multiline: Boolean,
        preferredLines: Int,
        suggestionLabels: Array<String>?,
        suggestionValues: Array<String>?,
        onChanged: Consumer<String>,
        onLongClick: Runnable,
        onEditorStateChanged: (Boolean) -> Unit = {},
    ) = StringOptionRow(
        title, initialValue, description, password, multiline, preferredLines,
        suggestionLabels, suggestionValues, onChanged, onLongClick, onEditorStateChanged,
    )

    @JvmStatic
    fun create(
        title: String,
        valueProvider: Supplier<String>,
        description: String,
        password: Boolean,
        multiline: Boolean,
        preferredLines: Int,
        suggestionLabels: Array<String>?,
        suggestionValues: Array<String>?,
        onChanged: Consumer<String>,
        onLongClick: Runnable,
    ): ProfileSettingsComposeItem = StringOptionItem(
        title, valueProvider, description, password, multiline, preferredLines, suggestionLabels,
        suggestionValues, onChanged, onLongClick,
    )

    private class StringOptionItem(
        private val title: String,
        private val valueProvider: Supplier<String>,
        private val description: String,
        private val password: Boolean,
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
            ComposeProfileStringOption.Content(title, initialValue, description, password, multiline, preferredLines, suggestionLabels, suggestionValues, onChanged, onLongClick, onEditorStateChanged)
        }
    }

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun StringOptionRow(
    title: String,
    initialValue: String,
    description: String,
    password: Boolean,
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
    var editedValue by rememberSaveable(initialValue) { mutableStateOf(initialValue) }
    var revealPassword by rememberSaveable { mutableStateOf(false) }
    fun closeDialog() {
        dialogOpen = false
        onEditorStateChanged(false)
    }
    val editorMaxLines = if (multiline && !password) preferredLines.coerceIn(5, 18) else 1
    val suggestions = ProfileOpenOptionResolver.choices(suggestionLabels, suggestionValues, false)
    val preview = when {
        currentValue.isBlank() -> "—"
        password -> "••••••"
        else -> currentValue.replace(Regex("\\s+"), " ").take(48).let {
            if (currentValue.length > 48) "$it …" else it
        }
    }

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
                        editedValue = currentValue
                        revealPassword = false
                        dialogOpen = true
                        onEditorStateChanged(true)
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
                maxLines = 1,
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
                    modifier = Modifier.heightIn(max = if (multiline) 560.dp else 380.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (description.isNotBlank()) {
                        Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (multiline && !password) {
                        Text(
                            stringResource(R.string.ProfileMultilineTextHint),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    OutlinedTextField(
                        value = editedValue,
                        onValueChange = { editedValue = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.ProfileStringInputLabel)) },
                        singleLine = !multiline || password,
                        minLines = if (multiline && !password) minOf(6, editorMaxLines) else 1,
                        maxLines = editorMaxLines,
                        textStyle = if (multiline && !password) {
                            TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                        } else {
                            MaterialTheme.typography.bodyLarge
                        },
                        visualTransformation = if (password && !revealPassword) {
                            PasswordVisualTransformation()
                        } else {
                            VisualTransformation.None
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (password) KeyboardType.Password else KeyboardType.Text,
                        ),
                        trailingIcon = if (password) {
                            {
                                TextButton(onClick = { revealPassword = !revealPassword }) {
                                    Text(stringResource(
                                        if (revealPassword) R.string.ProfileStringHidePassword
                                        else R.string.ProfileStringShowPassword,
                                    ))
                                }
                            }
                        } else {
                            null
                        },
                    )
                    if (suggestions.isNotEmpty() && !multiline && !password) {
                        Text(
                            stringResource(R.string.ProfileOpenOptionSuggestions),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelLarge,
                        )
                        for (choice in suggestions) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .combinedClickable(onClick = { editedValue = choice.value })
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = editedValue == choice.value,
                                    onClick = { editedValue = choice.value },
                                )
                                Text(
                                    text = choice.label,
                                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        currentValue = editedValue
                        closeDialog()
                        onChanged.accept(editedValue)
                    },
                ) { Text(stringResource(android.R.string.ok)) }
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
