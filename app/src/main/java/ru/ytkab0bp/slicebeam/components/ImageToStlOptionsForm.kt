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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.ytkab0bp.slicebeam.R

/** Immutable editable parameters shared by the Compose workspace form and Android fallback. */
internal data class ImageToStlOptions(
    val targetFormat: Int = 0,
    val mode: Int = 0,
    val width: String = "80",
    val height: String = "60",
    val base: String = "1.2",
    val relief: String = "4.0",
    val resolution: Int = 0,
    val frameBase: String = "5",
    val footDepth: String = "24",
    val openingDiameter: String = "18",
    val curveRadius: String = "55",
    val diameter: String = "90",
    val topDiameter: String = "90",
    val wrapAngle: Int = 360,
    val waveCount: Int = 0,
    val waveDepth: String = "3",
    val closeBottom: Boolean = true,
    val closeTop: Boolean = false,
    val createLightBase: Boolean = false,
    val lightBaseThickness: String = "7.5",
    val cableDiameter: String = "6",
    val frameWidth: String = "4",
    val toneMode: Int = 0,
    val brightness: Int = 100,
    val contrast: Int = 100,
    val invert: Boolean = false,
)

internal fun defaultImageToStlOptions(initialMode: Int, targetFormat: Int): ImageToStlOptions =
    ImageToStlOptions(
        targetFormat = targetFormat.coerceIn(0, 4),
        mode = initialMode.coerceIn(0, 10),
        width = if (initialMode >= 2) "45" else "80",
    )

