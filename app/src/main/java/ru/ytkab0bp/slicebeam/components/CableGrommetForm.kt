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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.ytkab0bp.slicebeam.R

internal data class CableGrommetFormValues(
    val diameter: String = "12",
    val clearance: String = "0.4",
    val wall: String = "2",
    val flange: String = "50",
    val flangeThickness: String = "3",
    val tubeLength: String = "20",
    val screwHole: String = "4.2",
)

internal enum class CableGrommetIssue(val message: Int) {
    DIAMETER(R.string.CableGrommetErrorDiameter),
    CLEARANCE(R.string.CableGrommetErrorClearance),
    WALL(R.string.CableGrommetErrorWall),
    FLANGE(R.string.CableGrommetErrorFlange),
    FLANGE_THICKNESS(R.string.CableGrommetErrorFlangeThickness),
    TUBE_LENGTH(R.string.CableGrommetErrorTubeLength),
    SCREW_HOLE(R.string.CableGrommetErrorScrewHole),
}

internal object CableGrommetFormLogic {
    fun defaults() = CableGrommetFormValues()
    fun parse(text: String): Float = text.trim().replace(',', '.').toFloatOrNull() ?: Float.NaN

    fun validate(values: CableGrommetFormValues): CableGrommetIssue? {
        val diameter = parse(values.diameter)
        val clearance = parse(values.clearance)
        val wall = parse(values.wall)
        val flange = parse(values.flange)
        if (!inRange(diameter, 2f, 40f)) return CableGrommetIssue.DIAMETER
        if (!inRange(clearance, 0f, 1.5f)) return CableGrommetIssue.CLEARANCE
        if (!inRange(wall, 1.2f, 6f)) return CableGrommetIssue.WALL
        if (!inRange(flange, 30f, 100f) || flange <= diameter + 2f * (clearance + wall) + 10f) {
            return CableGrommetIssue.FLANGE
        }
        if (!inRange(parse(values.flangeThickness), 1.5f, 6f)) return CableGrommetIssue.FLANGE_THICKNESS
        if (!inRange(parse(values.tubeLength), 8f, 60f)) return CableGrommetIssue.TUBE_LENGTH
        if (!inRange(parse(values.screwHole), 2.5f, 8f)) return CableGrommetIssue.SCREW_HOLE
        return null
    }

    private fun inRange(value: Float, minimum: Float, maximum: Float) =
        value.isFinite() && value in minimum..maximum
}

@Composable
internal fun CableGrommetFormContent(
    values: CableGrommetFormValues,
    errorMessage: Int?,
    onValuesChanged: (CableGrommetFormValues) -> Unit,
    onGenerate: (CableGrommetFormValues) -> Unit,
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
            Text(stringResource(R.string.CableGrommetDescription), color = MaterialTheme.colorScheme.onSurfaceVariant)
            GeneratorDimensionField(R.string.CableGrommetDiameter, values.diameter) {
                onValuesChanged(values.copy(diameter = it))
            }
            GeneratorDimensionField(R.string.CableGrommetClearance, values.clearance) {
                onValuesChanged(values.copy(clearance = it))
            }
            GeneratorDimensionField(R.string.CableGrommetWall, values.wall) {
                onValuesChanged(values.copy(wall = it))
            }
            GeneratorDimensionField(R.string.CableGrommetFlange, values.flange) {
                onValuesChanged(values.copy(flange = it))
            }
            GeneratorDimensionField(R.string.CableGrommetFlangeThickness, values.flangeThickness) {
                onValuesChanged(values.copy(flangeThickness = it))
            }
            GeneratorDimensionField(R.string.CableGrommetTubeLength, values.tubeLength) {
                onValuesChanged(values.copy(tubeLength = it))
            }
            GeneratorDimensionField(R.string.CableGrommetScrewHole, values.screwHole) {
                onValuesChanged(values.copy(screwHole = it))
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
