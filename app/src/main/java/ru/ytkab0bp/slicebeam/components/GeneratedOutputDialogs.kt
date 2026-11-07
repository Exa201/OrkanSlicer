package ru.ytkab0bp.slicebeam.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import ru.ytkab0bp.slicebeam.R

@Composable
internal fun GeneratedModelOutputDialog(
    message: String,
    onDismiss: () -> Unit,
    onDiscard: () -> Unit,
    onSave: () -> Unit,
    onUse: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.GeneratedOutputTitle)) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onUse, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.GeneratedOutputUse))
            }
        },
        dismissButton = {
            Column {
                TextButton(onClick = onSave, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.GeneratedOutputSave))
                }
                TextButton(onClick = onDiscard, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.GeneratedOutputCancel))
                }
            }
        },
    )
}

@Composable
internal fun GeneratedPairOutputDialog(
    onDismiss: () -> Unit,
    onLoadModelOnly: () -> Unit,
    onLoadBoth: () -> Unit,
    onSaveModel: () -> Unit,
    onSaveAccessory: () -> Unit,
    onSaveBoth: () -> Unit,
) {
    var saving by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.widthIn(max = 560.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 640.dp)
                    .verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    stringResource(if (saving) R.string.GeneratedOutputSaveTitle else R.string.GeneratedOutputTitle),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    stringResource(if (saving) R.string.LithophanePairSaveQuestion else R.string.LithophanePairQuestion),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (saving) {
                    PairAction(R.string.LithophaneSaveModelOnly, onSaveModel)
                    PairAction(R.string.LithophaneSaveAccessoryOnly, onSaveAccessory)
                    PairAction(R.string.LithophaneSaveBoth, onSaveBoth)
                    TextButton(onClick = { saving = false }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text(stringResource(android.R.string.cancel))
                    }
                } else {
                    PairAction(R.string.ConverterLoad, onLoadBoth)
                    PairAction(R.string.LithophaneGenerate, onLoadModelOnly)
                    OutlinedButton(
                        onClick = { saving = true },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) { Text(stringResource(R.string.ConverterOnlySave)) }
                    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text(stringResource(android.R.string.cancel))
                    }
                }
            }
        }
    }
}

@Composable
private fun PairAction(label: Int, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text(stringResource(label), maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun GeneratedRasterPreviewDialog(
    bitmap: Bitmap,
    onDismiss: () -> Unit,
    onDiscard: () -> Unit,
    onSave: () -> Unit,
) {
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    DisposableEffect(bitmap) {
        onDispose { if (!bitmap.isRecycled) bitmap.recycle() }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ConverterOutputPreview)) },
        text = {
            Image(
                bitmap = image,
                contentDescription = stringResource(R.string.ConverterOutputPreview),
                modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                contentScale = ContentScale.Fit,
            )
        },
        confirmButton = {
            TextButton(onClick = onSave, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.ConverterSaveNow))
            }
        },
        dismissButton = {
            TextButton(onClick = onDiscard, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.GeneratedOutputCancel))
            }
        },
    )
}

@Composable
internal fun GeneratedSavedOutputDialog(
    onDismiss: () -> Unit,
    onKeepSaved: () -> Unit,
    onLoad: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ConverterSavedTitle)) },
        text = { Text(stringResource(R.string.ConverterLoadQuestion)) },
        confirmButton = {
            TextButton(onClick = onLoad, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.ConverterLoad))
            }
        },
        dismissButton = {
            TextButton(onClick = onKeepSaved, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.ConverterOnlySave))
            }
        },
    )
}

@Composable
internal fun ConverterErrorDialog(
    message: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ConverterFailed)) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(android.R.string.ok))
            }
        },
    )
}
