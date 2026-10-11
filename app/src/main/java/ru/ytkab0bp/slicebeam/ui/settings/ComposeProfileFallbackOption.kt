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
import ru.ytkab0bp.slicebeam.config.ProfileFallbackValueValidator
import ru.ytkab0bp.slicebeam.config.ProfileOpenOptionResolver
import ru.ytkab0bp.slicebeam.slic3r.ConfigOptionDef
import java.util.function.Consumer
import java.util.function.Supplier

/** Compose editor for Orca profile values that do not fit a specialized option row. */
object ComposeProfileFallbackOption {
    @Composable
    fun Content(
        title: String,
        fullTitle: String,
        description: String,
        definition: ConfigOptionDef,
        initialValue: String,
        choices: List<ProfileOpenOptionResolver.Choice>,
        onChanged: Consumer<String>,
        onLongClick: Runnable,
        onEditorStateChanged: (Boolean) -> Unit = {},
    ) = FallbackOptionRow(
        title, fullTitle, description, definition, initialValue, choices, onChanged, onLongClick,
        onEditorStateChanged,
    )

    @JvmStatic
    fun create(
        title: String,
        fullTitle: String,
        description: String,
        definition: ConfigOptionDef,
        valueProvider: Supplier<String>,
        choices: List<ProfileOpenOptionResolver.Choice>,
        onChanged: Consumer<String>,
        onLongClick: Runnable,
    ): ProfileSettingsComposeItem = FallbackOptionItem(
        title, fullTitle, description, definition, valueProvider, choices.toList(), onChanged, onLongClick,
    )

    private class FallbackOptionItem(
        private val title: String,
        private val fullTitle: String,
        private val description: String,
        private val definition: ConfigOptionDef,
        private val valueProvider: Supplier<String>,
        private val choices: List<ProfileOpenOptionResolver.Choice>,
        private val onChanged: Consumer<String>,
        private val onLongClick: Runnable,
    ) : ProfileSettingsComposeItem {
        @Composable
        override fun Content(rowPosition: Int, onEditorStateChanged: (Boolean) -> Unit) {
            val initialValue = valueProvider.get().orEmpty()
            ComposeProfileFallbackOption.Content(title, fullTitle, description, definition, initialValue, choices, onChanged, onLongClick, onEditorStateChanged)
        }
    }

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun FallbackOptionRow(
    title: String,
    fullTitle: String,
    description: String,
    definition: ConfigOptionDef,
    initialValue: String,
    choices: List<ProfileOpenOptionResolver.Choice>,
    onChanged: Consumer<String>,
    onLongClick: Runnable,
    onEditorStateChanged: (Boolean) -> Unit,
) {
    var currentValue by rememberSaveable(initialValue) { mutableStateOf(initialValue) }
    var editedValue by rememberSaveable(initialValue) { mutableStateOf(initialValue) }
    var dialogOpen by rememberSaveable { mutableStateOf(false) }
    fun closeDialog() {
        dialogOpen = false
        onEditorStateChanged(false)
    }
    val isEnum = definition.type == ConfigOptionDef.ConfigOptionType.ENUM
    val hasEnumChoices = isEnum && choices.isNotEmpty()
    val editable = !isEnum || hasEnumChoices
    val codeOnly = definition.key.endsWith("_gcode")
    val stringValue = definition.type == ConfigOptionDef.ConfigOptionType.STRING ||
        definition.type == ConfigOptionDef.ConfigOptionType.STRINGS
    val displayValue = when {
        isEnum -> ProfileOpenOptionResolver.displayValue(
            currentValue,
            choices.ifEmpty { emptyList() },
        ).let { resolved ->
            if (resolved == currentValue) {
                val legacyIndex = currentValue.toIntOrNull()
                choices.getOrNull(legacyIndex ?: -1)?.label ?: resolved
            } else resolved
        }
        stringValue -> currentValue.trim().ifBlank { "—" }
        else -> currentValue.ifBlank { "—" }
    }
    val validation = ProfileFallbackValueValidator.validate(definition, editedValue)
    val inputType = when (definition.type) {
        ConfigOptionDef.ConfigOptionType.INT -> KeyboardType.Number
        ConfigOptionDef.ConfigOptionType.FLOAT,
        ConfigOptionDef.ConfigOptionType.PERCENT,
        ConfigOptionDef.ConfigOptionType.FLOAT_OR_PERCENT -> KeyboardType.Decimal
        else -> KeyboardType.Text
    }
    val multiline = definition.multiline
    val maxLines = if (multiline) definition.height.takeIf { it > 0 }?.coerceAtMost(16) ?: 8 else 1

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
                        if (editable) {
                            editedValue = currentValue
                            dialogOpen = true
                            onEditorStateChanged(true)
                        }
                    },
                    onLongClick = onLongClick::run,
                )
                .semantics {
                    contentDescription = listOfNotNull(
                        title.takeIf { it.isNotBlank() },
                        displayValue,
                    ).joinToString(", ")
                }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!codeOnly && title.isNotBlank()) {
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = displayValue,
                modifier = if (codeOnly || title.isBlank()) Modifier.weight(1f) else Modifier,
                fontSize = if (stringValue) 14.sp else 15.sp,
                color = MaterialTheme.colorScheme.primary,
                style = if (codeOnly) TextStyle(fontFamily = FontFamily.Monospace) else MaterialTheme.typography.bodyMedium,
                maxLines = if (codeOnly || multiline) 3 else 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }

    if (dialogOpen && hasEnumChoices) {
        val selectedIndex = choices.indexOfFirst { it.value == currentValue }.takeIf { it >= 0 }
            ?: currentValue.toIntOrNull()?.takeIf { it in choices.indices } ?: -1
        AlertDialog(
            onDismissRequest = { closeDialog() },
            title = { Text(fullTitle.ifBlank { title }) },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    if (description.isNotBlank()) {
                        Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    choices.forEachIndexed { index, choice ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .combinedClickable(onClick = {
                                    currentValue = choice.value
                                    closeDialog()
                                    onChanged.accept(choice.value)
                                })
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = selectedIndex == index,
                                onClick = {
                                    currentValue = choice.value
                                    closeDialog()
                                    onChanged.accept(choice.value)
                                },
                            )
                            Text(
                                text = choice.label,
                                modifier = Modifier.weight(1f).padding(start = 8.dp),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { closeDialog() }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    } else if (dialogOpen) {
        val valid = validation == ProfileFallbackValueValidator.Result.VALID
        val validationMessage = when (validation) {
            ProfileFallbackValueValidator.Result.VALID -> null
            ProfileFallbackValueValidator.Result.OUT_OF_RANGE -> R.string.ProfileOpenOptionOutOfRange
            ProfileFallbackValueValidator.Result.INVALID_VECTOR -> R.string.ProfileVectorInputHint
            ProfileFallbackValueValidator.Result.INVALID_NUMBER -> R.string.ProfileOpenOptionInvalidNumber
        }
        AlertDialog(
            onDismissRequest = { closeDialog() },
            title = { Text(fullTitle.ifBlank { title }) },
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
                    OutlinedTextField(
                        value = editedValue,
                        onValueChange = { editedValue = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(
                            if (stringValue) R.string.ProfileStringInputLabel else R.string.ProfileOpenOptionInputLabel,
                        )) },
                        singleLine = !multiline,
                        minLines = if (multiline) minOf(4, maxLines) else 1,
                        maxLines = maxLines,
                        isError = !valid,
                        supportingText = validationMessage?.let { messageId ->
                            { Text(stringResource(messageId)) }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = inputType),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = valid,
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
