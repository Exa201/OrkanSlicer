package ru.ytkab0bp.slicebeam.ui.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.fragment.ProfileSettingsComposeItem
import ru.ytkab0bp.slicebeam.config.ProfileNumericValueValidator
import java.util.function.Consumer
import java.util.function.Supplier
import kotlinx.coroutines.delay

/** Compose editor for one scalar Orca number; vectors and special numeric types keep their native editor. */
object ComposeProfileNumericOption {
    @Composable
    fun Content(
        title: String,
        initialValue: String,
        unit: String,
        description: String,
        minimum: Float,
        maximum: Float,
        integer: Boolean,
        percentAllowed: Boolean,
        percentRequired: Boolean,
        onChanged: Consumer<String>,
        onLongClick: Runnable,
        onEditorStateChanged: (Boolean) -> Unit = {},
    ) = NumericOptionRow(
        title, initialValue, unit, description, minimum, maximum, integer,
        percentAllowed, percentRequired, onChanged, onLongClick, onEditorStateChanged,
    )

    @JvmStatic
    fun create(
        title: String,
        valueProvider: Supplier<String>,
        unit: String,
        description: String,
        minimum: Float,
        maximum: Float,
        integer: Boolean,
        percentAllowed: Boolean,
        percentRequired: Boolean,
        onChanged: Consumer<String>,
        onLongClick: Runnable,
    ): ProfileSettingsComposeItem = NumericOptionItem(
        title, valueProvider, unit, description, minimum, maximum, integer,
        percentAllowed, percentRequired, onChanged, onLongClick,
    )

    private class NumericOptionItem(
        private val title: String,
        private val valueProvider: Supplier<String>,
        private val unit: String,
        private val description: String,
        private val minimum: Float,
        private val maximum: Float,
        private val integer: Boolean,
        private val percentAllowed: Boolean,
        private val percentRequired: Boolean,
        private val onChanged: Consumer<String>,
        private val onLongClick: Runnable,
    ) : ProfileSettingsComposeItem {
        @Composable
        override fun Content(rowPosition: Int, onEditorStateChanged: (Boolean) -> Unit) {
            val initialValue = valueProvider.get()
            ComposeProfileNumericOption.Content(title, initialValue, unit, description, minimum, maximum, integer, percentAllowed, percentRequired, onChanged, onLongClick, onEditorStateChanged)
        }
    }

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun NumericOptionRow(
    title: String,
    initialValue: String,
    unit: String,
    description: String,
    minimum: Float,
    maximum: Float,
    integer: Boolean,
    percentAllowed: Boolean,
    percentRequired: Boolean,
    onChanged: Consumer<String>,
    onLongClick: Runnable,
    onEditorStateChanged: (Boolean) -> Unit,
) {
    fun canonicalValue(value: String): String =
        if (percentRequired && !value.trim().endsWith("%")) "${value.trim()}%" else value.trim()
    fun editableValue(value: String): String =
        if (percentAllowed && value.trim().endsWith("%")) value.trim().dropLast(1).trim() else value.trim()

    var currentValue by rememberSaveable(initialValue) { mutableStateOf(canonicalValue(initialValue)) }
    var dialogOpen by rememberSaveable { mutableStateOf(false) }
    var editedValue by rememberSaveable(initialValue) { mutableStateOf(editableValue(initialValue)) }
    var percentageSelected by rememberSaveable(initialValue) {
        mutableStateOf(percentRequired || initialValue.trim().endsWith("%"))
    }
    fun closeDialog() {
        dialogOpen = false
        onEditorStateChanged(false)
    }
    val inputFocus = remember { FocusRequester() }

    LaunchedEffect(dialogOpen) {
        if (dialogOpen) {
            delay(120)
            inputFocus.requestFocus()
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
                        editedValue = editableValue(currentValue)
                        percentageSelected = percentRequired || currentValue.endsWith("%")
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
                    text = currentValue,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val displayedUnit = if (currentValue.endsWith("%")) "" else unit
                if (displayedUnit.isNotBlank()) {
                    Text(
                        text = displayedUnit,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }

    if (dialogOpen) {
        val validation = ProfileNumericValueValidator.validate(editedValue, minimum, maximum, integer)
        val keyboardType = if (integer) KeyboardType.Number else KeyboardType.Decimal
        val toggleSignDescription = stringResource(R.string.ProfileNumericToggleSign)
        AlertDialog(
            onDismissRequest = { closeDialog() },
            title = { Text(title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (description.isNotBlank()) {
                        Text(
                            description,
                            modifier = Modifier.heightIn(max = 128.dp).verticalScroll(rememberScrollState()),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (percentAllowed && !percentRequired) {
                        TextButton(onClick = { percentageSelected = !percentageSelected }) {
                            Text(stringResource(
                                if (percentageSelected) R.string.ProfileNumericAsPercent
                                else R.string.ProfileNumericAsAbsolute,
                            ))
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = editedValue,
                            onValueChange = { editedValue = it },
                            modifier = Modifier.weight(1f).focusRequester(inputFocus),
                            label = { Text(stringResource(R.string.ProfileNumericInputLabel)) },
                            singleLine = true,
                            isError = validation != ProfileNumericValueValidator.Result.VALID,
                            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
                            supportingText = {
                                when (validation) {
                                    ProfileNumericValueValidator.Result.INVALID_NUMBER ->
                                        Text(stringResource(R.string.ProfileNumericInvalidNumber))
                                    ProfileNumericValueValidator.Result.OUT_OF_RANGE ->
                                        Text(stringResource(R.string.ProfileNumericOutOfRange))
                                    ProfileNumericValueValidator.Result.VALID -> Unit
                                }
                            },
                        )
                        if (minimum < 0f) {
                            TextButton(
                                onClick = { editedValue = ProfileNumericValueValidator.toggleSign(editedValue) },
                                modifier = Modifier.semantics { contentDescription = toggleSignDescription },
                            ) { Text("±", fontSize = 20.sp) }
                        }
                    }
                    val inputUnit = if (percentageSelected) "%" else unit
                    if (inputUnit.isNotBlank()) {
                        Text(inputUnit, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = validation == ProfileNumericValueValidator.Result.VALID,
                    onClick = {
                        val normalized = ProfileNumericValueValidator.normalizeForStorage(
                            editedValue, integer, percentageSelected,
                        )
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
