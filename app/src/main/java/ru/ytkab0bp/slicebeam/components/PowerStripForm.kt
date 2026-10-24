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

internal data class PowerStripFormValues(
    val length: String = "180",
    val width: String = "45",
    val height: String = "30",
    val clearance: String = "2",
    val base: String = "3",
    val stop: String = "12",
    val notch: String = "14",
    val screw: String = "4.2",
)

internal enum class PowerStripIssue(val message: Int) {
    LENGTH(R.string.PowerStripErrorLength),
    WIDTH(R.string.PowerStripErrorWidth),
    HEIGHT(R.string.PowerStripErrorHeight),
    CLEARANCE(R.string.PowerStripErrorClearance),
    BASE(R.string.PowerStripErrorBase),
    STOP(R.string.PowerStripErrorStop),
    NOTCH(R.string.PowerStripErrorNotch),
    SCREW(R.string.PowerStripErrorScrew),
}

internal object PowerStripFormLogic {
    fun defaults() = PowerStripFormValues()
    fun parse(text: String): Float = text.trim().replace(',', '.').toFloatOrNull() ?: Float.NaN

    fun validate(values: PowerStripFormValues): PowerStripIssue? {
        val length = parse(values.length)
        val width = parse(values.width)
        val clearance = parse(values.clearance)
        val notch = parse(values.notch)
        if (!inRange(length, 80f, 300f)) return PowerStripIssue.LENGTH
        if (!inRange(width, 30f, 100f)) return PowerStripIssue.WIDTH
        if (!inRange(parse(values.height), 10f, 60f)) return PowerStripIssue.HEIGHT
        if (!inRange(clearance, .5f, 5f)) return PowerStripIssue.CLEARANCE
        if (!inRange(parse(values.base), 2f, 6f)) return PowerStripIssue.BASE
        if (!inRange(parse(values.stop), 8f, 25f)) return PowerStripIssue.STOP
        if (!inRange(notch, 8f, 30f) || notch > width + 2f * clearance - 10f) return PowerStripIssue.NOTCH
        if (!inRange(parse(values.screw), 3f, 8f)) return PowerStripIssue.SCREW
        return null
    }

    private fun inRange(value: Float, minimum: Float, maximum: Float) =
        value.isFinite() && value in minimum..maximum
}

@Composable
internal fun PowerStripFormContent(
    values: PowerStripFormValues,
    errorMessage: Int?,
    onValuesChanged: (PowerStripFormValues) -> Unit,
    onGenerate: (PowerStripFormValues) -> Unit,
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
            Text(stringResource(R.string.PowerStripDescription), color = MaterialTheme.colorScheme.onSurfaceVariant)
            GeneratorDimensionField(R.string.PowerStripLength, values.length) { onValuesChanged(values.copy(length = it)) }
            GeneratorDimensionField(R.string.PowerStripWidth, values.width) { onValuesChanged(values.copy(width = it)) }
            GeneratorDimensionField(R.string.PowerStripHeight, values.height) { onValuesChanged(values.copy(height = it)) }
            GeneratorDimensionField(R.string.PowerStripClearance, values.clearance) { onValuesChanged(values.copy(clearance = it)) }
            GeneratorDimensionField(R.string.PowerStripBase, values.base) { onValuesChanged(values.copy(base = it)) }
            GeneratorDimensionField(R.string.PowerStripStop, values.stop) { onValuesChanged(values.copy(stop = it)) }
            GeneratorDimensionField(R.string.PowerStripNotch, values.notch) { onValuesChanged(values.copy(notch = it)) }
            GeneratorDimensionField(R.string.PowerStripScrew, values.screw) { onValuesChanged(values.copy(screw = it)) }
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
