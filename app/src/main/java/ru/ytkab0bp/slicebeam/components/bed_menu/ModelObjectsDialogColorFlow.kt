package ru.ytkab0bp.slicebeam.components.bed_menu

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.ui.ColorFlow
import ru.ytkab0bp.slicebeam.ui.OrkanDesignTokens

/** Adaptive, searchable object sheet. Actions always receive source model indices. */
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
    val stagedSelection = remember(rows) { mutableStateListOf<Int>().apply { addAll(rows.indices.filter { rows[it].selected }) } }
    var expandedGroups by remember(rows) { mutableStateOf(rows.indices.filter { rows[it].parts.size > 1 }.toSet()) }
    var search by remember(rows) { mutableStateOf("") }
    val matchingIndices = remember(rows, search) { ModelObjectSearchPolicy.matchingIndices(rows, search) }
    val configuration = LocalConfiguration.current
    val landscape = configuration.screenWidthDp > configuration.screenHeightDp

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(if (landscape) 0.94f else 0.96f)
                .widthIn(max = if (landscape) 960.dp else 760.dp).fillMaxHeight(0.92f),
            shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 8.dp,
        ) {
            Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                        Text("${matchingIndices.size} / ${rows.size}", color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text(cancelLabel) }
                }
                OutlinedTextField(value = search, onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 4.dp).testTag("model-objects-search"),
                    singleLine = true, label = { Text(stringResource(R.string.SettingsSearchHint)) })
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { groupSelectionMode = !groupSelectionMode },
                        modifier = Modifier.heightIn(min = 48.dp).testTag("model-objects-group-selection-toggle")) {
                        Text(stringResource(if (groupSelectionMode) R.string.ModelMenuCancelGroupSelection else R.string.ModelMenuSelectGroup),
                            color = ColorFlow.model)
                    }
                    if (groupSelectionMode) Text("${stagedSelection.size}", color = ColorFlow.model)
                }
                if (rows.any { it.parts.size > 1 }) Text(stringResource(R.string.ModelObjectMultipartHelp),
                    Modifier.padding(horizontal = 8.dp, vertical = 4.dp).testTag("model-objects-multipart-help"),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyColumn(Modifier.fillMaxWidth().weight(1f).heightIn(min = 120.dp).testTag("model-objects-dialog-rows"),
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    itemsIndexed(matchingIndices, key = { _, originalIndex -> originalIndex }) { _, index ->
                        val row = rows[index]
                        val selected = index in stagedSelection
                        Column {
                            Column(Modifier.fillMaxWidth()
                                .background(if (selected) ColorFlow.model.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(OrkanDesignTokens.controlRadius))) {
                                Row(Modifier.fillMaxWidth().heightIn(min = OrkanDesignTokens.touchTarget)
                                    .clickable {
                                        if (groupSelectionMode) { if (selected) stagedSelection.remove(index) else stagedSelection.add(index) }
                                        else onSelect(index)
                                    }.testTag("model-object-row-$index").padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Box(Modifier.size(20.dp).background(Color(row.color), CircleShape))
                                    Column(Modifier.weight(1f)) {
                                        Text(row.name, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        Text("T${row.tool}: ${row.material} · #${row.color.toUInt().toString(16).padStart(6, '0').takeLast(6).uppercase()}",
                                            maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                // Actions get a dedicated line so long translated labels and large
                                // accessibility text never compress the object name into a sliver.
                                Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(onClick = { onAssign(index) }, enabled = row.assignEnabled,
                                        modifier = Modifier.weight(1f).heightIn(min = OrkanDesignTokens.touchTarget)
                                            .testTag("model-object-assign-$index")) {
                                        Text(assignLabel, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    }
                                    if (row.parts.size > 1) TextButton(
                                        onClick = { expandedGroups = if (index in expandedGroups) expandedGroups - index else expandedGroups + index },
                                        modifier = Modifier.weight(1f).heightIn(min = OrkanDesignTokens.touchTarget)
                                            .testTag("model-object-group-toggle-$index")) {
                                        val expanded = index in expandedGroups
                                        Text(stringResource(if (expanded) R.string.ModelObjectCollapseParts else R.string.ModelObjectExpandParts, row.parts.size),
                                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                            if (index in expandedGroups) row.parts.forEachIndexed { partIndex, part ->
                                Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 4.dp, top = 3.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                                    .testTag("model-object-part-row-$index-$partIndex").padding(horizontal = 10.dp, vertical = 6.dp)) {
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(Modifier.size(16.dp).background(Color(part.color), CircleShape))
                                        Column(Modifier.weight(1f)) {
                                            Text(part.label, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                            Text("T${part.tool}: ${part.material}", maxLines = 2, overflow = TextOverflow.Ellipsis,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically) {
                                        TextButton(onClick = { onTransformPart(index, partIndex) },
                                            modifier = Modifier.weight(1f).heightIn(min = OrkanDesignTokens.touchTarget)
                                                .testTag("model-object-transform-part-$index-$partIndex")) {
                                            Text(stringResource(R.string.ModelObjectPartTransform), maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        }
                                        TextButton(onClick = { onEditPart(index, partIndex) },
                                            modifier = Modifier.weight(1f).heightIn(min = OrkanDesignTokens.touchTarget)
                                                .testTag("model-object-edit-part-$index-$partIndex")) {
                                            Text(stringResource(R.string.ModelObjectEditPart), maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text(cancelLabel) }
                    if (groupSelectionMode) TextButton(onClick = { onApplySelection(stagedSelection.sorted().toIntArray()) },
                        modifier = Modifier.heightIn(min = 48.dp).testTag("model-objects-selection-confirm")) {
                        Text(stringResource(R.string.ModelMenuApplySelection, stagedSelection.size))
                    }
                }
            }
        }
    }
}
