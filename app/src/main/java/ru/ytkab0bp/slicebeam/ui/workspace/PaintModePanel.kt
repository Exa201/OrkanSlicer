package ru.ytkab0bp.slicebeam.ui.workspace

import android.graphics.Color as AndroidColor
import androidx.core.graphics.ColorUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.render.GLRenderer
import ru.ytkab0bp.slicebeam.utils.FilamentSlot
import ru.ytkab0bp.slicebeam.utils.Prefs
import ru.ytkab0bp.slicebeam.view.GLView

/** Compose controls for paint sessions. All renderer mutations are queued on the GL thread. */
@Composable
internal fun PaintModePanel(
    glView: GLView,
    mode: Int,
    onCommit: (baseFilament: Int?) -> Unit,
    onCancel: () -> Unit,
) {
    val slots = remember { Prefs.getFilamentSlots() }
    val colorMode = mode == GLRenderer.PAINT_MODE_COLOR
    var selectedFilament by remember { mutableIntStateOf(if (colorMode && slots.size >= 2) 2 else 1) }
    var selectedBaseFilament by remember { mutableIntStateOf(0) }
    var selectedTool by remember { mutableIntStateOf(GLRenderer.PAINT_TOOL_BRUSH) }
    var brushEnabled by remember { mutableStateOf(true) }
    var brushSphere by remember { mutableStateOf(glView.renderer.isBrushSphere) }
    var brushRadius by remember { mutableFloatStateOf(4f) }
    var heightBand by remember { mutableFloatStateOf(10f) }
    var bucketAngle by remember { mutableFloatStateOf(30f) }

    fun onGl(action: () -> Unit) {
        glView.queueEvent {
            action()
            glView.requestRender()
        }
    }

    LaunchedEffect(glView, mode) {
        onGl {
            glView.renderer.paintFilament = selectedFilament
            glView.renderer.paintTool = selectedTool
            glView.renderer.paintBrushRadius = brushRadius.toDouble()
            glView.renderer.heightBandWidth = heightBand.toDouble()
            glView.renderer.bucketAngleThreshold = bucketAngle.toDouble()
            glView.renderer.isBrushSphere = brushSphere
        }
    }
    DisposableEffect(glView) {
        onDispose { onCancel() }
    }

    Column(
        Modifier.fillMaxWidth().heightIn(max = 640.dp).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.PaintWith), style = MaterialTheme.typography.titleMedium)
        if (colorMode) {
            Text(stringResource(R.string.PaintBaseFilament), style = MaterialTheme.typography.labelLarge)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                slots.forEachIndexed { index, slot ->
                    FilamentSwatch(slot, index + 1, selected = selectedBaseFilament == index + 1) {
                        selectedBaseFilament = index + 1
                        // Keep uniform reassignment as a draft. Clearing the session selector
                        // prevents commit from restoring stale 3MF facet assignments.
                        onGl { glView.renderer.clearPaintDraftForUniformFilament() }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                slots.forEachIndexed { index, slot ->
                    FilamentSwatch(slot, index + 1, selected = selectedFilament == index + 1) {
                        selectedFilament = index + 1
                        onGl { glView.renderer.paintFilament = index + 1 }
                    }
                }
            }
        } else {
            val states = buildList {
                add(1 to if (mode == GLRenderer.PAINT_MODE_FUZZY) R.string.PaintFuzzy else R.string.PaintEnforce)
                if (mode != GLRenderer.PAINT_MODE_FUZZY) add(2 to R.string.PaintBlock)
                add(0 to R.string.PaintErase)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                states.forEach { (value, label) ->
                    FilterChip(
                        selected = selectedFilament == value,
                        onClick = { selectedFilament = value; onGl { glView.renderer.paintFilament = value } },
                        label = { Text(stringResource(label)) },
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                GLRenderer.PAINT_TOOL_BRUSH to R.string.PaintBrush,
                GLRenderer.PAINT_TOOL_BUCKET to R.string.PaintBucket,
                GLRenderer.PAINT_TOOL_HEIGHT to R.string.PaintHeight,
            ).forEach { (tool, label) ->
                FilterChip(
                    selected = selectedTool == tool,
                    onClick = { selectedTool = tool; onGl { glView.renderer.paintTool = tool } },
                    label = { Text(stringResource(label)) },
                )
            }
        }
        if (selectedTool == GLRenderer.PAINT_TOOL_BRUSH) {
            Text(stringResource(R.string.PaintBrushRadius, brushRadius))
            Slider(
                value = brushRadius,
                onValueChange = { brushRadius = it; onGl { glView.renderer.paintBrushRadius = it.toDouble() } },
                valueRange = 1f..20f,
                modifier = Modifier.semantics { contentDescription = glView.context.getString(R.string.PaintBrushRadius, brushRadius) },
            )
            OutlinedButton(onClick = { brushSphere = !brushSphere; onGl { glView.renderer.isBrushSphere = brushSphere } }) {
                Text(stringResource(if (brushSphere) R.string.PaintBrushSphere else R.string.PaintBrushCircle))
            }
        }
        if (selectedTool == GLRenderer.PAINT_TOOL_HEIGHT) {
            Text(stringResource(R.string.PaintHeightBand, heightBand.toDouble()))
            Slider(value = heightBand, onValueChange = { heightBand = it; onGl { glView.renderer.heightBandWidth = it.toDouble() } }, valueRange = 1f..100f)
        }
        if (selectedTool == GLRenderer.PAINT_TOOL_BUCKET) {
            Text(stringResource(R.string.PaintAngleLimit, bucketAngle.toInt()))
            Slider(value = bucketAngle, onValueChange = { bucketAngle = it; onGl { glView.renderer.bucketAngleThreshold = it.toDouble() } }, valueRange = 1f..90f)
        }
        OutlinedButton(
            onClick = { brushEnabled = !brushEnabled; glView.isPaintBrushActive = brushEnabled },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) { Text(stringResource(if (brushEnabled) R.string.PaintDisableBrush else R.string.PaintEnableBrush)) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onGl { glView.renderer.clearPaint() } }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                Text(stringResource(R.string.ActionClear))
            }
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                Text(stringResource(android.R.string.cancel))
            }
                Button(
                    onClick = { onCommit(selectedBaseFilament.takeIf { it > 0 }) },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                ) {
                Text(stringResource(R.string.ActionDone))
            }
        }
    }
}

@Composable
private fun FilamentSwatch(slot: FilamentSlot, index: Int, selected: Boolean = false, onClick: () -> Unit) {
    val type = slot.type?.takeIf(String::isNotBlank) ?: stringResource(R.string.FilamentType)
    val description = stringResource(R.string.FilamentPaletteSlotDescription, index, type)
    val swatchColor = Color(slot.color or 0xFF000000.toInt())
    val textColor = if (ColorUtils.calculateLuminance(slot.color) > 0.5) AndroidColor.BLACK else AndroidColor.WHITE
    Box(
        modifier = Modifier.size(48.dp)
            .clip(CircleShape)
            .background(swatchColor)
            .border(3.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description; this.selected = selected },
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        Text(index.toString(), color = Color(textColor), style = MaterialTheme.typography.labelLarge)
    }
}
