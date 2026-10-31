package ru.ytkab0bp.slicebeam.components.bed_menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.slic3r.ModifierVolumeBoxState

private val modifierTypes = listOf(
    1 to R.string.ModelModifierNegative,
    3 to R.string.ModelModifierSupportBlocker,
    4 to R.string.ModelModifierSupportEnforcer,
)
private fun modifierTypeLabel(type: Int) = when (type) {
    1 -> R.string.ModelModifierNegative
    2 -> R.string.ModelModifierParameter
    3 -> R.string.ModelModifierSupportBlocker
    4 -> R.string.ModelModifierSupportEnforcer
    else -> R.string.ModelMenuModifiers
}

@Composable
internal fun ModelModifierVolumesDialog(
    existing: List<ModifierVolumeBoxState>,
    onDismiss: () -> Unit,
    onAdd: (type: Int, position: DoubleArray, size: DoubleArray) -> Unit,
    onUpdate: (volumeIndex: Int, position: DoubleArray, size: DoubleArray) -> Unit,
    onDelete: (volumeIndex: Int) -> Unit,
) {
    var selectedType by remember { mutableStateOf(2) }
    var x by remember { mutableStateOf("0") }
    var y by remember { mutableStateOf("0") }
    var z by remember { mutableStateOf("0") }
    var width by remember { mutableStateOf("10") }
    var depth by remember { mutableStateOf("10") }
    var height by remember { mutableStateOf("10") }
    var editingVolume by remember { mutableStateOf<ModifierVolumeBoxState?>(null) }
    val position = listOf(x, y, z).mapNotNull { it.toDoubleOrNull() }
    val size = listOf(width, depth, height).mapNotNull { it.toDoubleOrNull() }
    val validInput = position.size == 3 && size.size == 3 &&
        size.all { it in 0.2..500.0 } && position.all { it in -10000.0..10000.0 }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ModelMenuModifiers)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.ModelModifiersHelp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                modifierTypes.forEach { (type, label) ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text(stringResource(label)) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    )
                }
                Text(stringResource(R.string.ModelModifierPosition), style = MaterialTheme.typography.labelLarge)
                NumberRow(x, { x = it }, y, { y = it }, z, { z = it })
                Text(stringResource(R.string.ModelModifierSize), style = MaterialTheme.typography.labelLarge)
                NumberRow(width, { width = it }, depth, { depth = it }, height, { height = it })
                if (existing.isNotEmpty()) {
                    Text(stringResource(R.string.ModelModifierExisting), style = MaterialTheme.typography.titleSmall)
                    existing.forEach { volume ->
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(modifierTypeLabel(volume.type)), modifier = Modifier.weight(1f))
                            TextButton(onClick = {
                                editingVolume = volume
                                selectedType = volume.type.takeIf { type -> modifierTypes.any { it.first == type } } ?: 1
                                x = volume.position[0].toString(); y = volume.position[1].toString(); z = volume.position[2].toString()
                                width = volume.size[0].toString(); depth = volume.size[1].toString(); height = volume.size[2].toString()
                            }, modifier = Modifier.heightIn(min = 48.dp)) {
                                Text(stringResource(R.string.ModelModifierEdit))
                            }
                            TextButton(onClick = { onDelete(volume.volumeIndex) }, modifier = Modifier.heightIn(min = 48.dp)) {
                                Text(stringResource(R.string.ModelModifierDelete))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = validInput,
                onClick = {
                    val editing = editingVolume
                    if (editing == null) onAdd(selectedType, position.toDoubleArray(), size.toDoubleArray())
                    else onUpdate(editing.volumeIndex, position.toDoubleArray(), size.toDoubleArray())
                },
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text(stringResource(if (editingVolume == null) R.string.ModelModifierAddBox else R.string.ModelModifierSaveChanges)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

@Composable
private fun NumberRow(
    first: String, onFirst: (String) -> Unit,
    second: String, onSecond: (String) -> Unit,
    third: String, onThird: (String) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        NumberField(first, onFirst, "X mm", Modifier.weight(1f))
        NumberField(second, onSecond, "Y mm", Modifier.weight(1f))
        NumberField(third, onThird, "Z mm", Modifier.weight(1f))
    }
}

@Composable
private fun NumberField(value: String, onValue: (String) -> Unit, label: String, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { input -> if (input.length <= 10 && input.matches(Regex("-?\\d{0,6}(?:[.,]\\d{0,3})?"))) onValue(input.replace(',', '.')) },
        modifier = modifier.heightIn(min = 56.dp),
        singleLine = true,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
}
