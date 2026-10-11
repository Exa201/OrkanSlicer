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
import ru.ytkab0bp.slicebeam.config.ProfileVectorValueValidator
import java.util.function.Consumer
import java.util.function.Supplier

/** Compose editor for comma-separated Orca numeric vectors and percentage vectors. */
object ComposeProfileVectorOption {
    @Composable
    fun Content(
        title: String,
        initialValue: String,
        unit: String,
        description: String,
        minimum: Float,
        maximum: Float,
        kind: ProfileVectorValueValidator.Kind,
        onChanged: Consumer<String>,
        onLongClick: Runnable,
        nullable: Boolean = false,
        onEditorStateChanged: (Boolean) -> Unit = {},
    ) = VectorOptionRow(
        title, initialValue, unit, description, minimum, maximum, kind, onChanged, onLongClick, nullable,
        onEditorStateChanged,
    )

    @JvmOverloads
    @JvmStatic
    fun create(
        title: String,
        valueProvider: Supplier<String>,
        unit: String,
        description: String,
        minimum: Float,
        maximum: Float,
        kind: ProfileVectorValueValidator.Kind,
        onChanged: Consumer<String>,
        onLongClick: Runnable,
        nullable: Boolean = false,
    ): ProfileSettingsComposeItem = VectorOptionItem(
        title, valueProvider, unit, description, minimum, maximum, kind, onChanged, onLongClick, nullable,
    )

    private class VectorOptionItem(
        private val title: String,
        private val valueProvider: Supplier<String>,
        private val unit: String,
        private val description: String,
        private val minimum: Float,
        private val maximum: Float,
        private val kind: ProfileVectorValueValidator.Kind,
        private val onChanged: Consumer<String>,
        private val onLongClick: Runnable,
        private val nullable: Boolean,
    ) : ProfileSettingsComposeItem {
        @Composable
        override fun Content(rowPosition: Int, onEditorStateChanged: (Boolean) -> Unit) {
            val initialValue = valueProvider.get().orEmpty()
            ComposeProfileVectorOption.Content(title, initialValue, unit, description, minimum, maximum, kind, onChanged, onLongClick, nullable, onEditorStateChanged)
        }
    }

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun VectorOptionRow(
    title: String,
    initialValue: String,
    unit: String,
    description: String,
    minimum: Float,
    maximum: Float,
    kind: ProfileVectorValueValidator.Kind,
    onChanged: Consumer<String>,
    onLongClick: Runnable,
    nullable: Boolean,
    onEditorStateChanged: (Boolean) -> Unit,
) {
    var currentValue by rememberSaveable(initialValue) { mutableStateOf(initialValue) }
    var dialogOpen by rememberSaveable { mutableStateOf(false) }
    var editedValue by rememberSaveable(initialValue) { mutableStateOf(initialValue) }
    fun closeDialog() {
        dialogOpen = false
        onEditorStateChanged(false)
    }
    val inheritedLabel = stringResource(R.string.ProfileNullableVectorInherited)
    val validation = ProfileVectorValueValidator.validate(currentValue, minimum, maximum, kind, nullable)
    val rowUnit = if (kind == ProfileVectorValueValidator.Kind.PERCENTS && currentValue.contains('%')) "" else unit

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
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = ProfileVectorValueValidator.preview(currentValue, inheritedLabel),
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (rowUnit.isNotBlank()) {
                    Text(rowUnit, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    if (dialogOpen) {
        val editedValidation = ProfileVectorValueValidator.validate(editedValue, minimum, maximum, kind, nullable)
        AlertDialog(
            onDismissRequest = { closeDialog() },
            title = { Text(title) },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (description.isNotBlank()) {
                        Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(stringResource(R.string.ProfileVectorInputHint), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (nullable) {
                        Text(
                            stringResource(R.string.ProfileNullableVectorHint),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(onClick = { editedValue = ProfileVectorValueValidator.inheritAll(editedValue) }) {
                            Text(stringResource(R.string.ProfileNullableVectorInheritAll))
                        }
                    }
                    OutlinedTextField(
                        value = editedValue,
                        onValueChange = { editedValue = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.ProfileVectorInputLabel)) },
                        minLines = 2,
                        maxLines = 5,
                        isError = editedValidation != ProfileVectorValueValidator.Result.VALID,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        supportingText = {
                            when (editedValidation) {
                                ProfileVectorValueValidator.Result.EMPTY ->
                                    Text(stringResource(R.string.ProfileVectorEmpty))
                                ProfileVectorValueValidator.Result.INVALID_NUMBER ->
                                    Text(stringResource(R.string.ProfileNumericInvalidNumber))
                                ProfileVectorValueValidator.Result.OUT_OF_RANGE ->
                                    Text(stringResource(R.string.ProfileNumericOutOfRange))
                                ProfileVectorValueValidator.Result.VALID -> Unit
                            }
                        },
                    )
                    if (unit.isNotBlank()) Text(unit, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(
                    enabled = editedValidation == ProfileVectorValueValidator.Result.VALID,
                    onClick = {
                        val normalized = ProfileVectorValueValidator.normalizeForStorage(editedValue, kind, nullable)
                        currentValue = normalized
                        closeDialog()
                        onChanged.accept(normalized)
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
