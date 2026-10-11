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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.fragment.ProfileSettingsComposeItem
import ru.ytkab0bp.slicebeam.config.ProfileEnumOptionResolver
import java.util.LinkedHashSet
import java.util.function.Consumer
import java.util.function.Supplier

private val profileStringSetStateSaver = listSaver<MutableState<LinkedHashSet<String>>, String>(
    save = { it.value.toList() },
    restore = { mutableStateOf(LinkedHashSet(it)) },
)

/** Multi-select UI for Orca enum vectors; schema values and labels still come from the native core. */
object ComposeProfileEnumVectorOption {
    @Composable
    fun Content(
        title: String,
        initialValue: String,
        labels: Array<String>,
        values: Array<String>,
        onChanged: Consumer<String>,
        onLongClick: Runnable,
        onEditorStateChanged: (Boolean) -> Unit = {},
    ) = EnumVectorOptionRow(title, initialValue, labels, values, onChanged, onLongClick, onEditorStateChanged)

    @JvmStatic
    fun create(
        title: String,
        valueProvider: Supplier<String>,
        labels: Array<String>,
        values: Array<String>,
        onChanged: Consumer<String>,
        onLongClick: Runnable,
    ): ProfileSettingsComposeItem = EnumVectorOptionItem(
        title, valueProvider, labels.copyOf(), values.copyOf(), onChanged, onLongClick,
    )

    private class EnumVectorOptionItem(
        private val title: String,
        private val valueProvider: Supplier<String>,
        private val labels: Array<String>,
        private val values: Array<String>,
        private val onChanged: Consumer<String>,
        private val onLongClick: Runnable,
    ) : ProfileSettingsComposeItem {
        @Composable
        override fun Content(rowPosition: Int, onEditorStateChanged: (Boolean) -> Unit) {
            val initialValue = valueProvider.get().orEmpty()
            ComposeProfileEnumVectorOption.Content(title, initialValue, labels, values, onChanged, onLongClick, onEditorStateChanged)
        }
    }

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun EnumVectorOptionRow(
    title: String,
    initialValue: String,
    labels: Array<String>,
    values: Array<String>,
    onChanged: Consumer<String>,
    onLongClick: Runnable,
    onEditorStateChanged: (Boolean) -> Unit,
) {
    val optionCount = minOf(labels.size, values.size)
    val availableValues = values.take(optionCount).toTypedArray()
    var currentValue by rememberSaveable(initialValue) { mutableStateOf(initialValue) }
    var dialogOpen by rememberSaveable { mutableStateOf(false) }
    var editedValues by rememberSaveable(initialValue, saver = profileStringSetStateSaver) {
        mutableStateOf(ProfileEnumOptionResolver.parseValues(initialValue))
    }
    fun closeDialog() {
        dialogOpen = false
        onEditorStateChanged(false)
    }

    val currentSelections = ProfileEnumOptionResolver.parseValues(currentValue)
    val knownLabels = (0 until optionCount)
        .filter { values[it] in currentSelections }
        .map { labels[it] }
    val unknownValues = currentSelections.filter { it !in availableValues }
    val summaryValues = knownLabels + unknownValues
    val summary = when {
        summaryValues.isEmpty() -> stringResource(R.string.ProfileEnumVectorNoneSelected)
        summaryValues.size <= 2 -> summaryValues.joinToString(" · ")
        else -> summaryValues.take(2).joinToString(" · ") + " …"
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
                        editedValues = ProfileEnumOptionResolver.parseValues(currentValue)
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
                title,
                modifier = Modifier.weight(1f),
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                summary,
                modifier = Modifier.weight(1f),
                fontSize = 15.sp,
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
                    modifier = Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        stringResource(R.string.ProfileEnumVectorSelectHint),
                        modifier = Modifier.padding(bottom = 6.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    for (index in 0 until optionCount) {
                        val optionValue = values[index]
                        val checked = optionValue in editedValues
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .combinedClickable(
                                    onClick = {
                                        val updated = LinkedHashSet(editedValues)
                                        if (checked) updated.remove(optionValue) else updated.add(optionValue)
                                        editedValues = updated
                                    },
                                    onLongClick = onLongClick::run,
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { next ->
                                    val updated = LinkedHashSet(editedValues)
                                    if (next) updated.add(optionValue) else updated.remove(optionValue)
                                    editedValues = updated
                                },
                            )
                            Text(
                                labels[index],
                                modifier = Modifier.weight(1f).padding(start = 8.dp),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val serialized = ProfileEnumOptionResolver.serializeSelection(
                            availableValues, editedValues, currentValue,
                        )
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
