package ru.ytkab0bp.slicebeam.components.bed_menu

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.slic3r.GCodeViewer
import ru.ytkab0bp.slicebeam.slic3r.GCodePreviewPolicy
import ru.ytkab0bp.slicebeam.slic3r.GCodePreviewSelection
import ru.ytkab0bp.slicebeam.utils.Prefs
import ru.ytkab0bp.slicebeam.utils.ViewUtils
import ru.ytkab0bp.slicebeam.ui.workspace.WorkspacePanelDimension
import ru.ytkab0bp.slicebeam.ui.workspace.WorkspacePanelSize

/** Hosts the post-slice layer range editor in the exclusive workspace overlay. */
internal fun showSliceLayersWorkspace(bed: BedFragment) {
    val context = bed.getContext() ?: return
    val controller = SliceLayersWorkspaceController(bed, context)
    val toolId = "slice-layer-preview"
    bed.showWorkspaceToolPanel(
        toolId = toolId,
        panelSize = WorkspacePanelSize(
            portrait = WorkspacePanelDimension.ClampedFraction(0.72f, minDp = 280f, maxDp = 380f),
            landscape = WorkspacePanelDimension.Fixed(380f),
        ),
        allowBackgroundInteraction = false,
        dockPanel = true,
        onDismiss = {
            controller.close()
            bed.clearWorkspaceToolPanel(toolId)
        },
    ) {
        DisposableEffect(controller) {
            controller.open()
            onDispose { controller.close() }
        }
        SliceLayersPanel(
            onClose = { bed.dismissWorkspaceToolPanel() },
            blockSize = controller.currentBlockSize,
            onBlockSizeChanged = controller::changePartSize,
            content = { controller.Content() },
        )
    }
}

@Composable
private fun SliceLayersPanel(
    onClose: () -> Unit,
    blockSize: Int,
    onBlockSizeChanged: (Int) -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).testTag("slice-layers-panel"),
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.MenuSliceLayers),
                modifier = Modifier.weight(1f).testTag("slice-layers-title"),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
            )
            IconButton(onClick = onClose, modifier = Modifier.heightIn(min = 48.dp).testTag("slice-layers-close")) {
                Text(stringResource(android.R.string.cancel), color = MaterialTheme.colorScheme.primary)
            }
        }
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                stringResource(R.string.GCodePreviewPartSize),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            listOf(25, 50, 100, 200).forEach { block ->
                FilterChip(
                    selected = block == blockSize,
                    onClick = { onBlockSizeChanged(block) },
                    label = { Text(block.toString()) },
                    modifier = Modifier.testTag("slice-preview-size-$block"),
                )
            }
        }
        Box(Modifier.fillMaxWidth().weight(1f)) { content() }
    }
}

