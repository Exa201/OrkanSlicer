package ru.ytkab0bp.slicebeam.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.ytkab0bp.slicebeam.R

internal data class WallToolHolderFormValues(
    val diameter: String = "16",
    val clearance: String = "0.4",
    val count: String = "4",
    val thickness: String = "3",
    val screw: String = "4.2",
    val guideHeight: String = "14",
)

internal enum class WallToolHolderIssue(val message: Int) {
    DIAMETER(R.string.WallToolHolderErrorDiameter),
    CLEARANCE(R.string.WallToolHolderErrorClearance),
    COUNT(R.string.WallToolHolderErrorCount),
    THICKNESS(R.string.WallToolHolderErrorThickness),
    SCREW(R.string.WallToolHolderErrorScrew),
    GUIDE_HEIGHT(R.string.WallToolHolderErrorGuideHeight),
}

internal object WallToolHolderFormLogic {
    fun defaults() = WallToolHolderFormValues()
    fun parse(text: String): Float = text.trim().replace(',', '.').toFloatOrNull() ?: Float.NaN
    fun parseCount(text: String): Int = text.trim().toIntOrNull() ?: -1

    fun validate(values: WallToolHolderFormValues): WallToolHolderIssue? {
        if (!inRange(parse(values.diameter), 4f, 30f)) return WallToolHolderIssue.DIAMETER
        if (!inRange(parse(values.clearance), 0f, 1f)) return WallToolHolderIssue.CLEARANCE
        if (parseCount(values.count) !in 2..6) return WallToolHolderIssue.COUNT
        if (!inRange(parse(values.thickness), 2f, 6f)) return WallToolHolderIssue.THICKNESS
        if (!inRange(parse(values.screw), 3f, 8f)) return WallToolHolderIssue.SCREW
        if (!inRange(parse(values.guideHeight), 6f, 35f)) return WallToolHolderIssue.GUIDE_HEIGHT
        return null
    }

    private fun inRange(value: Float, minimum: Float, maximum: Float) =
        value.isFinite() && value in minimum..maximum
}

@Composable
internal fun WallToolHolderFormContent(
    values: WallToolHolderFormValues,
    errorMessage: Int?,
    onValuesChanged: (WallToolHolderFormValues) -> Unit,
    onGenerate: (WallToolHolderFormValues) -> Unit,
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
            Text(stringResource(R.string.WallToolHolderDescription), color = MaterialTheme.colorScheme.onSurfaceVariant)
            GeneratorDimensionField(R.string.WallToolHolderDiameter, values.diameter) {
                onValuesChanged(values.copy(diameter = it))
            }
            GeneratorDimensionField(R.string.WallToolHolderClearance, values.clearance) {
                onValuesChanged(values.copy(clearance = it))
            }
            GeneratorDimensionField(
                R.string.WallToolHolderCount, values.count, keyboardType = KeyboardType.Number,
            ) { onValuesChanged(values.copy(count = it)) }
            GeneratorDimensionField(R.string.WallToolHolderThickness, values.thickness) {
                onValuesChanged(values.copy(thickness = it))
            }
            GeneratorDimensionField(R.string.WallToolHolderScrew, values.screw) {
                onValuesChanged(values.copy(screw = it))
            }
            GeneratorDimensionField(R.string.WallToolHolderGuideHeight, values.guideHeight) {
                onValuesChanged(values.copy(guideHeight = it))
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
