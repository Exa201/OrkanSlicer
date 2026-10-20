package ru.ytkab0bp.slicebeam.components.bed_menu

import android.content.Context
import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.components.WorkspaceComposeToolRoute
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import ru.ytkab0bp.slicebeam.ui.ThemeAwareComposeView
import ru.ytkab0bp.slicebeam.utils.Vec3d
import ru.ytkab0bp.slicebeam.ui.workspace.WorkspacePanelDimension
import ru.ytkab0bp.slicebeam.ui.workspace.WorkspacePanelSize
import ru.ytkab0bp.slicebeam.utils.ViewUtils
import kotlin.math.round
import kotlin.math.roundToInt

/** Compose editor for the cut plane and the optional connectors between cut pieces. */
class TransformCutMenu(private val bed: BedFragment) : WorkspaceComposeToolRoute() {
    private val tempMin = Vec3d()
    private val tempMax = Vec3d()
    private var cutObject = -1
    private var connectorMode = false
    @Volatile private var isAttachedToWorkspace = false
    private val cutReady = mutableStateOf(false)
    private var zRange = TransformCutPolicy.zRange(0.0, 1.0)
    private val cutZ = mutableFloatStateOf(zRange.initial)
    private val rotationX = mutableFloatStateOf(0f)
    private val rotationY = mutableFloatStateOf(0f)
    private val connectorRadius = mutableFloatStateOf(5f)
    private val connectorHeight = mutableFloatStateOf(10f)
    private val connectorType = mutableIntStateOf(0)
    private val connectorCount = mutableIntStateOf(0)
    private val connectorModeState = mutableStateOf(false)

    init {
        fragment = bed
    }

