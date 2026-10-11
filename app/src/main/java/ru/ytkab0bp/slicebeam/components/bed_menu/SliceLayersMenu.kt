package ru.ytkab0bp.slicebeam.components.bed_menu

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.components.UnfoldMenu
import ru.ytkab0bp.slicebeam.slic3r.GCodeViewer
import ru.ytkab0bp.slicebeam.slic3r.GCodePreviewPolicy
import ru.ytkab0bp.slicebeam.slic3r.GCodePreviewSelection
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import ru.ytkab0bp.slicebeam.ui.ThemeAwareComposeView
import ru.ytkab0bp.slicebeam.utils.Prefs
import ru.ytkab0bp.slicebeam.utils.ViewUtils

/** Layer-range editor for the sliced G-code preview. */
class SliceLayersMenu : UnfoldMenu() {
    private var layerRange by mutableStateOf(SliceLayerRange(1, 1, 1))
    private var title by mutableStateOf("")
    private var blockSize by mutableIntStateOf(GCodePreviewPolicy.DEFAULT_BLOCK_SIZE)
    private var applyCallback: Runnable? = null
    private var destroyed = false

    private fun getViewer(): GCodeViewer? =
        fragment?.glView?.renderer?.viewer

    private fun updateTitle(from: Int, to: Int) {
        val context = fragment?.getContext() ?: return
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

    private fun changePartSize(size: Int) {
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
        val viewport = fragment?.glView ?: return
        val result = viewport.renderer.gcodeResult ?: return
        result.previewSelection = GCodePreviewSelection(from, to, blockSize)
        viewport.queueEvent {
            if (viewport.renderer.gcodeResult !== result) return@queueEvent
            val viewer = viewport.renderer.viewer ?: return@queueEvent
            viewer.applyPerformanceMode(Prefs.isPerformanceModeEnabled())
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

    override fun onCreateView(ctx: Context, portrait: Boolean): View = ThemeAwareComposeView(ctx).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
        setContent {
            BeamComposeTheme(themeRevision = themeRevision) {
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
                    onDismiss = ::dismiss,
                )
            }
        }
    }

    override fun getRequestedSize(into: FrameLayout, portrait: Boolean): Int =
        if (portrait) {
            minOf(ViewUtils.dp(410), (into.height * .7f).toInt())
        } else {
            super.getRequestedSize(into, false)
        }

    override fun onCreate() {
        super.onCreate()
        destroyed = false
        val viewer = getViewer() ?: return
        val range = viewer.layersViewRange
        blockSize = fragment?.glView?.renderer?.gcodeResult?.previewSelection?.blockSize
            ?: GCodePreviewPolicy.DEFAULT_BLOCK_SIZE
        layerRange = normalizeSliceLayerRange(viewer.getLayersCount(), range.first, range.second)
        updateTitle(layerRange.from, layerRange.to)
    }

    override fun onDestroy() {
        applyView(layerRange.from, layerRange.to)
        super.onDestroy()
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
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
        Text(
            text = title,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.primary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
        SliceLayerPartControls(range, blockSize, onPartSelected, onPartSizeChanged, onShowAll)
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
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier.fillMaxWidth().height(52.dp)
                .clickable(onClick = onDismiss)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            androidx.compose.material3.Icon(
                painter = painterResource(R.drawable.arrow_left_outline_28),
                contentDescription = stringResource(R.string.MenuOrientationPositionBack),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.MenuOrientationPositionBack),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
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
