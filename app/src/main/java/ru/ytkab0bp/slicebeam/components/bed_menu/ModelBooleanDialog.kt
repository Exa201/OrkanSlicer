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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.slic3r.BooleanOperation

@Composable
internal fun ModelBooleanDialog(
    onDismiss: () -> Unit,
    onChoose: (BooleanOperation) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ModelMenuBoolean)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.ModelBooleanHelp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = { onChoose(BooleanOperation.UNION) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) { Text(stringResource(R.string.ModelBooleanUnion)) }
                TextButton(
                    onClick = { onChoose(BooleanOperation.DIFFERENCE) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) { Text(stringResource(R.string.ModelBooleanDifference)) }
                TextButton(
                    onClick = { onChoose(BooleanOperation.INTERSECTION) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
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