private class SliceLayersWorkspaceController(
    private val bed: BedFragment,
    private val context: android.content.Context,
) {
    private var layerRange by mutableStateOf(SliceLayerRange(1, 1, 1))
    private var title by mutableStateOf(
        context.getString(R.string.MenuSliceInfoLayers, 1, 1),
    )
    private var blockSize by mutableIntStateOf(GCodePreviewPolicy.DEFAULT_BLOCK_SIZE)
    private var applyCallback: Runnable? = null
    private var destroyed = false
    private var openGeneration = 0

    private fun getViewer(): GCodeViewer? = bed.glView?.renderer?.viewer

    private fun updateTitle(from: Int, to: Int) {
        title = context.getString(R.string.MenuSliceInfoLayers, from, to)
    }

    private fun updateLayerRange(next: SliceLayerRange) {
        if (destroyed || next == layerRange) return
        layerRange = next
        updateTitle(next.from, next.to)
        scheduleApply(next.from, next.to)
    }

    private fun setFromFraction(fraction: Float) {
        val current = layerRange
        updateLayerRange(withSliceLayerFrom(current, layerFromSliderFraction(fraction, current.maximum)))
    }

    private fun setToFraction(fraction: Float) {
        val current = layerRange
        updateLayerRange(withSliceLayerTo(current, layerFromSliderFraction(fraction, current.maximum)))
    }

    private fun finishLayerSliderInteraction() {
        if (destroyed) return
        applyView(layerRange.from, layerRange.to)
    }

    private fun selectPart(part: Int) {
        val selection = GCodePreviewPolicy.blockRange(part, layerRange.maximum, blockSize)
        updateLayerRange(layerRange.copy(from = selection.from, to = selection.to))
        applyView(layerRange.from, layerRange.to)
    }

    fun changePartSize(size: Int) {
        blockSize = size
        selectPart(GCodePreviewPolicy.blockAt(layerRange.from, layerRange.maximum, size))
    }

    private fun showAllLayers() {
        updateLayerRange(layerRange.copy(from = 1, to = layerRange.maximum))
        applyView(layerRange.from, layerRange.to)
    }

    private fun applyView(from: Int, to: Int) {
        if (destroyed) return
        cancelPendingApply()
        val viewport = bed.glView ?: return
        val result = viewport.renderer.gcodeResult ?: return
        result.previewSelection = GCodePreviewSelection(from, to, blockSize)
        viewport.queueEvent {
            if (viewport.renderer.gcodeResult !== result) return@queueEvent
            val viewer = viewport.renderer.viewer ?: return@queueEvent
            viewer.applyPerformanceMode(Prefs.isPerformanceModeEnabled() || Prefs.isLowRamDevice())
            if (viewer.getLayersCount() > 0) viewer.setLayersViewRange(from.toLong() - 1L, to.toLong() - 1L)
            viewport.requestRender()
            viewport.postInvalidate()
        }
        updateTitle(from, to)
    }

    private fun scheduleApply(from: Int, to: Int) {
        if (destroyed) return
        cancelPendingApply()
        val callback = Runnable {
            applyCallback = null
            applyView(from, to)
        }
        applyCallback = callback
        ViewUtils.postOnMainThread(callback, 50L)
    }

    private fun cancelPendingApply() {
        applyCallback?.let { ViewUtils.removeCallbacks(it) }
        applyCallback = null
    }

    @Composable
    fun Content() {
        SliceLayersContent(
            title = title,
            range = layerRange,
            blockSize = blockSize,
            onPartSelected = ::selectPart,
            onPartSizeChanged = ::changePartSize,
            onShowAll = ::showAllLayers,
            onFromChanged = ::setFromFraction,
            onToChanged = ::setToFraction,
            onValueChangeFinished = ::finishLayerSliderInteraction,
        )
    }

    val currentBlockSize: Int get() = blockSize

    fun open() {
        destroyed = false
        val generation = ++openGeneration
        val viewport = bed.glView
        val renderer = viewport?.renderer
        val viewer = renderer?.viewer
        blockSize = bed.glView?.renderer?.gcodeResult?.previewSelection?.blockSize
            ?: GCodePreviewPolicy.DEFAULT_BLOCK_SIZE
        if (viewer != null) {
            val range = viewer.layersViewRange
            layerRange = normalizeSliceLayerRange(viewer.getLayersCount(), range.first, range.second)
            updateTitle(layerRange.from, layerRange.to)
            return
        }

        // The G-code viewer is initialized on the GL thread after its next frame.
        // The panel can be opened during that handoff; keep its header populated and
        // refresh the real range as soon as the renderer publishes the viewer.
        layerRange = normalizeSliceLayerRange(1L, 0L, 0L)
        updateTitle(layerRange.from, layerRange.to)
        if (viewport != null && renderer != null) {
            viewport.queueEvent {
                renderer.afterNextFrame {
                    if (destroyed || generation != openGeneration) return@afterNextFrame
                    val readyViewer = renderer.viewer ?: return@afterNextFrame
                    val range = readyViewer.layersViewRange
                    val readyRange = normalizeSliceLayerRange(
                        readyViewer.getLayersCount(), range.first, range.second,
                    )
                    ViewUtils.postOnMainThread {
                        if (destroyed || generation != openGeneration) return@postOnMainThread
                        layerRange = readyRange
                        blockSize = renderer.gcodeResult?.previewSelection?.blockSize
                            ?: GCodePreviewPolicy.DEFAULT_BLOCK_SIZE
                        updateTitle(layerRange.from, layerRange.to)
                    }
                }
                viewport.requestRender()
            }
        }
    }

    fun close() {
        if (destroyed) return
        openGeneration++
        applyView(layerRange.from, layerRange.to)
        destroyed = true
        cancelPendingApply()
    }
}

@Composable
private fun SliceLayersContent(
    title: String,
    range: SliceLayerRange,
    blockSize: Int,
    onPartSelected: (Int) -> Unit,
    onPartSizeChanged: (Int) -> Unit,
    onShowAll: () -> Unit,
    onFromChanged: (Float) -> Unit,
    onToChanged: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
) {
    val visibleTitle = title.ifBlank {
        stringResource(R.string.MenuSliceInfoLayers, range.from, range.to)
    }
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
    ) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState())
                .testTag("slice-layers-scroll-content"),
        ) {
        Text(
            text = visibleTitle,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).testTag("slice-layers-range-title"),
            color = MaterialTheme.colorScheme.primary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
        SliceLayerPartControls(
            range, blockSize, onPartSelected, onPartSizeChanged, onShowAll, showSizeOptions = false,
        )
        LayerRangeSlider(
            label = stringResource(R.string.GCodeLayerRangeStart),
            layer = range.from,
            maximum = range.maximum,
            onValueChange = onFromChanged,
            onValueChangeFinished = onValueChangeFinished,
        )
        LayerRangeSlider(
            label = stringResource(R.string.GCodeLayerRangeEnd),
            layer = range.to,
            maximum = range.maximum,
            onValueChange = onToChanged,
            onValueChangeFinished = onValueChangeFinished,
        )
        }
    }
}

@Composable
private fun LayerRangeSlider(
    label: String,
    layer: Int,
    maximum: Int,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "$layer / $maximum",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
            )
        }
        Slider(
            value = layerSliderFraction(layer, maximum),
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().height(52.dp)
                .semantics { contentDescription = "$label, $layer / $maximum" },
            enabled = maximum > 1,
            valueRange = 0f..1f,
            steps = 0,
            onValueChangeFinished = onValueChangeFinished,
        )
    }
}
