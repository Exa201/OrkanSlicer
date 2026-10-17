package ru.ytkab0bp.slicebeam.components

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.ytkab0bp.slicebeam.R

internal data class CableClipFormValues(
    val diameter: String = "6",
    val clearance: String = "0.35",
    val wall: String = "2.4",
    val width: String = "12",
    val mouth: String = "4.8",
    val withMount: Boolean = true,
    val tabLength: String = "18",
    val screwHole: String = "4.2",
)

internal enum class CableClipIssue(val message: Int) {
    DIAMETER(R.string.CableClipErrorDiameter),
    CLEARANCE(R.string.CableClipErrorClearance),
    WALL(R.string.CableClipErrorWall),
    WIDTH(R.string.CableClipErrorWidth),
    MOUTH(R.string.CableClipErrorMouth),
    TAB_LENGTH(R.string.CableClipErrorTabLength),
    SCREW_HOLE(R.string.CableClipErrorScrewHole),
}

internal object CableClipFormLogic {
    fun defaults() = CableClipFormValues()
    fun parse(text: String): Float = text.trim().replace(',', '.').toFloatOrNull() ?: Float.NaN

    fun validate(values: CableClipFormValues): CableClipIssue? {
        val diameter = parse(values.diameter)
        val clearance = parse(values.clearance)
        val wall = parse(values.wall)
        val width = parse(values.width)
        val mouth = parse(values.mouth)
        if (!inRange(diameter, 2f, 60f)) return CableClipIssue.DIAMETER
        if (!inRange(clearance, 0f, 1.5f)) return CableClipIssue.CLEARANCE
        if (!inRange(wall, 1.2f, 6f)) return CableClipIssue.WALL
        if (!inRange(width, 4f, 40f)) return CableClipIssue.WIDTH
        if (!inRange(mouth, diameter * .4f, diameter) || mouth >= diameter) return CableClipIssue.MOUTH
        if (values.withMount) {
            if (!inRange(parse(values.tabLength), 8f, 40f)) return CableClipIssue.TAB_LENGTH
            val hole = parse(values.screwHole)
            if (!inRange(hole, 0f, 9.9f) || hole >= width + 4f) return CableClipIssue.SCREW_HOLE
        }
        return null
    }

    private fun inRange(value: Float, minimum: Float, maximum: Float) =
        value.isFinite() && value in minimum..maximum
}

@Composable
internal fun CableClipFormContent(
    values: CableClipFormValues,
    errorMessage: Int?,
    onValuesChanged: (CableClipFormValues) -> Unit,
    onGenerate: (CableClipFormValues) -> Unit,
) {
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * .68f
    Column(
        modifier = Modifier.fillMaxWidth().heightIn(max = maxHeight).padding(horizontal = 18.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.CableClipDescription), color = MaterialTheme.colorScheme.onSurfaceVariant)
            GeneratorDimensionField(R.string.CableClipDiameter, values.diameter) {
                onValuesChanged(values.copy(diameter = it))
            }
            GeneratorDimensionField(R.string.CableClipClearance, values.clearance) {
                onValuesChanged(values.copy(clearance = it))
            }
            GeneratorDimensionField(R.string.CableClipWall, values.wall) {
                onValuesChanged(values.copy(wall = it))
            }
            GeneratorDimensionField(R.string.CableClipWidth, values.width) {
                onValuesChanged(values.copy(width = it))
            }
            GeneratorDimensionField(R.string.CableClipMouth, values.mouth) {
                onValuesChanged(values.copy(mouth = it))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = values.withMount, onCheckedChange = { onValuesChanged(values.copy(withMount = it)) })
                Text(stringResource(R.string.CableClipMount), modifier = Modifier.weight(1f))
            }
            GeneratorDimensionField(R.string.CableClipTabLength, values.tabLength, enabled = values.withMount) {
                onValuesChanged(values.copy(tabLength = it))
            }
            GeneratorDimensionField(R.string.CableClipScrewHole, values.screwHole, enabled = values.withMount) {
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
