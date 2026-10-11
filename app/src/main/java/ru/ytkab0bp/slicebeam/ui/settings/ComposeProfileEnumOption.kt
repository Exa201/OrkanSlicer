package ru.ytkab0bp.slicebeam.ui.settings

import ru.ytkab0bp.slicebeam.fragment.ProfileSettingsComposeItem

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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.ytkab0bp.slicebeam.config.ProfileEnumOptionResolver
import java.util.function.Consumer
import java.util.function.Supplier

/** Compose selector for one Orca enum while values and labels still come from the native schema. */
object ComposeProfileEnumOption {
    @Composable
    fun Content(
        title: String,
        initialValue: String,
        labels: Array<String>,
        values: Array<String>,
        onChanged: Consumer<String>,
        onLongClick: Runnable,
        onEditorStateChanged: (Boolean) -> Unit = {},
    ) = EnumOptionRow(title, initialValue, labels, values, onChanged, onLongClick, onEditorStateChanged)

    @JvmStatic
    fun create(
        title: String,
        valueProvider: Supplier<String>,
        labels: Array<String>,
        values: Array<String>,
        onChanged: Consumer<String>,
        onLongClick: Runnable,
    ): ProfileSettingsComposeItem = EnumOptionItem(
        title, valueProvider, labels.copyOf(), values.copyOf(), onChanged, onLongClick,
    )

    private class EnumOptionItem(
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
            ComposeProfileEnumOption.Content(title, initialValue, labels, values, onChanged, onLongClick, onEditorStateChanged)
        }
    }

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun EnumOptionRow(
    title: String,
    initialValue: String,
    labels: Array<String>,
    values: Array<String>,
    onChanged: Consumer<String>,
    onLongClick: Runnable,
    onEditorStateChanged: (Boolean) -> Unit,
) {
    val optionCount = minOf(labels.size, values.size)
    var currentValue by rememberSaveable(initialValue) { mutableStateOf(initialValue) }
    var dialogOpen by rememberSaveable { mutableStateOf(false) }
    fun closeDialog() {
        dialogOpen = false
        onEditorStateChanged(false)
    }
    val selected = ProfileEnumOptionResolver.selectedIndex(currentValue, values, optionCount)
    val currentLabel = labels.getOrNull(selected) ?: currentValue

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
                text = currentLabel,
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
                    modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    for (index in 0 until optionCount) {
                        val value = values[index]
                        val label = labels[index]
                        val isSelected = index == selected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .combinedClickable(
                                    onClick = {
                                        currentValue = value
                                        closeDialog()
                                        onChanged.accept(value)
                                    },
                                    onLongClick = onLongClick::run,
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    currentValue = value
                                    closeDialog()
                                    onChanged.accept(value)
                                },
                            )
                            Text(
                                text = label,
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
                    Text(androidx.compose.ui.res.stringResource(android.R.string.cancel))
                }
            },
        )
    }
}
}
