package ru.ytkab0bp.slicebeam.ui.workspace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.view.EmbossModeController
import ru.ytkab0bp.slicebeam.view.EmbossTextInputValidator

@Composable
internal fun EmbossModePanel(
    controller: EmbossModeController,
    startInStandaloneMode: Boolean = false,
    onExit: () -> Unit,
) {
    val initialText = stringResource(R.string.AppName)
    val initialSize = stringResource(R.string.EmbossDefaultFontSize)
    val initialDepth = stringResource(R.string.EmbossDefaultDepth)
    var text by remember { mutableStateOf(initialText) }
    var size by remember { mutableStateOf(initialSize) }
    var depth by remember { mutableStateOf(initialDepth) }
    var addMode by remember { mutableStateOf(true) }
    var standaloneMode by remember { mutableStateOf(startInStandaloneMode) }
    val validation = EmbossTextInputValidator.validate(text, size, depth)

    fun sync() = controller.update(text, size, depth, addMode, standaloneMode)
    DisposableEffect(controller) {
        controller.start()
        sync()
        onDispose { controller.stop() }
    }

    Column(Modifier.fillMaxWidth().heightIn(max = 640.dp).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.EmbossTitle), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(if (standaloneMode) R.string.TextModelPlacementInstruction else R.string.EmbossInstruction),
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedTextField(
            value = text,
            onValueChange = { text = it; sync() },
            label = { Text(stringResource(R.string.EmbossTextLabel)) },
            placeholder = { Text(stringResource(R.string.EmbossTextHint)) },
            singleLine = true,
            isError = validation.error == EmbossTextInputValidator.Error.EMPTY_TEXT ||
                validation.error == EmbossTextInputValidator.Error.TEXT_TOO_LONG,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = size,
            onValueChange = { size = it; sync() },
            label = { Text(stringResource(R.string.EmbossFontSize)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            isError = validation.error == EmbossTextInputValidator.Error.INVALID_SIZE,
            modifier = Modifier.fillMaxWidth().testTag("emboss-font-size"),
        )
        OutlinedTextField(
            value = depth,
            onValueChange = { depth = it; sync() },
            label = { Text(stringResource(R.string.EmbossDepth)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            isError = validation.error == EmbossTextInputValidator.Error.INVALID_DEPTH,
            modifier = Modifier.fillMaxWidth(),
        )
        if (validation.error != null) {
            Text(
                stringResource(R.string.EmbossInvalidParameters),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.testTag("emboss-input-error"),
            )
        }
        FilterChip(
            selected = standaloneMode,
            onClick = { standaloneMode = !standaloneMode; sync() },
            label = { Text(stringResource(R.string.TextModelCreateSeparate)) },
            modifier = Modifier.testTag("emboss-standalone-mode"),
        )
        if (!standaloneMode) {
            Text(stringResource(R.string.EmbossOperation), style = MaterialTheme.typography.labelLarge)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = addMode,
                    onClick = { addMode = true; sync() },
                    label = { Text(stringResource(R.string.EmbossRaise)) },
                )
            FilterChip(
                selected = !addMode,
                onClick = { addMode = false; sync() },
                label = { Text(stringResource(R.string.EmbossEngrave)) },
            )
            }
        }
        Button(
            onClick = onExit,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("emboss-done"),
        ) {
            Text(stringResource(R.string.ActionDone))
        }
    }
}
