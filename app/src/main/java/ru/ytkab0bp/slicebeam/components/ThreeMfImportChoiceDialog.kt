package ru.ytkab0bp.slicebeam.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** Lets a project opt into its embedded settings without mutating saved profiles. */
@Composable
internal fun ThreeMfImportChoiceDialog(
    title: String,
    explanation: String,
    printerLabel: String,
    printerNames: List<String>,
    initialPrinterIndex: Int,
    filePrinterIndex: Int,
    filePrinterLabel: String,
    unavailableFilePrinterLabel: String,
    printSettingsLabel: String,
    filamentSettingsLabel: String,
    temporaryNote: String,
    applyLabel: String,
    cancelLabel: String,
    canUsePrintSettings: Boolean,
    canUseFilamentSettings: Boolean,
    onDismiss: () -> Unit,
    onApply: (printer: String?, useFilePrinter: Boolean, usePrintSettings: Boolean, useFilamentSettings: Boolean) -> Unit,
) {
    var selectedPrinterIndex by remember(printerNames, initialPrinterIndex) {
        mutableIntStateOf(initialPrinterIndex.coerceIn(0, (printerNames.size - 1).coerceAtLeast(0)))
    }
    var useFilePrinter by remember(filePrinterIndex) { mutableStateOf(false) }
    var usePrintSettings by remember(canUsePrintSettings) { mutableStateOf(false) }
    var useFilamentSettings by remember(canUseFilamentSettings) { mutableStateOf(false) }
    var printerMenuExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(explanation)
                Text(printerLabel, style = MaterialTheme.typography.titleSmall)
                Box {
                    OutlinedButton(
                        onClick = { printerMenuExpanded = true },
                        enabled = printerNames.isNotEmpty() && !useFilePrinter,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        Text(printerNames.getOrNull(selectedPrinterIndex).orEmpty(), modifier = Modifier.weight(1f))
                        Text("⌄", style = MaterialTheme.typography.titleMedium)
                    }
                    DropdownMenu(
                        expanded = printerMenuExpanded,
                        onDismissRequest = { printerMenuExpanded = false },
                    ) {
                        printerNames.forEachIndexed { index, name ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    selectedPrinterIndex = index
                                    printerMenuExpanded = false
                                },
                                modifier = Modifier.heightIn(min = 48.dp),
                            )
                        }
                    }
                }
                ThreeMfChoiceRow(
                    label = if (filePrinterIndex >= 0) filePrinterLabel else unavailableFilePrinterLabel,
                    testTag = "3mf-import-file-printer",
                    checked = useFilePrinter,
                    enabled = filePrinterIndex >= 0,
                    onCheckedChange = { checked ->
                        useFilePrinter = checked
                        selectedPrinterIndex = if (checked) filePrinterIndex else initialPrinterIndex
                    },
                )
                ThreeMfChoiceRow(
                    label = printSettingsLabel,
                    testTag = "3mf-import-print-settings",
                    checked = usePrintSettings,
                    enabled = canUsePrintSettings,
                    onCheckedChange = { usePrintSettings = it },
                )
                ThreeMfChoiceRow(
                    label = filamentSettingsLabel,
                    testTag = "3mf-import-filament-settings",
                    checked = useFilamentSettings,
                    enabled = canUseFilamentSettings,
                    onCheckedChange = { useFilamentSettings = it },
                )
                Text(temporaryNote, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onApply(
                        printerNames.getOrNull(selectedPrinterIndex),
                        useFilePrinter,
                        usePrintSettings,
                        useFilamentSettings,
                    )
                },
                modifier = Modifier.heightIn(min = 48.dp).testTag("3mf-import-apply"),
            ) { Text(applyLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(cancelLabel)
            }
        },
    )
}

@Composable
private fun ThreeMfChoiceRow(
    label: String,
    testTag: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = onCheckedChange)
            .testTag(testTag)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
        Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}
