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
import java.util.Locale

internal data class UtilityBoxFormValues(
    val width: String = "70",
    val depth: String = "50",
    val height: String = "35",
    val wall: String = "2",
    val floor: String = "2.4",
    val clearance: String = "0.3",
    val lidSkirt: String = "8",
    val cableHole: String = "0",
    val withLid: Boolean = true,
)

internal object UtilityBoxFormLogic {
    fun defaults() = UtilityBoxFormValues()
    fun parse(text: String): Float = text.trim().replace(',', '.').toFloatOrNull() ?: Float.NaN

    fun bounded(text: String, fallback: Float, minimum: Float, maximum: Float): Float {
        val value = text.trim().replace(',', '.').toFloatOrNull()?.takeIf(Float::isFinite) ?: fallback
        return value.coerceIn(minimum, maximum)
    }

    fun normalized(values: UtilityBoxFormValues) = UtilityBoxFormValues(
        width = format(bounded(values.width, 70f, 12f, 220f)),
        depth = format(bounded(values.depth, 50f, 12f, 220f)),
        height = format(bounded(values.height, 35f, 8f, 250f)),
        wall = format(bounded(values.wall, 2f, 1f, 10f)),
        floor = format(bounded(values.floor, 2.4f, 1f, 12f)),
        clearance = format(bounded(values.clearance, .3f, 0f, 1.5f)),
        lidSkirt = format(bounded(values.lidSkirt, 8f, 3f, 30f)),
        cableHole = format(bounded(values.cableHole, 0f, 0f, 60f)),
        withLid = values.withLid,
    )

    private fun format(value: Float) = String.format(Locale.US, "%.2f", value)
        .replace(Regex("0+$"), "").replace(Regex("\\.$"), "")
}

@Composable
internal fun UtilityBoxFormContent(
    values: UtilityBoxFormValues,
    onValuesChanged: (UtilityBoxFormValues) -> Unit,
    onGenerate: (UtilityBoxFormValues) -> Unit,
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
            Text(stringResource(R.string.UtilityBoxDescription), color = MaterialTheme.colorScheme.onSurfaceVariant)
            GeneratorDimensionField(R.string.UtilityBoxWidth, values.width) { onValuesChanged(values.copy(width = it)) }
            GeneratorDimensionField(R.string.UtilityBoxDepth, values.depth) { onValuesChanged(values.copy(depth = it)) }
            GeneratorDimensionField(R.string.UtilityBoxHeight, values.height) { onValuesChanged(values.copy(height = it)) }
            GeneratorDimensionField(R.string.UtilityBoxWall, values.wall) { onValuesChanged(values.copy(wall = it)) }
            GeneratorDimensionField(R.string.UtilityBoxFloor, values.floor) { onValuesChanged(values.copy(floor = it)) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = values.withLid, onCheckedChange = { onValuesChanged(values.copy(withLid = it)) })
                Text(stringResource(R.string.UtilityBoxIncludeLid), modifier = Modifier.weight(1f))
            }
            GeneratorDimensionField(R.string.UtilityBoxClearance, values.clearance, enabled = values.withLid) {
                onValuesChanged(values.copy(clearance = it))
            }
            GeneratorDimensionField(R.string.UtilityBoxLidSkirt, values.lidSkirt, enabled = values.withLid) {
                onValuesChanged(values.copy(lidSkirt = it))
            }
            GeneratorDimensionField(R.string.UtilityBoxCableHole, values.cableHole) {
                onValuesChanged(values.copy(cableHole = it))
            }
        }
        Button(onClick = { onGenerate(values) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Text(stringResource(R.string.ConverterConvertSave), fontSize = 16.sp)
        }
    }
}
