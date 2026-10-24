package ru.ytkab0bp.slicebeam.components.bed_menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.slic3r.BooleanOperation

@Composable
internal fun ModelBooleanDialog(
    targetName: String,
    toolNames: List<String>,
    onDismiss: () -> Unit,
    onChoose: (BooleanOperation) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ModelMenuBoolean)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    stringResource(R.string.ModelBooleanHelp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(R.string.ModelBooleanTargetSummary, targetName),
                    modifier = Modifier.testTag("model-boolean-target"),
                )
                Text(
                    stringResource(R.string.ModelBooleanToolsSummary, toolNames.joinToString()),
                    modifier = Modifier.testTag("model-boolean-tools"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = { onChoose(BooleanOperation.UNION) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("model-boolean-union"),
                ) { Text(stringResource(R.string.ModelBooleanUnion)) }
                TextButton(
                    onClick = { onChoose(BooleanOperation.DIFFERENCE) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("model-boolean-difference"),
                ) { Text(stringResource(R.string.ModelBooleanDifference)) }
                TextButton(
                    onClick = { onChoose(BooleanOperation.INTERSECTION) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("model-boolean-intersection"),
                ) { Text(stringResource(R.string.ModelBooleanIntersection)) }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}
