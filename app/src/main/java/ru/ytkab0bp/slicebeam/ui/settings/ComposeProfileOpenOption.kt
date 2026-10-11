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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.fragment.ProfileSettingsComposeItem
import ru.ytkab0bp.slicebeam.config.ProfileNumericValueValidator
import ru.ytkab0bp.slicebeam.config.ProfileOpenOptionResolver
import java.util.function.Consumer
import java.util.function.Supplier

/** Orca open enums show native suggestions but keep arbitrary, validated values editable. */
object ComposeProfileOpenOption {
    @Composable
    fun Content(
        title: String,
        initialValue: String,
        description: String,
        unit: String,
        choices: List<ProfileOpenOptionResolver.Choice>,
        integerValue: Boolean,
        percentRequired: Boolean,
        minimum: Float,
        maximum: Float,
        onChanged: Consumer<String>,
        onLongClick: Runnable,
        onEditorStateChanged: (Boolean) -> Unit = {},
    ) = OpenOptionRow(
        title, initialValue, description, unit, choices, integerValue, percentRequired,
        minimum, maximum, onChanged, onLongClick, onEditorStateChanged,
    )

    @JvmStatic
    fun create(
        title: String,
        valueProvider: Supplier<String>,
        description: String,
        unit: String,
        labels: Array<String>?,
        values: Array<String>?,
        integerEnum: Boolean,
        integerValue: Boolean,
        percentRequired: Boolean,
        minimum: Float,
        maximum: Float,
        onChanged: Consumer<String>,
        onLongClick: Runnable,
    ): ProfileSettingsComposeItem = OpenOptionItem(
        title, valueProvider, description, unit, labels?.copyOf(), values?.copyOf(),
        integerEnum, integerValue, percentRequired, minimum, maximum, onChanged, onLongClick,
    )

    private class OpenOptionItem(
        private val title: String,
        private val valueProvider: Supplier<String>,
        private val description: String,
        private val unit: String,
        private val labels: Array<String>?,
        private val values: Array<String>?,
        private val integerEnum: Boolean,
        private val integerValue: Boolean,
        private val percentRequired: Boolean,
        private val minimum: Float,
        private val maximum: Float,
        private val onChanged: Consumer<String>,
        private val onLongClick: Runnable,
    ) : ProfileSettingsComposeItem {
        @Composable
        override fun Content(rowPosition: Int, onEditorStateChanged: (Boolean) -> Unit) {
            val initialValue = valueProvider.get().orEmpty()
                        val choices = ProfileOpenOptionResolver.choices(labels, values, integerEnum)
            ComposeProfileOpenOption.Content(title, initialValue, description, unit, choices, integerValue, percentRequired, minimum, maximum, onChanged, onLongClick, onEditorStateChanged)
        }
    }

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun OpenOptionRow(
    title: String,
    initialValue: String,
    description: String,
    unit: String,
    choices: List<ProfileOpenOptionResolver.Choice>,
    integerValue: Boolean,
    percentRequired: Boolean,
    minimum: Float,
    maximum: Float,
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
    val currentLabel = ProfileOpenOptionResolver.displayValue(currentValue, choices)

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
                text = currentLabel.ifBlank { "—" },
                modifier = Modifier.weight(1f),
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }

    val hasPercent = editedValue.trim().endsWith("%")
    val validation = when {
        percentRequired && !hasPercent -> ProfileNumericValueValidator.Result.INVALID_NUMBER
        else -> ProfileNumericValueValidator.validate(editedValue, minimum, maximum, integerValue)
    }
    val valid = validation == ProfileNumericValueValidator.Result.VALID

    if (dialogOpen) {
        AlertDialog(
            onDismissRequest = { closeDialog() },
            title = { Text(title) },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (description.isNotBlank()) {
                        Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        stringResource(R.string.ProfileOpenOptionHint),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = editedValue,
                        onValueChange = { editedValue = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.ProfileOpenOptionInputLabel)) },
                        suffix = if (unit.isNotBlank() && !hasPercent) ({ Text(unit) }) else null,
                        singleLine = true,
                        isError = !valid,
                        supportingText = if (valid) null else {
                            {
                                Text(stringResource(
                                    if (validation == ProfileNumericValueValidator.Result.OUT_OF_RANGE)
                                        R.string.ProfileOpenOptionOutOfRange
                                    else R.string.ProfileOpenOptionInvalidNumber,
                                ))
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (integerValue) KeyboardType.Number else KeyboardType.Decimal,
                        ),
                    )
                    if (choices.isNotEmpty()) {
                        Text(
                            stringResource(R.string.ProfileOpenOptionSuggestions),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelLarge,
                        )
                        for (choice in choices) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .combinedClickable(onClick = { editedValue = choice.value })
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = editedValue.trim() == choice.value,
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
                    enabled = valid,
                    onClick = {
                        val normalized = ProfileNumericValueValidator.normalizeForStorage(
                            editedValue, integerValue, percentRequired || hasPercent,
                        )
                        currentValue = normalized
                        closeDialog()
                        onChanged.accept(normalized)
                    },
                ) {
                    Text(stringResource(android.R.string.ok))
                }
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
