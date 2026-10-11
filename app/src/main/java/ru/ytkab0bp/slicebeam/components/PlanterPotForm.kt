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

internal data class PlanterPotFormValues(
    val bottomDiameter: String = "70",
    val topDiameter: String = "80",
    val height: String = "80",
    val wall: String = "2.4",
    val base: String = "3",
    val drain: String = "5",
    val withSaucer: Boolean = true,
    val saucerClearance: String = "3",
    val saucerWall: String = "2",
    val saucerHeight: String = "8",
    val saucerBase: String = "2",
)

internal enum class PlanterPotIssue(val message: Int) {
    BOTTOM(R.string.PlanterErrorBottom),
    TOP(R.string.PlanterErrorTop),
    HEIGHT(R.string.PlanterErrorHeight),
    WALL(R.string.PlanterErrorWall),
    BASE(R.string.PlanterErrorBase),
    DRAIN(R.string.PlanterErrorDrain),
    SAUCER_CLEARANCE(R.string.PlanterErrorSaucerClearance),
    SAUCER_WALL(R.string.PlanterErrorSaucerWall),
    SAUCER_HEIGHT(R.string.PlanterErrorSaucerHeight),
    SAUCER_BASE(R.string.PlanterErrorSaucerBase),
}

/** Pure parsing and dimension rules, kept separate so invalid inputs can be tested without Android. */
internal object PlanterPotFormLogic {
    fun defaults() = PlanterPotFormValues()

    fun parse(text: String): Float = text.trim().replace(',', '.').toFloatOrNull() ?: Float.NaN

    fun validate(values: PlanterPotFormValues): PlanterPotIssue? {
        val bottom = parse(values.bottomDiameter)
        val top = parse(values.topDiameter)
        val height = parse(values.height)
        val wall = parse(values.wall)
        val base = parse(values.base)
        val drain = parse(values.drain)
        if (!inRange(bottom, 30f, 180f)) return PlanterPotIssue.BOTTOM
        if (!inRange(top, 30f, 200f)) return PlanterPotIssue.TOP
        if (!inRange(height, 20f, 220f)) return PlanterPotIssue.HEIGHT
        if (!inRange(wall, 1.2f, 5f)) return PlanterPotIssue.WALL
        if (!inRange(base, 1.5f, 8f)) return PlanterPotIssue.BASE
        if (!inRange(drain, 2f, 20f) || drain >= bottom - 2f * wall) return PlanterPotIssue.DRAIN
        if (!values.withSaucer) return null
        if (!inRange(parse(values.saucerClearance), 1f, 8f)) return PlanterPotIssue.SAUCER_CLEARANCE
        if (!inRange(parse(values.saucerWall), 1.2f, 6f)) return PlanterPotIssue.SAUCER_WALL
        if (!inRange(parse(values.saucerHeight), 4f, 25f)) return PlanterPotIssue.SAUCER_HEIGHT
        if (!inRange(parse(values.saucerBase), 1.2f, 5f)) return PlanterPotIssue.SAUCER_BASE
        return null
    }

    private fun inRange(value: Float, minimum: Float, maximum: Float) =
        value.isFinite() && value in minimum..maximum
}

@Composable
internal fun PlanterPotFormContent(
    values: PlanterPotFormValues,
    errorMessage: Int?,
    onValuesChanged: (PlanterPotFormValues) -> Unit,
    onGenerate: (PlanterPotFormValues) -> Unit,
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
                text = stringResource(R.string.PlanterDescription),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
            GeneratorDimensionField(R.string.PlanterBottomDiameter, values.bottomDiameter) {
                onValuesChanged(values.copy(bottomDiameter = it))
            }
            GeneratorDimensionField(R.string.PlanterTopDiameter, values.topDiameter) {
                onValuesChanged(values.copy(topDiameter = it))
            }
            GeneratorDimensionField(R.string.PlanterHeight, values.height) {
                onValuesChanged(values.copy(height = it))
            }
            GeneratorDimensionField(R.string.PlanterWall, values.wall) {
                onValuesChanged(values.copy(wall = it))
            }
            GeneratorDimensionField(R.string.PlanterBase, values.base) {
                onValuesChanged(values.copy(base = it))
            }
            GeneratorDimensionField(R.string.PlanterDrain, values.drain) {
                onValuesChanged(values.copy(drain = it))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = values.withSaucer,
                    onCheckedChange = { onValuesChanged(values.copy(withSaucer = it)) },
                )
                Text(
                    text = stringResource(R.string.PlanterIncludeSaucer),
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            GeneratorDimensionField(
                R.string.PlanterSaucerClearance, values.saucerClearance, enabled = values.withSaucer,
            ) { onValuesChanged(values.copy(saucerClearance = it)) }
            GeneratorDimensionField(
                R.string.PlanterSaucerWall, values.saucerWall, enabled = values.withSaucer,
            ) { onValuesChanged(values.copy(saucerWall = it)) }
            GeneratorDimensionField(
                R.string.PlanterSaucerHeight, values.saucerHeight, enabled = values.withSaucer,
            ) { onValuesChanged(values.copy(saucerHeight = it)) }
            GeneratorDimensionField(
                R.string.PlanterSaucerBase, values.saucerBase, enabled = values.withSaucer,
            ) { onValuesChanged(values.copy(saucerBase = it)) }
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
