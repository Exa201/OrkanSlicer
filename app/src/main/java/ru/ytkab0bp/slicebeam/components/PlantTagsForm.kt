package ru.ytkab0bp.slicebeam.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.ytkab0bp.slicebeam.R

internal data class PlantTagsFormValues(
    val names: String = "Tomate\nBasilikum\nPetersilie\nMinze",
    val width: String = "70",
    val height: String = "24",
    val font: String = "8",
    val base: String = "2",
    val raised: String = "1.2",
    val hole: String = "4",
    val gap: String = "6",
)

internal enum class PlantTagsIssue(val message: Int) {
    COUNT(R.string.PlantTagsErrorCount),
    NAME_LENGTH(R.string.PlantTagsErrorNameLength),
    WIDTH(R.string.PlantTagsErrorWidth),
    HEIGHT(R.string.PlantTagsErrorHeight),
    FONT(R.string.PlantTagsErrorFont),
    BASE(R.string.PlantTagsErrorBase),
    RAISED(R.string.PlantTagsErrorRaised),
    HOLE(R.string.PlantTagsErrorHole),
    GAP(R.string.PlantTagsErrorGap),
    PLATE_SIZE(R.string.PlantTagsErrorPlateSize),
}

internal object PlantTagsFormLogic {
    fun defaults() = PlantTagsFormValues()
    fun labels(names: String): List<String> = names.split(Regex("\\R")).map { it.trim() }.filter { it.isNotEmpty() }
    fun parse(text: String): Float = text.trim().replace(',', '.').toFloatOrNull() ?: Float.NaN

    fun validate(values: PlantTagsFormValues): PlantTagsIssue? {
        val labels = labels(values.names)
        val width = parse(values.width)
        val height = parse(values.height)
        if (labels.isEmpty() || labels.size > 8) return PlantTagsIssue.COUNT
        if (labels.any { it.length > 24 }) return PlantTagsIssue.NAME_LENGTH
        if (!inRange(width, 45f, 100f)) return PlantTagsIssue.WIDTH
        if (!inRange(height, 18f, 40f)) return PlantTagsIssue.HEIGHT
        if (!inRange(parse(values.font), 3f, 20f)) return PlantTagsIssue.FONT
        if (!inRange(parse(values.base), .8f, 6f)) return PlantTagsIssue.BASE
        if (!inRange(parse(values.raised), .4f, 4f)) return PlantTagsIssue.RAISED
        val hole = parse(values.hole)
        if (!hole.isFinite() || hole < 0f || hole > minOf(width, height) * .5f) return PlantTagsIssue.HOLE
        val gap = parse(values.gap)
        if (!inRange(gap, 3f, 15f)) return PlantTagsIssue.GAP
        val columns = minOf(2, labels.size).toFloat()
        val rows = ((labels.size + 1) / 2).toFloat()
        if (columns * width + (columns - 1) * gap > 200f || rows * height + (rows - 1) * gap > 200f) {
            return PlantTagsIssue.PLATE_SIZE
        }
        return null
    }

    private fun inRange(value: Float, minimum: Float, maximum: Float) =
        value.isFinite() && value in minimum..maximum
}

@Composable
internal fun PlantTagsFormContent(
    values: PlantTagsFormValues,
    errorMessage: Int?,
    onValuesChanged: (PlantTagsFormValues) -> Unit,
    onGenerate: (PlantTagsFormValues) -> Unit,
) {
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * .82f
    Column(
        modifier = Modifier.fillMaxWidth().heightIn(max = maxHeight).padding(horizontal = 18.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.PlantTagsDescription), color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = values.names,
                onValueChange = { onValuesChanged(values.copy(names = it.take(240))) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.PlantTagsNames)) },
                minLines = 4,
                maxLines = 7,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                shape = MaterialTheme.shapes.medium,
            )
            GeneratorDimensionField(R.string.PlantTagsWidth, values.width) {
                onValuesChanged(values.copy(width = it))
            }
            GeneratorDimensionField(R.string.PlantTagsHeight, values.height) {
                onValuesChanged(values.copy(height = it))
            }
            GeneratorDimensionField(R.string.PlantTagsFont, values.font) {
                onValuesChanged(values.copy(font = it))
            }
            GeneratorDimensionField(R.string.PlantTagsBase, values.base) {
                onValuesChanged(values.copy(base = it))
            }
            GeneratorDimensionField(R.string.PlantTagsRaised, values.raised) {
                onValuesChanged(values.copy(raised = it))
            }
            GeneratorDimensionField(R.string.PlantTagsHole, values.hole) {
                onValuesChanged(values.copy(hole = it))
            }
            GeneratorDimensionField(R.string.PlantTagsGap, values.gap) {
                onValuesChanged(values.copy(gap = it))
            }
            if (errorMessage != null) Text(
                stringResource(errorMessage), color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Button(onClick = { onGenerate(values) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Text(stringResource(R.string.ConverterConvertSave), fontSize = 16.sp)
        }
    }
}
