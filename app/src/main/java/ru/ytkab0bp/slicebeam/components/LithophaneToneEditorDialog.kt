package ru.ytkab0bp.slicebeam.components

import android.app.Activity
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import ru.ytkab0bp.slicebeam.R
import java.util.concurrent.atomic.AtomicReference

@Composable
internal fun LithophaneToneEditorDialog(
    activity: Activity,
    image: Uri,
    initialToneMode: Int,
    initialBrightness: Int,
    initialContrast: Int,
    initialInvert: Boolean,
    onDismiss: () -> Unit,
    onApply: (toneMode: Int, brightness: Int, contrast: Int, invert: Boolean) -> Unit,
) {
    val modes = stringArrayResource(R.array.LithophaneGrayscaleModes)
    var toneMode by remember(image) { mutableStateOf(initialToneMode.coerceIn(0, modes.lastIndex)) }
    var brightness by remember(image) { mutableStateOf(initialBrightness.coerceIn(0, 200)) }
    var contrast by remember(image) { mutableStateOf(initialContrast.coerceIn(0, 200)) }
    var invert by remember(image) { mutableStateOf(initialInvert) }
    var modeMenuExpanded by remember { mutableStateOf(false) }
    var preview by remember(image) { mutableStateOf<Bitmap?>(null) }
    val previewReference = remember(image) { AtomicReference<Bitmap?>(null) }

    DisposableEffect(Unit) {
        onDispose { previewReference.getAndSet(null)?.let { if (!it.isRecycled) it.recycle() } }
    }
    DisposableEffect(preview) {
        val displayed = preview
        onDispose {
            if (displayed != null && displayed !== previewReference.get() && !displayed.isRecycled) {
                displayed.recycle()
            }
        }
    }
    DisposableEffect(image, toneMode, brightness, contrast, invert) {
        val cancel = ImageToStlConverter.requestLithophanePreview(
            activity = activity,
            uri = image,
            invert = invert,
            toneMode = toneMode,
            brightness = (brightness - 100) / 100f,
            contrast = contrast / 100f,
        ) { rendered ->
            previewReference.set(rendered)
            preview = rendered
        }
        onDispose(cancel)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.widthIn(max = 560.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 680.dp)
                    .verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(stringResource(R.string.LithophaneImageEditor), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.LithophanePreviewCaption), style = MaterialTheme.typography.bodySmall)
                Box(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp, max = 320.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    preview?.let { bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = stringResource(R.string.LithophanePreviewCaption),
                            modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp).testTag("tone-editor-preview-image"),
                            contentScale = ContentScale.Fit,
                        )
                    } ?: Text(
                        stringResource(R.string.LithophanePreviewUnavailable),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box {
                    OutlinedButton(
                        onClick = { modeMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("tone-editor-mode"),
                    ) {
                        Text(
                            "${stringResource(R.string.LithophaneGrayscaleMode)}: ${modes.getOrNull(toneMode).orEmpty()}",
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    DropdownMenu(expanded = modeMenuExpanded, onDismissRequest = { modeMenuExpanded = false }) {
                        modes.forEachIndexed { index, label ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = { toneMode = index; modeMenuExpanded = false },
                            )
                        }
                    }
                }
                ToneSlider(
                    label = stringResource(R.string.LithophaneBrightnessValue, brightness - 100),
                    value = brightness.toFloat(),
                    valueRange = 0f..200f,
                    tag = "tone-editor-brightness",
                    onValueChange = { brightness = it.toInt().coerceIn(0, 200) },
                )
                ToneSlider(
                    label = stringResource(R.string.LithophaneContrastValue, contrast),
                    value = contrast.toFloat(),
                    valueRange = 0f..200f,
                    tag = "tone-editor-contrast",
                    onValueChange = { contrast = it.toInt().coerceIn(0, 200) },
                )
                Text(stringResource(R.string.LithophaneToneHelp), style = MaterialTheme.typography.bodySmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = invert,
                        onCheckedChange = { invert = it },
                        modifier = Modifier.testTag("tone-editor-invert"),
                    )
                    Text(stringResource(R.string.LithophaneInvert), modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("tone-editor-cancel")) {
                        Text(stringResource(android.R.string.cancel))
                    }
                    TextButton(
                        onClick = { onApply(toneMode, brightness, contrast, invert) },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("tone-editor-apply"),
                    ) { Text(stringResource(R.string.ConverterApply)) }
                }
            }
        }
    }
}

@Composable
private fun ToneSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    tag: String,
    onValueChange: (Float) -> Unit,
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.testTag(tag).semantics { contentDescription = label },
        )
    }
}
