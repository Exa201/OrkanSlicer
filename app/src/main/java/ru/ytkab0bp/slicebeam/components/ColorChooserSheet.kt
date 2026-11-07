package ru.ytkab0bp.slicebeam.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import ru.ytkab0bp.slicebeam.utils.CssColors
import java.util.Locale

/** Compose color palette used by filament editors and profile-setting rows. */
@Composable
internal fun ColorChooserDialog(
    initialColor: Int,
    onDismiss: () -> Unit,
    onColorChosen: (Int) -> Unit,
    resetLabel: String? = null,
    onReset: (() -> Unit)? = null,
) {
    var showCustomPicker by remember { mutableStateOf(false) }
    if (showCustomPicker) {
        CustomColorPickerDialog(
            initialColor = initialColor,
            onDismiss = { showCustomPicker = false },
            onColorChosen = { color ->
                onColorChosen(color or OPAQUE_ALPHA)
                onDismiss()
            },
        )
    } else {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                BeamComposeTheme {
                    ColorChooserContent(
                        onPickColor = { color ->
                            onColorChosen(color or OPAQUE_ALPHA)
                            onDismiss()
                        },
                        onOpenWheel = { showCustomPicker = true },
                        resetLabel = resetLabel,
                        onReset = onReset?.let { reset ->
                            {
                                reset()
                                onDismiss()
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorChooserContent(
    onPickColor: (Int) -> Unit,
    onOpenWheel: () -> Unit,
    resetLabel: String? = null,
    onReset: (() -> Unit)? = null,
) {
    val configuration = LocalConfiguration.current
    val columns = ColorChooserPalette.columnsForWidth(configuration.screenWidthDp)
    val commonColors = remember { ColorChooserPalette.commonColors() }
    val allColors = remember { ColorChooserPalette.allColors() }
    val shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = configuration.screenHeightDp.dp * 0.9f)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.ColorChooserCustom),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleSmall,
        )
        Button(
            onClick = onOpenWheel,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Text(stringResource(R.string.OpenCustomColorPicker), fontSize = 16.sp)
        }
        if (resetLabel != null && onReset != null) {
            Button(
                onClick = onReset,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Text(resetLabel, fontSize = 16.sp)
            }
        }

        ColorSectionTitle(R.string.ColorChooserCommon)
        ColorSwatchGrid(colors = commonColors, columns = columns, onPickColor = onPickColor)

        ColorSectionTitle(R.string.ColorChooserCss)
        ColorSwatchGrid(colors = allColors, columns = columns, onPickColor = onPickColor)
    }
}

@Composable
private fun ColorSectionTitle(textRes: Int) {
    Text(
        text = stringResource(textRes),
        modifier = Modifier.padding(top = 8.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.titleSmall,
    )
}

@Composable
private fun ColorSwatchGrid(
    colors: List<Int>,
    columns: Int,
    onPickColor: (Int) -> Unit,
) {
    Column {
        colors.chunked(columns).forEach { rowColors ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                rowColors.forEach { color ->
                    ColorSwatch(color = color, onClick = { onPickColor(color) })
                }
                if (rowColors.size < columns) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ColorSwatch(color: Int, onClick: () -> Unit) {
    val opaqueColor = color or OPAQUE_ALPHA
    val shape = RoundedCornerShape(8.dp)
    val colorLabel = stringResource(
        R.string.ColorChooserSwatchDescription,
        String.format(Locale.ROOT, "#%06X", color and COLOR_MASK),
    )
    val swatchModifier = Modifier
        .padding(SWATCH_MARGIN_DP.dp)
        .size(SWATCH_SIZE_DP.dp)
        .clip(shape)
        .background(Color(opaqueColor))
        .then(
            if (ColorUtils.calculateLuminance(opaqueColor) > WHITE_OUTLINE_THRESHOLD) {
                Modifier.border(1.dp, MaterialTheme.colorScheme.outline, shape)
            } else {
                Modifier
            },
        )
        .semantics { contentDescription = colorLabel }
        .clickable(role = Role.Button, onClick = onClick)

    Box(
        modifier = swatchModifier,
        contentAlignment = Alignment.Center,
    ) {}
}

/** Palette data and width policy are kept outside Compose so they can be verified on the JVM. */
internal object ColorChooserPalette {
    fun commonColors(): List<Int> = CssColors.COMMON.mapNotNull { CssColors.ALL[it] }

    fun allColors(): List<Int> = CssColors.ALL.values.toList()

    fun columnsForWidth(screenWidthDp: Int): Int =
        ((screenWidthDp - HORIZONTAL_PADDING_DP) / CELL_WIDTH_DP).coerceIn(MIN_COLUMNS, MAX_COLUMNS)

    private const val HORIZONTAL_PADDING_DP = 32
    private const val CELL_WIDTH_DP = 52
    private const val MIN_COLUMNS = 2
    private const val MAX_COLUMNS = 8
}

private const val OPAQUE_ALPHA = -0x1000000
private const val COLOR_MASK = 0xFFFFFF
private const val SWATCH_SIZE_DP = 44
private const val SWATCH_MARGIN_DP = 4
private const val WHITE_OUTLINE_THRESHOLD = 0.85
