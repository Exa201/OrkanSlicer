package ru.ytkab0bp.slicebeam.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Immutable
import ru.ytkab0bp.slicebeam.R
import java.util.Locale

@Immutable
internal data class TextTagFormValues(
    val text: String = "OrkanSlicer",
    val bold: Boolean = true,
    val width: String = "80",
    val height: String = "28",
    val fontSize: String = "10",
    val baseThickness: String = "2",
    val raisedHeight: String = "1.2",
    val holeDiameter: String = "5",
)

internal enum class TextTagIssue(val message: Int) {
    EMPTY_TEXT(R.string.TextTagNeedsText),
}

internal object TextTagFormLogic {
    fun defaults() = TextTagFormValues()
    fun parse(text: String): Float = text.trim().replace(',', '.').toFloatOrNull() ?: Float.NaN
    fun validate(values: TextTagFormValues): TextTagIssue? =
        if (values.text.isBlank()) TextTagIssue.EMPTY_TEXT else null

    fun normalized(values: TextTagFormValues): TextTagFormValues {
        val text = values.text.take(24)
        val width = bounded(values.width, 80f, 30f, 180f)
        val height = bounded(values.height, 28f, 18f, 70f)
        return values.copy(
            text = text,
            width = format(width),
            height = format(height),
            fontSize = format(bounded(values.fontSize, 10f, 3f, 30f)),
            baseThickness = format(bounded(values.baseThickness, 2f, .8f, 8f)),
            raisedHeight = format(bounded(values.raisedHeight, 1.2f, .4f, 5f)),
            holeDiameter = format(bounded(values.holeDiameter, 5f, 0f, minOf(width, height) * .55f)),
        )
    }

    private fun format(value: Float) = String.format(Locale.US, "%.2f", value)
        .replace(Regex("0+$"), "").replace(Regex("\\.$"), "")

    private fun bounded(text: String, fallback: Float, minimum: Float, maximum: Float): Float {
        val value = text.trim().replace(',', '.').toFloatOrNull()?.takeIf(Float::isFinite) ?: fallback
        return value.coerceIn(minimum, maximum)
    }
}

@Composable
internal fun TextTagFormContent(
    values: TextTagFormValues,
    errorMessage: Int?,
    onValuesChanged: (TextTagFormValues) -> Unit,
    onGenerate: (TextTagFormValues) -> Unit,
) {
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * .68f
    val previewText = if (values.text.isBlank()) stringResource(R.string.TextTagPreviewPlaceholder) else values.text
    Column(
        modifier = Modifier.fillMaxWidth().heightIn(max = maxHeight).padding(horizontal = 18.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.TextTagDescription), color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = values.text,
                onValueChange = { onValuesChanged(values.copy(text = it.take(24))) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.TextTagTextLabel)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                shape = MaterialTheme.shapes.medium,
            )
            Surface(
                modifier = Modifier.fillMaxWidth().heightIn(min = 68.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Text(
                    text = previewText,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 24.sp,
                    fontWeight = if (values.bold) FontWeight.Bold else FontWeight.Normal,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = values.bold, onCheckedChange = { onValuesChanged(values.copy(bold = it)) })
                Text(stringResource(R.string.TextTagBold), modifier = Modifier.weight(1f))
            }
            GeneratorDimensionField(R.string.TextTagWidth, values.width) { onValuesChanged(values.copy(width = it)) }
            GeneratorDimensionField(R.string.TextTagHeight, values.height) { onValuesChanged(values.copy(height = it)) }
            GeneratorDimensionField(R.string.TextTagFontSize, values.fontSize) { onValuesChanged(values.copy(fontSize = it)) }
            GeneratorDimensionField(R.string.TextTagBaseThickness, values.baseThickness) {
                onValuesChanged(values.copy(baseThickness = it))
            }
            GeneratorDimensionField(R.string.TextTagRaisedHeight, values.raisedHeight) {
                onValuesChanged(values.copy(raisedHeight = it))
            }
            GeneratorDimensionField(R.string.TextTagHoleDiameter, values.holeDiameter) {
                onValuesChanged(values.copy(holeDiameter = it))
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