    override fun onCreate() {
        super.onCreate()
        isAttachedToWorkspace = true
        val glView = bed.glView ?: return
        glView.queueEvent {
            if (!isAttachedToWorkspace) return@queueEvent
            val renderer = glView.renderer
            val objectIndex = renderer.selectedObject
            val min = Vec3d()
            val max = Vec3d()
            val range = runCatching {
                val model = renderer.model ?: return@runCatching null
                if (objectIndex < 0) return@runCatching null
                model.getBoundingBoxExact(objectIndex, min, max)
                TransformCutPolicy.zRange(min.z, max.z)
            }.getOrNull()
            ViewUtils.postOnMainThread {
                if (!isAttachedToWorkspace) return@postOnMainThread
                cutObject = if (range == null) -1 else objectIndex
                if (range != null) {
                    tempMin.set(min)
                    tempMax.set(max)
                    zRange = range
                    cutZ.floatValue = range.initial
                    cutReady.value = true
                }
                val z = cutZ.floatValue
                val rotX = TransformCutPolicy.degreesToRadians(rotationX.floatValue)
                val rotY = TransformCutPolicy.degreesToRadians(rotationY.floatValue)
                val planeMin = Vec3d(tempMin)
                val planeMax = Vec3d(tempMax)
                val radius = connectorRadius.floatValue
                val height = connectorHeight.floatValue
                val type = connectorType.intValue
                glView.queueEvent {
                    if (!isAttachedToWorkspace) return@queueEvent
                    val activeRenderer = glView.renderer
                    activeRenderer.showCutPlane(true)
                    activeRenderer.setCutConnectorPlacement(false)
                    if (range != null) activeRenderer.setCutPlane(z, rotX, rotY, planeMin, planeMax)
                    activeRenderer.setCutConnectorDefaults(radius, height, type)
                    updateConnectorCountOnGl(activeRenderer)
                    glView.requestRender()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isAttachedToWorkspace = false
        connectorModeState.value = false
        connectorMode = false
        val glView = bed.glView ?: return
        glView.queueEvent {
            val renderer = glView.renderer
            renderer.setCutConnectorPlacement(false)
            if (cutObject >= 0) renderer.discardCutConnectorPreviewsForObject(cutObject)
            renderer.showCutPlane(false)
            glView.requestRender()
        }
    }

    private fun updateCutPlaneInRenderer() {
        val glView = bed.glView ?: return
        val z = cutZ.floatValue
        val rotX = TransformCutPolicy.degreesToRadians(rotationX.floatValue)
        val rotY = TransformCutPolicy.degreesToRadians(rotationY.floatValue)
        val min = Vec3d(tempMin.x, tempMin.y, tempMin.z)
        val max = Vec3d(tempMax.x, tempMax.y, tempMax.z)
        glView.queueEvent {
            glView.renderer.setCutPlane(z, rotX, rotY, min, max)
            glView.requestRender()
        }
    }

    private fun updateConnectorCount() {
        if (cutObject < 0) {
            connectorCount.intValue = 0
            return
        }
        val glView = bed.glView ?: return
        glView.queueEvent {
            if (!isAttachedToWorkspace) return@queueEvent
            val count = glView.renderer.getCutConnectorCount(cutObject)
            ViewUtils.postOnMainThread {
                if (isAttachedToWorkspace && connectorModeState.value) connectorCount.intValue = count
            }
        }
    }

    private fun updateConnectorSettings() {
        val glView = bed.glView ?: return
        val radius = connectorRadius.floatValue
        val height = connectorHeight.floatValue
        val type = connectorType.intValue
        glView.queueEvent {
            if (!isAttachedToWorkspace) return@queueEvent
            glView.renderer.setCutConnectorDefaults(radius, height, type)
            glView.requestRender()
        }
    }

    private fun updateConnectorCountOnGl(renderer: ru.ytkab0bp.slicebeam.render.GLRenderer) {
        if (cutObject < 0) return
        val count = renderer.getCutConnectorCount(cutObject)
        ViewUtils.postOnMainThread {
            if (isAttachedToWorkspace) connectorCount.intValue = count
        }
    }

    private fun setCutZ(value: Float) {
        cutZ.floatValue = TransformCutPolicy.roundHundredths(value)
            .coerceIn(zRange.min, zRange.max)
        updateCutPlaneInRenderer()
    }

    private fun setRotationX(value: Float) {
        rotationX.floatValue = TransformCutPolicy.roundHundredths(value).coerceIn(-180f, 180f)
        updateCutPlaneInRenderer()
    }

    private fun setRotationY(value: Float) {
        rotationY.floatValue = TransformCutPolicy.roundHundredths(value).coerceIn(-180f, 180f)
        updateCutPlaneInRenderer()
    }

    private fun setConnectorRadius(value: Float) {
        connectorRadius.floatValue = TransformCutPolicy.roundHundredths(value).coerceIn(1f, 30f)
        updateConnectorSettings()
    }

    private fun setConnectorHeight(value: Float) {
        connectorHeight.floatValue = TransformCutPolicy.roundHundredths(value).coerceIn(1f, 50f)
        updateConnectorSettings()
    }

    private fun setConnectorType(value: Float) {
        connectorType.intValue = TransformCutPolicy.connectorType(value)
        updateConnectorSettings()
    }

    private fun toggleConnectorMode() {
        connectorMode = !connectorMode
        connectorModeState.value = connectorMode
        val glView = bed.glView ?: return
        glView.queueEvent {
            if (!isAttachedToWorkspace) return@queueEvent
            glView.renderer.setCutConnectorPlacement(connectorMode)
            glView.requestRender()
        }
    }

    private fun undoConnector() {
        if (cutObject < 0) return
        val glView = bed.glView ?: return
        glView.queueEvent {
            if (!isAttachedToWorkspace) return@queueEvent
            glView.renderer.removeLastCutConnectorForObject(cutObject)
            updateConnectorCountOnGl(glView.renderer)
            glView.requestRender()
        }
    }

    private fun clearConnectors() {
        if (cutObject < 0) return
        val glView = bed.glView ?: return
        glView.queueEvent {
            if (!isAttachedToWorkspace) return@queueEvent
            glView.renderer.clearCutConnectorsForObject(cutObject)
            updateConnectorCountOnGl(glView.renderer)
            glView.requestRender()
        }
    }

    private fun applyCut() {
        val glView = bed.glView
        if (cutObject >= 0 && cutReady.value && glView != null) {
            val objectIndex = cutObject
            val z = cutZ.floatValue.toDouble()
            val rotX = TransformCutPolicy.degreesToRadians(rotationX.floatValue).toDouble()
            val rotY = TransformCutPolicy.degreesToRadians(rotationY.floatValue).toDouble()
            glView.queueEvent {
                if (!isAttachedToWorkspace) return@queueEvent
                val renderer = glView.renderer
                val model = renderer.model ?: return@queueEvent
                renderer.commitCutConnectorsForObject(objectIndex)
                val applied = model.cut(objectIndex, z, rotX, rotY, true, true)
                renderer.clearAllCutConnectorPreviews()
                ViewUtils.postOnMainThread {
                    if (applied) bed.updateModel()
                    if (isAttachedToWorkspace) dismiss()
                }
            }
            return
        }
        dismiss()
    }

    @Composable
    override fun ComposeContent(portrait: Boolean) {
        val zValue = cutZ.floatValue
        val xDegrees = rotationX.floatValue
        val yDegrees = rotationY.floatValue
        val radius = connectorRadius.floatValue
        val height = connectorHeight.floatValue
        val type = connectorType.intValue
        val addingConnectors = connectorModeState.value
        val count = connectorCount.intValue
        LaunchedEffect(addingConnectors, cutObject) {
            if (addingConnectors && cutObject >= 0) {
                while (isActive) {
                    updateConnectorCount()
                    delay(CONNECTOR_COUNT_REFRESH_MS)
                }
            }
        }
        TransformCutEditorContent(
            zValue = zValue,
            zMin = zRange.min,
            zMax = zRange.max,
            rotationX = xDegrees,
            rotationY = yDegrees,
            connectorRadius = radius,
            connectorHeight = height,
            connectorType = type,
            connectorCount = count,
            connectorMode = addingConnectors,
            canCut = cutReady.value && cutObject >= 0,
            onZChanged = ::setCutZ,
            onRotationXChanged = ::setRotationX,
            onRotationYChanged = ::setRotationY,
            onConnectorModeChanged = ::toggleConnectorMode,
            onRadiusChanged = ::setConnectorRadius,
            onHeightChanged = ::setConnectorHeight,
            onTypeChanged = ::setConnectorType,
            onUndoConnector = ::undoConnector,
            onClearConnectors = ::clearConnectors,
            onApplyCut = ::applyCut,
        )
    }

    internal override fun workspacePanelSize(): WorkspacePanelSize = WorkspacePanelSize(
        portrait = WorkspacePanelDimension.ClampedFraction(1f / 3f, minDp = 180f, maxDp = 280f),
        landscape = WorkspacePanelDimension.ClampedFraction(0.42f, maxDp = 360f),
    )

    private companion object {
        const val CONNECTOR_COUNT_REFRESH_MS = 300L
    }
}

@Composable
private fun TransformCutEditorContent(
    zValue: Float,
    zMin: Float,
    zMax: Float,
    rotationX: Float,
    rotationY: Float,
    connectorRadius: Float,
    connectorHeight: Float,
    connectorType: Int,
    connectorCount: Int,
    connectorMode: Boolean,
    canCut: Boolean,
    onZChanged: (Float) -> Unit,
    onRotationXChanged: (Float) -> Unit,
    onRotationYChanged: (Float) -> Unit,
    onConnectorModeChanged: () -> Unit,
    onRadiusChanged: (Float) -> Unit,
    onHeightChanged: (Float) -> Unit,
    onTypeChanged: (Float) -> Unit,
    onUndoConnector: () -> Unit,
    onClearConnectors: () -> Unit,
    onApplyCut: () -> Unit,
) {
    val connectorTypeString = stringResource(
        when (connectorType) {
            1 -> R.string.CutConnectorDowel
            2 -> R.string.CutConnectorSnap
            else -> R.string.CutConnectorPlug
        },
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        CutValueSlider(
            label = stringResource(R.string.CutPositionZ, zValue.toDouble()),
            value = zValue,
            valueRange = zMin..zMax,
            onValueChange = onZChanged,
        )
        CutValueSlider(
            label = stringResource(R.string.CutRotationX, rotationX.roundToInt()),
            value = rotationX,
            valueRange = -180f..180f,
            onValueChange = onRotationXChanged,
        )
        CutValueSlider(
            label = stringResource(R.string.CutRotationY, rotationY.roundToInt()),
            value = rotationY,
            valueRange = -180f..180f,
            onValueChange = onRotationYChanged,
        )

        Button(
            onClick = onConnectorModeChanged,
            enabled = canCut,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(top = 4.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(
                stringResource(if (connectorMode) R.string.CutAddingConnectors else R.string.CutAddConnector),
                fontSize = 15.sp,
            )
        }

        if (connectorMode) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = stringResource(R.string.CutConnectorCount, connectorCount),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    CutValueSlider(
                        label = stringResource(R.string.CutConnectorRadius, connectorRadius),
                        value = connectorRadius,
                        valueRange = 1f..30f,
                        onValueChange = onRadiusChanged,
                    )
                    CutValueSlider(
                        label = stringResource(R.string.CutConnectorHeight, connectorHeight),
                        value = connectorHeight,
                        valueRange = 1f..50f,
                        onValueChange = onHeightChanged,
                    )
                    CutValueSlider(
                        label = stringResource(R.string.CutConnectorType, connectorTypeString),
                        value = connectorType.toFloat(),
                        valueRange = 0f..2f,
                        steps = 1,
                        onValueChange = onTypeChanged,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = onUndoConnector,
                            enabled = connectorCount > 0,
                            modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text(stringResource(R.string.CutUndoConnector), maxLines = 2)
                        }
                        OutlinedButton(
                            onClick = onClearConnectors,
                            enabled = connectorCount > 0,
                            modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text(stringResource(R.string.CutClearConnectors), maxLines = 2)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.padding(top = 2.dp))
        Button(
            onClick = onApplyCut,
            enabled = canCut,
            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).padding(top = 2.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(
                stringResource(R.string.CutApply),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun CutValueSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onValueChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelLarge,
            fontSize = 14.sp,
        )
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)
                .semantics { contentDescription = label },
        )
    }
}
