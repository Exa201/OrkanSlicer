package ru.ytkab0bp.slicebeam.components.bed_menu

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.withFrameNanos

internal data class ModelObjectRow(
    val name: String,
    val tool: Int,
    val material: String,
    val color: Int,
    val selected: Boolean,
    val assignEnabled: Boolean,
)

@Composable
internal fun ModelObjectsDialog(
    title: String,
    cancelLabel: String,
    assignLabel: String,
    rows: List<ModelObjectRow>,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
    onAssign: (Int) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 480.dp)) {
                itemsIndexed(rows) { index, row ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 64.dp)
                            .background(
                                if (row.selected) MaterialSelection else Color.Transparent,
                                RoundedCornerShape(10.dp),
                            ).clickable { onSelect(index) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(Modifier.size(18.dp).background(Color(row.color), CircleShape))
                        Column(Modifier.weight(1f)) {
                            Text(row.name, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("T${row.tool}: ${row.material} · #${row.color.toUInt().toString(16).padStart(6, '0').takeLast(6).uppercase()}",
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        TextButton(onClick = { onAssign(index) }, enabled = row.assignEnabled,
                            modifier = Modifier.heightIn(min = 48.dp)) {
                            Text(assignLabel)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text(cancelLabel) }
        },
    )
}

@Composable
internal fun ModelChoiceDialog(
    title: String,
    cancelLabel: String,
    choices: List<String>,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 480.dp)) {
                itemsIndexed(choices) { index, label ->
                    TextButton(onClick = { onSelect(index) }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text(label, Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss, Modifier.heightIn(min = 48.dp)) { Text(cancelLabel) } },
    )
}

@Composable
internal fun DuplicateModelDialog(
    title: String,
    hint: String,
    confirmLabel: String,
    cancelLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var count by remember { mutableStateOf("1") }
    val countFocusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        withFrameNanos { }
        countFocusRequester.requestFocus()
        keyboard?.show()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(hint)
                OutlinedTextField(
                    value = count,
                    onValueChange = { input -> count = input.filter(Char::isDigit).take(2) },
                    label = { Text(title) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().focusRequester(countFocusRequester),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(count.toIntOrNull()?.coerceIn(1, 50) ?: 1) }, Modifier.heightIn(min = 48.dp)) {
                Text(confirmLabel)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, Modifier.heightIn(min = 48.dp)) { Text(cancelLabel) } },
    )
}

private val MaterialSelection: Color
    @Composable get() = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
