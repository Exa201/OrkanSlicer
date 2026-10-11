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
import kotlin.math.tan

internal data class PhoneStandFormValues(
    val width: String = "84",
    val deviceThickness: String = "10",
    val tilt: String = "20",
    val depth: String = "110",
    val backHeight: String = "100",
    val baseThickness: String = "6",
    val lipHeight: String = "16",
    val cableGap: String = "16",
    val cableDepth: String = "22",
)

internal enum class PhoneStandIssue(val message: Int) {
    WIDTH(R.string.PhoneStandErrorWidth),
    THICKNESS(R.string.PhoneStandErrorThickness),
    TILT(R.string.PhoneStandErrorTilt),
    DEPTH(R.string.PhoneStandErrorDepth),
    BACK_HEIGHT(R.string.PhoneStandErrorBackHeight),
    BASE(R.string.PhoneStandErrorBase),
    LIP(R.string.PhoneStandErrorLip),
    CABLE_GAP(R.string.PhoneStandErrorCableGap),
    CABLE_DEPTH(R.string.PhoneStandErrorCableDepth),
    SUPPORT_CLEARANCE(R.string.PhoneStandErrorSupportClearance),
}

/** Pure form validation for dimension and fit checks used by the phone stand geometry writer. */
internal object PhoneStandFormLogic {
    fun defaults() = PhoneStandFormValues()

    fun parse(text: String): Float = text.trim().replace(',', '.').toFloatOrNull() ?: Float.NaN

    fun validate(values: PhoneStandFormValues): PhoneStandIssue? {
        val width = parse(values.width)
        val thickness = parse(values.deviceThickness)
        val tilt = parse(values.tilt)
        val depth = parse(values.depth)
        val back = parse(values.backHeight)
        val base = parse(values.baseThickness)
        val lip = parse(values.lipHeight)
        val gap = parse(values.cableGap)
        val cableDepth = parse(values.cableDepth)
        if (!inRange(width, 55f, 140f)) return PhoneStandIssue.WIDTH
        if (!inRange(thickness, 4f, 18f)) return PhoneStandIssue.THICKNESS
        if (!inRange(tilt, 10f, 35f)) return PhoneStandIssue.TILT
        if (!inRange(depth, 65f, 160f)) return PhoneStandIssue.DEPTH
        if (!inRange(back, 50f, 140f)) return PhoneStandIssue.BACK_HEIGHT
        if (!inRange(base, 4f, 12f)) return PhoneStandIssue.BASE
        if (!inRange(lip, 8f, 28f)) return PhoneStandIssue.LIP
        if (!inRange(gap, 8f, 30f) || gap > width - 20f) return PhoneStandIssue.CABLE_GAP
        if (!inRange(cableDepth, 12f, 40f) || cableDepth > depth - 18f) {
            return PhoneStandIssue.CABLE_DEPTH
        }
        val supportBottom = depth - (back - base) * tan(Math.toRadians(tilt.toDouble())).toFloat()
        if (supportBottom - (thickness + 2f) < maxOf(12f, thickness + 4f) + 10f) {
            return PhoneStandIssue.SUPPORT_CLEARANCE
        }
        return null
    }

    private fun inRange(value: Float, minimum: Float, maximum: Float) =
        value.isFinite() && value in minimum..maximum
}

@Composable
internal fun PhoneStandFormContent(
    values: PhoneStandFormValues,
    errorMessage: Int?,
    onValuesChanged: (PhoneStandFormValues) -> Unit,
    onGenerate: (PhoneStandFormValues) -> Unit,
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
            Text(
                text = stringResource(R.string.PhoneStandDescription),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
            GeneratorDimensionField(R.string.PhoneStandWidth, values.width) {
                onValuesChanged(values.copy(width = it))
            }
            GeneratorDimensionField(R.string.PhoneStandDeviceThickness, values.deviceThickness) {
                onValuesChanged(values.copy(deviceThickness = it))
            }
            GeneratorDimensionField(R.string.PhoneStandTilt, values.tilt) {
                onValuesChanged(values.copy(tilt = it))
            }
            GeneratorDimensionField(R.string.PhoneStandDepth, values.depth) {
                onValuesChanged(values.copy(depth = it))
            }
            GeneratorDimensionField(R.string.PhoneStandBackHeight, values.backHeight) {
                onValuesChanged(values.copy(backHeight = it))
            }
            GeneratorDimensionField(R.string.PhoneStandBaseThickness, values.baseThickness) {
                onValuesChanged(values.copy(baseThickness = it))
            }
            GeneratorDimensionField(R.string.PhoneStandLipHeight, values.lipHeight) {
                onValuesChanged(values.copy(lipHeight = it))
            }
            GeneratorDimensionField(R.string.PhoneStandCableGap, values.cableGap) {
                onValuesChanged(values.copy(cableGap = it))
            }
            GeneratorDimensionField(R.string.PhoneStandCableDepth, values.cableDepth) {
                onValuesChanged(values.copy(cableDepth = it))
            }
            if (errorMessage != null) {
                Text(
                    text = stringResource(errorMessage),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Button(
            onClick = { onGenerate(values) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        ) {
            Text(stringResource(R.string.ConverterConvertSave), fontSize = 16.sp)
        }
    }
}
