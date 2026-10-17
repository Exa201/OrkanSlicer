package ru.ytkab0bp.slicebeam.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ru.ytkab0bp.slicebeam.R

/** Selects the source and output formats before the platform document picker opens. */
@Composable
internal fun ImageFormatPairDialog(
    onDismiss: () -> Unit,
    onChooseSource: (sourceFormat: Int, targetFormat: Int) -> Unit,
) {
    val sourceFormats = stringArrayResource(R.array.ConverterSources)
    val targetFormats = stringArrayResource(R.array.ConverterTargets)
    var selectedSource by remember { mutableIntStateOf(0) }
    var selectedTarget by remember { mutableIntStateOf(0) }
    var expandedSource by remember { mutableStateOf(false) }
    var expandedTarget by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ConverterTitle)) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.ConverterSourceFormat), style = MaterialTheme.typography.titleSmall)
                FormatDropdown(
                    label = sourceFormats.getOrNull(selectedSource).orEmpty(),
                    options = sourceFormats,
                    expanded = expandedSource,
                    testTag = "image-source-format",
                    onExpand = { expandedSource = true },
                    onDismiss = { expandedSource = false },
                    onSelect = { selectedSource = it; expandedSource = false },
                )
                Text(stringResource(R.string.ConverterTarget), style = MaterialTheme.typography.titleSmall)
                FormatDropdown(
                    label = targetFormats.getOrNull(selectedTarget).orEmpty(),
                    options = targetFormats,
                    expanded = expandedTarget,
                    testTag = "image-target-format",
                    onExpand = { expandedTarget = true },
                    onDismiss = { expandedTarget = false },
                    onSelect = { selectedTarget = it; expandedTarget = false },
                )
                Text(stringResource(R.string.ConverterLocalFormatsNote), style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onChooseSource(selectedSource, selectedTarget) },
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text(stringResource(R.string.ConverterChooseSourceFile)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

@Composable
private fun FormatDropdown(
    label: String,
    options: Array<String>,
    expanded: Boolean,
    testTag: String,
    onExpand: () -> Unit,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    Box {
        OutlinedButton(onClick = onExpand, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(testTag)) {
            Text(label, modifier = Modifier.weight(1f).padding(end = 8.dp))
            Text("⌄", style = MaterialTheme.typography.titleMedium)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = { onSelect(index) },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
    }
}
