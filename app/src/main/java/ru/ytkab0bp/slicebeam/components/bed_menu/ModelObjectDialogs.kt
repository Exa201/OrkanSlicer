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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.withFrameNanos
import ru.ytkab0bp.slicebeam.R

internal data class ModelObjectRow(
    val name: String,
    val tool: Int,
    val material: String,
    val color: Int,
    val selected: Boolean,
    val assignEnabled: Boolean,
    val parts: List<ModelObjectPartRow> = emptyList(),
)

internal data class ModelObjectPartRow(
    val label: String,
    val tool: Int,
    val material: String,
    val color: Int,
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
    onTransformPart: (objectIndex: Int, partIndex: Int) -> Unit,
    onEditPart: (objectIndex: Int, partIndex: Int) -> Unit,
    onApplySelection: (IntArray) -> Unit,
) {
    var groupSelectionMode by remember(rows) { mutableStateOf(false) }
    val stagedSelection = remember(rows) {
        mutableStateListOf<Int>().apply { addAll(rows.indices.filter { rows[it].selected }) }
    }
    var expandedGroups by remember(rows) {
        mutableStateOf(rows.indices.filter { rows[it].parts.size > 1 }.toSet())
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                TextButton(
                    onClick = { groupSelectionMode = !groupSelectionMode },
                    modifier = Modifier.heightIn(min = 48.dp).testTag("model-objects-group-selection-toggle"),
                ) {
                    Text(stringResource(if (groupSelectionMode) R.string.ModelMenuCancelGroupSelection else R.string.ModelMenuSelectGroup))
                }
                if (rows.any { it.parts.size > 1 }) {
                    Text(
                        stringResource(R.string.ModelObjectMultipartHelp),
                        Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("model-objects-multipart-help"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                LazyColumn(
                    Modifier.fillMaxWidth().heightIn(max = 420.dp)
                        .testTag("model-objects-dialog-rows"),
                ) {
                    itemsIndexed(rows) { index, row ->
                        val selected = index in stagedSelection
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 64.dp)
                                .background(
                                    if (selected) MaterialSelection else Color.Transparent,
                                    RoundedCornerShape(10.dp),
                                ).clickable {
                                    if (groupSelectionMode) {
                                        if (selected) stagedSelection.remove(index) else stagedSelection.add(index)
                                    } else onSelect(index)
                                }.testTag("model-object-row-$index")
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
                            if (row.parts.size > 1) TextButton(
                                onClick = {
                                    expandedGroups = if (index in expandedGroups) expandedGroups - index
                                    else expandedGroups + index
                                },
                                modifier = Modifier.heightIn(min = 48.dp)
                                    .testTag("model-object-group-toggle-$index"),
                            ) {
                                val expanded = index in expandedGroups
                                Text(
                                    stringResource(
                                        if (expanded) R.string.ModelObjectCollapseParts else R.string.ModelObjectExpandParts,
                                        row.parts.size,
                                    ),
                                )
                            }
                        }
                        if (index in expandedGroups) {
                            row.parts.forEachIndexed { partIndex, part ->
                                Column(
                                    Modifier.fillMaxWidth().padding(start = 28.dp, end = 8.dp, bottom = 4.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        .testTag("model-object-part-row-$index-$partIndex")
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                ) {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Box(Modifier.size(14.dp).background(Color(part.color), CircleShape))
                                        Column(Modifier.weight(1f)) {
                                            Text(part.label, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("T${part.tool}: ${part.material}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                    ) {
                                        TextButton(
                                            onClick = { onTransformPart(index, partIndex) },
                                            modifier = Modifier.heightIn(min = 48.dp)
                                                .testTag("model-object-transform-part-$index-$partIndex"),
                                        ) { Text(stringResource(R.string.ModelObjectPartTransform)) }
                                        TextButton(
                                            onClick = { onEditPart(index, partIndex) },
                                            modifier = Modifier.heightIn(min = 48.dp)
                                                .testTag("model-object-edit-part-$index-$partIndex"),
                                        ) { Text(stringResource(R.string.ModelObjectEditPart)) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (groupSelectionMode) TextButton(
                onClick = { onApplySelection(stagedSelection.sorted().toIntArray()) },
                modifier = Modifier.heightIn(min = 48.dp).testTag("model-objects-selection-confirm"),
            ) { Text(stringResource(R.string.ModelMenuApplySelection, stagedSelection.size)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text(cancelLabel) }
        },
    )
}

@Composable
internal fun ModelObjectPartTransformDialog(
    cancelLabel: String,
    onDismiss: () -> Unit,
    onApply: (dx: Double, dy: Double, dz: Double, sx: Double, sy: Double, sz: Double) -> Unit,
) {
    val values = remember { mutableStateListOf("0", "0", "0", "100", "100", "100") }
    val labels = listOf(
        R.string.ModelObjectPartMoveX,
        R.string.ModelObjectPartMoveY,
        R.string.ModelObjectPartMoveZ,
        R.string.ModelObjectPartScaleX,
        R.string.ModelObjectPartScaleY,
        R.string.ModelObjectPartScaleZ,
    )
    val parsed = values.map { it.trim().replace(',', '.').toDoubleOrNull() }
    val valid = parsed.all { it != null && it.isFinite() } &&
        parsed.take(3).all { kotlin.math.abs(requireNotNull(it)) <= 10_000.0 } &&
        parsed.drop(3).all { requireNotNull(it) in 1.0..1000.0 }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ModelObjectPartTransformTitle)) },
        text = {
            Column(
                Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.ModelObjectPartTransformHint))
                if (!valid) Text(
                    stringResource(R.string.ModelObjectPartTransformInvalid),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("model-object-part-transform-error"),
                )
                labels.forEachIndexed { index, label ->
                    OutlinedTextField(
                        value = values[index],
                        onValueChange = { input ->
                            values[index] = input.filter { it.isDigit() || it == '-' || it == '.' || it == ',' }.take(16)
                        },
                        label = { Text(stringResource(label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth().testTag("model-object-part-transform-field-$index"),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    val numbers = parsed.map { requireNotNull(it) }
                    onApply(numbers[0], numbers[1], numbers[2], numbers[3] / 100.0, numbers[4] / 100.0, numbers[5] / 100.0)
                },
                modifier = Modifier.heightIn(min = 48.dp).testTag("model-object-part-transform-apply"),
            ) { Text(stringResource(R.string.ModelObjectPartTransformApply)) }
        },
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
    hint: String? = null,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                hint?.let { Text(it, Modifier.padding(bottom = 8.dp)) }
                LazyColumn(
                    Modifier.fillMaxWidth().heightIn(max = 480.dp)
                        .testTag("model-choice-dialog-options"),
                ) {
                    itemsIndexed(choices) { index, label ->
                        TextButton(onClick = { onSelect(index) }, Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .testTag("model-choice-option-$index")) {
                            Text(label, Modifier.fillMaxWidth())
                        }
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
                    modifier = Modifier.fillMaxWidth()
                        .focusRequester(countFocusRequester)
                        .testTag("duplicate-model-count"),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(count.toIntOrNull()?.coerceIn(1, 50) ?: 1) },
                Modifier.heightIn(min = 48.dp).testTag("duplicate-model-confirm"),
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, Modifier.heightIn(min = 48.dp)) { Text(cancelLabel) } },
    )
}

private val MaterialSelection: Color
    @Composable get() = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
