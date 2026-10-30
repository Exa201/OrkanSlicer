package ru.ytkab0bp.slicebeam.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.slic3r.StepImportOptions

@Composable
internal fun StepImportOptionsDialog(
    initial: StepImportOptions,
    onDismiss: () -> Unit,
    onImport: (StepImportOptions, saveAsDefault: Boolean) -> Unit,
) {
    var linear by remember(initial) { mutableStateOf(initial.linearDeflection.toString()) }
    var angular by remember(initial) { mutableStateOf(initial.angularDeflection.toString()) }
    var split by remember(initial) { mutableStateOf(initial.splitCompound) }
    var saveAsDefault by remember { mutableStateOf(false) }
    var invalid by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.StepImportTitle)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(stringResource(R.string.StepImportDescription))
                OutlinedTextField(
                    value = linear,
                    onValueChange = { linear = it; invalid = false },
                    modifier = Modifier.fillMaxWidth().testTag("step-linear-deflection"),
                    label = { Text(stringResource(R.string.StepLinearDeflection)) },
                    supportingText = { Text(stringResource(R.string.StepLinearDeflectionHint)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = angular,
                    onValueChange = { angular = it; invalid = false },
                    modifier = Modifier.fillMaxWidth().testTag("step-angular-deflection"),
                    label = { Text(stringResource(R.string.StepAngularDeflection)) },
                    supportingText = { Text(stringResource(R.string.StepAngularDeflectionHint)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = split, onCheckedChange = { split = it }, modifier = Modifier.testTag("step-split-compound"))
                    Text(stringResource(R.string.StepSplitCompound))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = saveAsDefault, onCheckedChange = { saveAsDefault = it }, modifier = Modifier.testTag("step-save-defaults"))
                    Text(stringResource(R.string.StepSaveDefaults))
                }
                if (invalid) Text(
                    stringResource(R.string.StepInvalidOptions),
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val options = StepImportOptions(
                    linear.replace(',', '.').toDoubleOrNull() ?: Double.NaN,
                    angular.replace(',', '.').toDoubleOrNull() ?: Double.NaN,
                    split,
                )
                if (options.validate() != null) invalid = true else onImport(options, saveAsDefault)
            }, modifier = Modifier.testTag("step-import-confirm")) { Text(stringResource(R.string.StepImportAction)) }
        },
        dismissButton = { TextButton(onClick = onDismiss, modifier = Modifier.testTag("step-import-cancel")) { Text(stringResource(android.R.string.cancel)) } },
    )
}