@Composable
internal fun ImageToStlOptionsForm(
    activity: Activity,
    images: List<Uri>,
    values: ImageToStlOptions,
    onValuesChanged: (ImageToStlOptions) -> Unit,
    onEditImage: () -> Unit,
    onGenerate: (ImageToStlOptions, saveAfterGeneration: Boolean) -> Unit,
) {
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * .68f
    val targetNames = stringArrayResource(R.array.ConverterTargets)
    val modeNames = stringArrayResource(R.array.ImageToStlModes)
    val resolutionNames = stringArrayResource(R.array.LithophaneResolutions)
    val stl = values.targetFormat == 0
    val selectionCount = activity.resources.getQuantityString(
        R.plurals.LithophaneImagesSelected, images.size, images.size,
    )
    Column(
        modifier = Modifier.fillMaxWidth().heightIn(max = maxHeight).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.LithophaneGeneratorDescription), color = MaterialTheme.colorScheme.onSurfaceVariant)
            ChoiceField(
                label = stringResource(R.string.ConverterTarget),
                selected = targetNames.getOrElse(values.targetFormat) { targetNames.first() },
                choices = targetNames.toList(),
                testTag = "image-to-stl-target",
            ) { index ->
                onValuesChanged(values.copy(targetFormat = index.coerceIn(targetNames.indices)))
            }
            if (values.targetFormat <= 1) {
                ChoiceField(
                    label = stringResource(R.string.ImageToStlMode),
                    selected = modeNames.getOrElse(values.mode) { modeNames.first() },
                    choices = modeNames.toList(),
                    testTag = "image-to-stl-mode",
                ) { index -> onValuesChanged(values.copy(mode = index.coerceIn(modeNames.indices))) }
            }
            if (stl) {
                Text(selectionCount, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SourcePreview(activity, images.firstOrNull())
                TextButton(
                    onClick = onEditImage,
                    enabled = images.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("image-to-stl-edit-image"),
                ) { Text(stringResource(R.string.LithophaneImageEditorOpen)) }
                GeneratorDimensionField(R.string.ImageToStlWidth, values.width) {
                    onValuesChanged(values.copy(width = it))
                }
                GeneratorDimensionField(R.string.ImageToStlHeight, values.height) {
                    onValuesChanged(values.copy(height = it))
                }
                val thinLabel = if (values.mode in 2..5) R.string.LithophaneMinThickness else R.string.ImageToStlBase
                val tallLabel = if (values.mode in 2..5) R.string.LithophaneMaxThickness else R.string.ImageToStlReliefHeight
                GeneratorDimensionField(thinLabel, values.base) { onValuesChanged(values.copy(base = it)) }
                GeneratorDimensionField(tallLabel, values.relief) { onValuesChanged(values.copy(relief = it)) }
                ChoiceField(
                    label = stringResource(R.string.LithophaneResolution),
                    selected = resolutionNames.getOrElse(values.resolution) { resolutionNames.first() },
                    choices = resolutionNames.toList(),
                    testTag = "image-to-stl-resolution",
                ) { index -> onValuesChanged(values.copy(resolution = index.coerceIn(resolutionNames.indices))) }

                if (values.mode == 4) {
                    GeneratorDimensionField(R.string.LithophaneFrameBaseDepth, values.frameBase) {
                        onValuesChanged(values.copy(frameBase = it))
                    }
                    GeneratorDimensionField(R.string.LithophaneFrameFootDepth, values.footDepth) {
                        onValuesChanged(values.copy(footDepth = it))
                    }
                    GeneratorDimensionField(R.string.LithophaneFrameOpening, values.openingDiameter) {
                        onValuesChanged(values.copy(openingDiameter = it))
                    }
                }
                if (values.mode == 5) {
                    GeneratorDimensionField(R.string.LithophaneCurveRadius, values.curveRadius) {
                        onValuesChanged(values.copy(curveRadius = it))
                    }
                }
                if (values.mode in 6..10) {
                    Text(stringResource(R.string.LithophaneRoundDescription), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    GeneratorDimensionField(R.string.LithophaneBaseDiameter, values.diameter) {
                        onValuesChanged(values.copy(diameter = it))
                    }
                    if (values.mode in 7..10) {
                        GeneratorDimensionField(R.string.LithophaneTopDiameter, values.topDiameter) {
                            onValuesChanged(values.copy(topDiameter = it))
                        }
                    }
                    Text(stringResource(R.string.LithophaneWrapAngle))
                    Slider(
                        value = values.wrapAngle.toFloat(),
                        onValueChange = { onValuesChanged(values.copy(wrapAngle = it.toInt())) },
                        valueRange = 0f..360f,
                        modifier = Modifier.testTag("image-to-stl-wrap-angle"),
                    )
                    if (values.mode in 9..10) {
                        Text(stringResource(R.string.LithophaneWaves))
                        Slider(
                            value = values.waveCount.toFloat(),
                            onValueChange = { onValuesChanged(values.copy(waveCount = it.toInt())) },
                            valueRange = 0f..24f,
                            steps = 23,
                            modifier = Modifier.testTag("image-to-stl-wave-count"),
                        )
                        GeneratorDimensionField(R.string.LithophaneWaveDepth, values.waveDepth) {
                            onValuesChanged(values.copy(waveDepth = it))
                        }
                    }
                    BooleanOption(R.string.LithophaneCloseBottom, values.closeBottom) {
                        onValuesChanged(values.copy(closeBottom = it))
                    }
                    BooleanOption(R.string.LithophaneCloseTop, values.closeTop) {
                        onValuesChanged(values.copy(closeTop = it))
                    }
                    BooleanOption(R.string.LithophaneCreateBase, values.createLightBase) {
                        onValuesChanged(values.copy(createLightBase = it))
                    }
                    if (values.mode == 10) {
                        GeneratorDimensionField(R.string.LithophaneBaseThickness, values.lightBaseThickness) {
                            onValuesChanged(values.copy(lightBaseThickness = it))
                        }
                        GeneratorDimensionField(R.string.LithophaneCableDiameter, values.cableDiameter) {
                            onValuesChanged(values.copy(cableDiameter = it))
                        }
                        GeneratorDimensionField(R.string.LithophaneFrameWidth, values.frameWidth) {
                            onValuesChanged(values.copy(frameWidth = it))
                        }
                    }
                }
            }
        }
        Button(
            onClick = { onGenerate(values, false) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("image-to-stl-generate"),
        ) { Text(stringResource(R.string.LithophaneGenerate)) }
        Button(
            onClick = { onGenerate(values, true) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("image-to-stl-generate-save"),
        ) { Text(stringResource(R.string.LithophaneGenerateAndSave)) }
    }
}

@Composable
private fun ChoiceField(
    label: String,
    selected: String,
    choices: List<String>,
    testTag: String,
    onSelected: (Int) -> Unit,
) {
    var expanded by remember(testTag) { mutableStateOf(false) }
    Column {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(testTag),
            ) {
                Text(selected, modifier = Modifier.weight(1f), maxLines = 2)
                Text("⌄", fontSize = 18.sp)
            }
            androidx.compose.material3.DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                choices.forEachIndexed { index, choice ->
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(choice) },
                        onClick = { expanded = false; onSelected(index) },
                    )
                }
            }
        }
    }
}

@Composable
private fun BooleanOption(label: Int, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onChecked)
        Text(stringResource(label), modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SourcePreview(activity: Activity, uri: Uri?) {
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = uri) {
        value = uri?.let { source ->
            withContext(Dispatchers.IO) {
                runCatching { ImageToStlConverter.decodePreviewBitmap(activity, source) }.getOrNull()
            }
        }
    }
    val previewBitmap = bitmap
    if (previewBitmap != null) {
        Image(
            bitmap = previewBitmap.asImageBitmap(),
            contentDescription = stringResource(R.string.ConverterSourcePreview),
            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 220.dp).testTag("image-to-stl-source-preview"),
            contentScale = ContentScale.Fit,
        )
    } else {
        Text(
            text = stringResource(if (uri == null) R.string.ConverterPickImage else R.string.LithophanePreviewUnavailable),
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
