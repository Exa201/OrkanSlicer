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
import ru.ytkab0bp.slicebeam.config.ProfilePointValueValidator
import java.util.function.Consumer
import java.util.function.Supplier

/** Compose editor for Orca POINT, POINT3 and POINTS options. */
object ComposeProfilePointOption {
    @Composable
    fun Content(
        title: String,
        initialValue: String,
        description: String,
        kind: ProfilePointValueValidator.Kind,
        allowEmpty: Boolean,
        onChanged: Consumer<String>,
        onLongClick: Runnable,
        onEditorStateChanged: (Boolean) -> Unit = {},
    ) = PointOptionRow(title, initialValue, description, kind, allowEmpty, onChanged, onLongClick, onEditorStateChanged)

    @JvmStatic
    fun create(
        title: String,
        valueProvider: Supplier<String>,
        description: String,
        kind: ProfilePointValueValidator.Kind,
        allowEmpty: Boolean,
        onChanged: Consumer<String>,
        onLongClick: Runnable,
    ): ProfileSettingsComposeItem = PointOptionItem(
        title, valueProvider, description, kind, allowEmpty, onChanged, onLongClick,
    )

    private class PointOptionItem(
        private val title: String,
        private val valueProvider: Supplier<String>,
        private val description: String,
        private val kind: ProfilePointValueValidator.Kind,
        private val allowEmpty: Boolean,
        private val onChanged: Consumer<String>,
        private val onLongClick: Runnable,
    ) : ProfileSettingsComposeItem {
        @Composable
        override fun Content(rowPosition: Int, onEditorStateChanged: (Boolean) -> Unit) {
            val initialValue = valueProvider.get().orEmpty()
            ComposeProfilePointOption.Content(title, initialValue, description, kind, allowEmpty, onChanged, onLongClick, onEditorStateChanged)
        }
    }

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun PointOptionRow(
    title: String,
    initialValue: String,
    description: String,
    kind: ProfilePointValueValidator.Kind,
    allowEmpty: Boolean,
    onChanged: Consumer<String>,
    onLongClick: Runnable,
    onEditorStateChanged: (Boolean) -> Unit,
) {
    var currentValue by rememberSaveable(initialValue) { mutableStateOf(initialValue) }
    var dialogOpen by rememberSaveable { mutableStateOf(false) }
    var editedValue by rememberSaveable(initialValue) {
        mutableStateOf(ProfilePointValueValidator.formatForEditor(initialValue, kind))
    }
    fun closeDialog() {
        dialogOpen = false
        onEditorStateChanged(false)
    }
    val preview = ProfilePointValueValidator.preview(currentValue, kind)

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
                        editedValue = ProfilePointValueValidator.formatForEditor(currentValue, kind)
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
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }

    if (dialogOpen) {
        val editedValidation = if (!allowEmpty && editedValue.isBlank()) {
            ProfilePointValueValidator.Result.EMPTY
        } else {
            ProfilePointValueValidator.validate(editedValue, kind)
        }
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
                    Text(stringResource(R.string.ProfilePointInputHint), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = editedValue,
                        onValueChange = { editedValue = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.ProfilePointInputLabel)) },
                        minLines = if (kind == ProfilePointValueValidator.Kind.POINTS) 4 else 1,
                        maxLines = 8,
                        isError = editedValidation != ProfilePointValueValidator.Result.VALID,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        supportingText = {
                            when (editedValidation) {
                                ProfilePointValueValidator.Result.EMPTY ->
                                    Text(stringResource(R.string.ProfilePointEmpty))
                                ProfilePointValueValidator.Result.INVALID_COORDINATE ->
                                    Text(stringResource(R.string.ProfilePointInvalidCoordinate))
                                ProfilePointValueValidator.Result.VALID -> Unit
                            }
                        },
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = editedValidation == ProfilePointValueValidator.Result.VALID,
                    onClick = {
                        val normalized = ProfilePointValueValidator.normalizeForStorage(editedValue, kind)
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
