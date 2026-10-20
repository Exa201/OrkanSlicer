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

@Composable
internal fun EmbossModePanel(controller: EmbossModeController, onExit: () -> Unit) {
    val initialText = stringResource(R.string.AppName)
    val initialSize = stringResource(R.string.EmbossDefaultFontSize)
    val initialDepth = stringResource(R.string.EmbossDefaultDepth)
    var text by remember { mutableStateOf(initialText) }
    var size by remember { mutableStateOf(initialSize) }
    var depth by remember { mutableStateOf(initialDepth) }
    var addMode by remember { mutableStateOf(true) }

    fun sync() = controller.update(text, size, depth, addMode)
    DisposableEffect(controller) {
        controller.start()
        onDispose { controller.stop() }
    }

    Column(Modifier.fillMaxWidth().heightIn(max = 640.dp).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.EmbossTitle), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.EmbossInstruction), style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(
            value = text,
            onValueChange = { text = it; sync() },
            label = { Text(stringResource(R.string.EmbossTextLabel)) },
            placeholder = { Text(stringResource(R.string.EmbossTextHint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = size,
            onValueChange = { size = it; sync() },
            label = { Text(stringResource(R.string.EmbossFontSize)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = depth,
            onValueChange = { depth = it; sync() },
            label = { Text(stringResource(R.string.EmbossDepth)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
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
        Button(
            onClick = onExit,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("emboss-done"),
        ) {
            Text(stringResource(R.string.ActionDone))
        }
    }
}
