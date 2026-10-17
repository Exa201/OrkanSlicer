package ru.ytkab0bp.slicebeam.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import ru.ytkab0bp.slicebeam.R
import java.util.Locale

@Composable
internal fun CustomColorPickerDialog(
    initialColor: Int,
    onDismiss: () -> Unit,
    onColorChosen: (Int) -> Unit,
) {
    val hsv = remember(initialColor) {
        FloatArray(3).also { android.graphics.Color.colorToHSV(initialColor, it) }
    }
    var hue by remember(initialColor) { mutableFloatStateOf(hsv[0]) }
    var saturation by remember(initialColor) { mutableFloatStateOf(hsv[1]) }
    var brightness by remember(initialColor) { mutableFloatStateOf(hsv[2]) }
    val selected = remember(hue, saturation, brightness) {
        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, brightness)))
    }
    val hueColors = remember {
        listOf(0f, 60f, 120f, 180f, 240f, 300f, 360f).map { angle ->
            Color(android.graphics.Color.HSVToColor(floatArrayOf(angle, 1f, 1f)))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ColorChooserCustom)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(selected),
                )
                Text(
                    text = String.format(Locale.ROOT, "#%06X", android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, brightness)) and 0x00FFFFFF),
                    style = MaterialTheme.typography.labelLarge,
                )
                ColorSlider(
                    label = stringResource(R.string.CustomColorHue),
                    value = hue,
                    valueText = "${hue.toInt()}°",
                    valueRange = 0f..360f,
                    onValueChange = { hue = it },
                    track = hueColors,
                )
                ColorSlider(
                    label = stringResource(R.string.CustomColorSaturation),
                    value = saturation,
                    valueText = "${(saturation * 100).toInt()}%",
                    valueRange = 0f..1f,
                    onValueChange = { saturation = it },
                    track = listOf(
                        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0f, brightness))),
                        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, brightness))),
                    ),
                )
                ColorSlider(
                    label = stringResource(R.string.CustomColorBrightness),
                    value = brightness,
                    valueText = "${(brightness * 100).toInt()}%",
                    valueRange = 0f..1f,
                    onValueChange = { brightness = it },
                    track = listOf(Color.Black, Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, 1f)))),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onColorChosen(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, brightness))) }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.CustomColorApply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

@Composable
private fun ColorSlider(
    label: String,
    value: Float,
    valueText: String,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    track: List<Color>,
) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(valueText, style = MaterialTheme.typography.bodyMedium)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Brush.horizontalGradient(track)),
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(horizontal = 2.dp)
                .semantics {
                    contentDescription = label
                    stateDescription = valueText
                },
        )
    }
}
