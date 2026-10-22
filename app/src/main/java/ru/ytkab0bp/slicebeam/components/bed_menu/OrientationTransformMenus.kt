package ru.ytkab0bp.slicebeam.components.bed_menu

import android.content.Context
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.ytkab0bp.eventbus.EventHandler
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.components.WorkspaceComposeToolRoute
import ru.ytkab0bp.slicebeam.events.LongClickTranslationEvent
import ru.ytkab0bp.slicebeam.events.SelectedObjectChangedEvent
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.slic3r.Model
import ru.ytkab0bp.slicebeam.theme.ThemesRepo
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import ru.ytkab0bp.slicebeam.ui.ThemeAwareComposeView
import ru.ytkab0bp.slicebeam.utils.Vec3d
import ru.ytkab0bp.slicebeam.utils.ViewUtils
import ru.ytkab0bp.slicebeam.ui.workspace.WorkspacePanelDimension
import ru.ytkab0bp.slicebeam.ui.workspace.WorkspacePanelSize

internal enum class OrientationAxis { X, Y, Z }

internal data class OrientationAxisValues(val x: Int, val y: Int, val z: Int)

internal data class OrientationVector3(val x: Double, val y: Double, val z: Double)

internal object OrientationTransformPolicy {
    fun setAxis(values: OrientationAxisValues, axis: OrientationAxis, value: Int): OrientationAxisValues = when (axis) {
        OrientationAxis.X -> values.copy(x = value)
        OrientationAxis.Y -> values.copy(y = value)
        OrientationAxis.Z -> values.copy(z = value)
    }

    fun positionFromRuler(
        current: OrientationVector3,
        values: OrientationAxisValues,
        axis: OrientationAxis,
    ) = when (axis) {
        OrientationAxis.X -> current.copy(x = values.x.toDouble())
        OrientationAxis.Y -> current.copy(y = values.y.toDouble())
        OrientationAxis.Z -> current.copy(z = values.z.toDouble())
    }

    fun normalizedDegrees(value: Double): Double = if (value.isFinite()) value % 360.0 else 0.0

    fun normalizedDegrees(values: OrientationVector3) = OrientationVector3(
        normalizedDegrees(values.x),
        normalizedDegrees(values.y),
        normalizedDegrees(values.z),
    )
}

/** Shared Compose shell and decimal-entry dialog for position and rotation editors. */
abstract class OrientationTransformMenuBase(fragment: BedFragment) : WorkspaceComposeToolRoute() {
    private val manualDialogVisible = mutableStateOf(false)
    private val manualDialogTitle = mutableIntStateOf(R.string.MenuOrientationPositionX)
    private val manualValue = mutableStateOf("")
    private var manualAction: ((Double) -> Unit)? = null

    init {
        this.fragment = fragment
    }

    protected val bed: BedFragment
        get() = checkNotNull(fragment)

    protected fun showManualEditor(title: Int, current: Double, onApply: (Double) -> Unit) {
        if (!current.isFinite()) return
        manualDialogTitle.intValue = title
        manualValue.value = java.lang.String.format(java.util.Locale.ROOT, "%.2f", current)
        manualAction = onApply
        manualDialogVisible.value = true
    }

    @Composable
    protected fun ManualEditorContent() {
        if (!manualDialogVisible.value) return
        val parsed = manualValue.value.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }
        AlertDialog(
            onDismissRequest = { closeManualEditor() },
            title = { Text(stringResource(manualDialogTitle.intValue)) },
            text = {
                OutlinedTextField(
                    value = manualValue.value,
                    onValueChange = { manualValue.value = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.OrientationManualValue)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val value = parsed ?: return@TextButton
                        manualAction?.invoke(value)
                        closeManualEditor()
                    },
                    enabled = parsed != null,
                ) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { closeManualEditor() }) { Text(stringResource(android.R.string.cancel)) }
            },
        )
    }

    private fun closeManualEditor() {
        manualDialogVisible.value = false
        manualAction = null
    }

    protected fun themedComposeView(ctx: Context, content: @Composable (ThemeAwareComposeView) -> Unit): View {
        val themedView = ThemeAwareComposeView(ctx)
        themedView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
        themedView.setContent {
            BeamComposeTheme(themeRevision = themedView.themeRevision) {
                content(themedView)
            }
        }
        return themedView
    }
}

/** Compose X/Y/Z position editor. Translation previews remain reversible until the ruler settles. */
open class OrientationPositionMenu(fragment: BedFragment) : OrientationTransformMenuBase(fragment) {
    private val rulerValues = mutableStateOf(OrientationAxisValues(0, 0, 0))
    private val position = mutableStateOf(OrientationVector3(0.0, 0.0, 0.0))
    private val hasSelection = mutableStateOf(false)
    private var selectedObjectForPreview = -1
    private var selectedObjectsForPreview = intArrayOf()
    private var eventBusRegistered = false

    fun translateVisual(x: Double?, y: Double?, z: Double?) {
        val glView = bed.glView ?: return
        val index = glView.renderer.selectedObject
        if (index < 0) return
        selectedObjectForPreview = index
        val current = position.value
        val target = OrientationVector3(x ?: current.x, y ?: current.y, z ?: current.z)
        position.value = target
        previewTranslation(index, target)
    }

    private fun previewFromRuler(axis: OrientationAxis, values: OrientationAxisValues) {
        val index = bed.glView?.renderer?.selectedObject ?: return
        if (index < 0) return
        val target = OrientationTransformPolicy.positionFromRuler(position.value, values, axis)
        position.value = target
        selectedObjectForPreview = index
        previewTranslation(index, target)
    }

    private fun previewTranslation(index: Int, target: OrientationVector3) {
        val glView = bed.glView ?: return
        val renderer = glView.renderer
        val model = renderer.model ?: return
        val selectedIndices = renderer.selectedObjectIndices().filter { it in 0 until model.objectsCount }.toIntArray()
        if (index !in selectedIndices) return
        runCatching {
            val current = Vec3d()
            model.getTranslation(index, current)
            renderer.setSelectionTranslation(
                target.x - current.x,
                target.y - current.y,
                target.z - current.z,
            )
            selectedObjectForPreview = index
            selectedObjectsForPreview = selectedIndices
            glView.requestRender()
        }
    }

    private fun commitTranslation() {
        val glView = bed.glView ?: return
        val renderer = glView.renderer
        val index = renderer.selectedObject
        val modelAtCommit = renderer.model ?: return
        val selectedIndices = renderer.selectedObjectIndices()
            .filter { it in 0 until modelAtCommit.objectsCount }
            .toIntArray()
        if (index < 0 || index !in selectedIndices ||
            (selectedObjectForPreview >= 0 && index != selectedObjectForPreview) ||
            (selectedObjectsForPreview.isNotEmpty() && !selectedObjectsForPreview.contentEquals(selectedIndices))
        ) {
            stopPreview()
            return
        }
        val target = position.value
        selectedObjectForPreview = -1
        selectedObjectsForPreview = intArrayOf()
        glView.queueEvent {
            val renderer = glView.renderer
            if (renderer.selectedObject != index || !renderer.selectedObjectIndices().contentEquals(selectedIndices)) {
                renderer.setSelectionTranslation(0.0, 0.0, 0.0)
                glView.requestRender()
                return@queueEvent
            }
            val model: Model = renderer.model ?: return@queueEvent
            runCatching {
                val current = Vec3d()
                model.getTranslation(index, current)
                val dx = target.x - current.x
                val dy = target.y - current.y
                val dz = target.z - current.z
                if (kotlin.math.abs(dx) < 1e-9 &&
                    kotlin.math.abs(dy) < 1e-9 &&
                    kotlin.math.abs(dz) < 1e-9
                ) {
                    renderer.setSelectionTranslation(0.0, 0.0, 0.0)
                    glView.requestRender()
                    return@runCatching
                }
                for (selectedIndex in selectedIndices) {
                    model.translate(selectedIndex, dx, dy, dz)
                    renderer.invalidateGlModel(selectedIndex)
                }
                renderer.setSelectionTranslation(0.0, 0.0, 0.0)
                glView.requestRender()
                ViewUtils.postOnMainThread { bed.updateModel() }
            }
        }
        glView.requestRender()
    }

    private fun changeRuler(axis: OrientationAxis, value: Int) {
        val next = OrientationTransformPolicy.setAxis(rulerValues.value, axis, value)
        rulerValues.value = next
        previewFromRuler(axis, next)
    }

    private fun editAxis(axis: OrientationAxis, title: Int) {
        if (!hasSelection.value) return
        val current = when (axis) {
            OrientationAxis.X -> position.value.x
            OrientationAxis.Y -> position.value.y
            OrientationAxis.Z -> position.value.z
        }
        showManualEditor(title, current) { value ->
            val currentPosition = position.value
            val target = when (axis) {
                OrientationAxis.X -> currentPosition.copy(x = value)
                OrientationAxis.Y -> currentPosition.copy(y = value)
                OrientationAxis.Z -> currentPosition.copy(z = value)
            }
            position.value = target
            val next = OrientationTransformPolicy.setAxis(rulerValues.value, axis, value.toInt())
            rulerValues.value = next
            bed.glView?.renderer?.selectedObject?.takeIf { it >= 0 }?.let { previewTranslation(it, target) }
            commitTranslation()
        }
    }

    @Composable
    override fun ComposeContent(portrait: Boolean) {
        val values = rulerValues.value
        val point = position.value
        val available = hasSelection.value
        PositionEditorContent(
            xTitle = stringResource(R.string.MenuOrientationPositionX),
            yTitle = stringResource(R.string.MenuOrientationPositionY),
            zTitle = stringResource(R.string.MenuOrientationPositionZ),
            xValue = stringResource(R.string.MenuOrientationPositionXValue, point.x),
            yValue = stringResource(R.string.MenuOrientationPositionYValue, point.y),
            zValue = stringResource(R.string.MenuOrientationPositionZValue, point.z),
            x = values.x,
            y = values.y,
            z = values.z,
            xColor = Color(ThemesRepo.getColor(R.attr.xTrackColor)),
            yColor = Color(ThemesRepo.getColor(R.attr.yTrackColor)),
            zColor = Color(ThemesRepo.getColor(R.attr.zTrackColor)),
            enabled = available,
            backTitle = stringResource(R.string.MenuOrientationPositionBack),
            onEditX = { editAxis(OrientationAxis.X, R.string.MenuOrientationPositionX) },
            onEditY = { editAxis(OrientationAxis.Y, R.string.MenuOrientationPositionY) },
            onEditZ = { editAxis(OrientationAxis.Z, R.string.MenuOrientationPositionZ) },
            onXChanged = { changeRuler(OrientationAxis.X, it) },
            onYChanged = { changeRuler(OrientationAxis.Y, it) },
            onZChanged = { changeRuler(OrientationAxis.Z, it) },
            onCommit = ::commitTranslation,
            onBack = ::dismiss,
        )
        ManualEditorContent()
    }

    internal override fun workspacePanelSize(): WorkspacePanelSize = WorkspacePanelSize(
        portrait = WorkspacePanelDimension.Fixed(316f),
        landscape = WorkspacePanelDimension.Fraction(0.5f),
    )

    override fun onCreate() {
        super.onCreate()
        SliceBeam.EVENT_BUS.registerListener(this)
        eventBusRegistered = true
        loadSelectionValues()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (eventBusRegistered) SliceBeam.EVENT_BUS.unregisterListener(this)
        eventBusRegistered = false
        stopPreview()
    }

    private fun loadSelectionValues() {
        val renderer = bed.glView?.renderer ?: return
        val index = renderer.selectedObject
        val model = renderer.model
        if (index < 0 || model == null) {
            hasSelection.value = false
            selectedObjectForPreview = -1
            return
        }
        runCatching {
            renderer.setSelectionTranslation(0.0, 0.0, 0.0)
            val translated = Vec3d()
            model.getTranslation(index, translated)
            val target = OrientationVector3(translated.x, translated.y, translated.z)
            position.value = target
            rulerValues.value = OrientationAxisValues(
                translated.x.toInt(), translated.y.toInt(), translated.z.toInt(),
            )
            selectedObjectForPreview = -1
            hasSelection.value = true
        }.onFailure {
            hasSelection.value = false
            selectedObjectForPreview = -1
        }
    }

    private fun stopPreview() {
        val glView = bed.glView ?: return
        if (selectedObjectForPreview >= 0) {
            glView.renderer.setSelectionTranslation(0.0, 0.0, 0.0)
            glView.requestRender()
        }
        selectedObjectForPreview = -1
        selectedObjectsForPreview = intArrayOf()
    }

    @EventHandler(runOnMainThread = true)
    fun onLongClickTranslation(event: LongClickTranslationEvent) {
        if (!event.visual) {
            stopPreview()
            loadSelectionValues()
            return
        }
        val renderer = bed.glView?.renderer ?: return
        val index = renderer.selectedObject
        if (index < 0) return
        val model = renderer.model ?: return
        runCatching {
            val current = Vec3d()
            model.getTranslation(index, current)
            position.value = OrientationVector3(current.x + event.x, current.y + event.y, current.z)
            rulerValues.value = OrientationAxisValues(
                position.value.x.toInt(),
                position.value.y.toInt(),
                position.value.z.toInt(),
            )
            selectedObjectForPreview = index
            selectedObjectsForPreview = renderer.selectedObjectIndices()
            hasSelection.value = true
        }
    }

    @EventHandler(runOnMainThread = true)
    fun onSelectedObjectChanged(event: SelectedObjectChangedEvent) {
        stopPreview()
        if (bed.glView?.renderer?.selectedObject == -1) dismiss() else loadSelectionValues()
    }
}

/** Compose X/Y/Z rotation editor with a persistent apply action. */
open class OrientationRotationMenu(fragment: BedFragment) : OrientationTransformMenuBase(fragment) {
    private val rulerValues = mutableStateOf(OrientationAxisValues(0, 0, 0))
    private val rotation = mutableStateOf(OrientationVector3(0.0, 0.0, 0.0))
    private val hasSelection = mutableStateOf(false)
    private var selectedObjectsForPreview = intArrayOf()
    private var eventBusRegistered = false

    private fun setRotationPreview(values: OrientationVector3) {
        val glView = bed.glView ?: return
        if (glView.renderer.selectedObject < 0 || selectedObjectsForPreview.isEmpty()) return
        rotation.value = values
        val normalized = OrientationTransformPolicy.normalizedDegrees(values)
        val expectedSelection = selectedObjectsForPreview.copyOf()
        glView.queueEvent {
            val renderer = glView.renderer
            if (!renderer.selectedObjectIndices().contentEquals(expectedSelection)) return@queueEvent
            renderer.prepareSelectionTransformPreview()
            renderer.setSelectionRotation(normalized.x, normalized.y, normalized.z)
            glView.requestRender()
        }
    }

    private fun changeRuler(axis: OrientationAxis, value: Int) {
        rulerValues.value = OrientationTransformPolicy.setAxis(rulerValues.value, axis, value)
        val axes = rulerValues.value
        setRotationPreview(OrientationVector3(axes.x.toDouble(), axes.y.toDouble(), axes.z.toDouble()))
    }

    private fun editAxis(axis: OrientationAxis, title: Int) {
        if (!hasSelection.value) return
        val current = when (axis) {
            OrientationAxis.X -> rotation.value.x
            OrientationAxis.Y -> rotation.value.y
            OrientationAxis.Z -> rotation.value.z
        }
        showManualEditor(title, current) { value ->
            val target = when (axis) {
                OrientationAxis.X -> rotation.value.copy(x = value)
                OrientationAxis.Y -> rotation.value.copy(y = value)
                OrientationAxis.Z -> rotation.value.copy(z = value)
            }
            rulerValues.value = OrientationTransformPolicy.setAxis(rulerValues.value, axis, value.toInt())
            setRotationPreview(target)
        }
    }

    private fun applyRotation() {
        val glView = bed.glView ?: return
        val renderer = glView.renderer
        val index = renderer.selectedObject
        val expectedSelection = selectedObjectsForPreview.copyOf()
        if (index < 0 || renderer.model == null || expectedSelection.isEmpty()) return
        val target = OrientationTransformPolicy.normalizedDegrees(rotation.value)
        glView.queueEvent {
            val activeRenderer = glView.renderer
            if (activeRenderer.selectedObject != index ||
                !activeRenderer.selectedObjectIndices().contentEquals(expectedSelection)) return@queueEvent
            val model: Model = activeRenderer.model ?: return@queueEvent
            runCatching {
                if (activeRenderer.rotateSelectedObjects(
                    Math.toRadians(target.x),
                    Math.toRadians(target.y),
                    Math.toRadians(target.z),
                )) {
                    glView.requestRender()
                    ViewUtils.postOnMainThread { bed.updateModel() }
                }
            }
        }
        rotation.value = OrientationVector3(0.0, 0.0, 0.0)
        rulerValues.value = OrientationAxisValues(0, 0, 0)
        glView.requestRender()
    }

    @Composable
    override fun ComposeContent(portrait: Boolean) {
        val values = rulerValues.value
        val degrees = rotation.value
        val available = hasSelection.value
        RotationEditorContent(
            xTitle = stringResource(R.string.MenuOrientationPositionX),
            yTitle = stringResource(R.string.MenuOrientationPositionY),
            zTitle = stringResource(R.string.MenuOrientationPositionZ),
            xValue = stringResource(R.string.MenuOrientationRotationXValue, degrees.x),
            yValue = stringResource(R.string.MenuOrientationRotationYValue, degrees.y),
            zValue = stringResource(R.string.MenuOrientationRotationZValue, degrees.z),
            x = values.x,
            y = values.y,
            z = values.z,
            xColor = Color(ThemesRepo.getColor(R.attr.xTrackColor)),
            yColor = Color(ThemesRepo.getColor(R.attr.yTrackColor)),
            zColor = Color(ThemesRepo.getColor(R.attr.zTrackColor)),
            enabled = available,
            backTitle = stringResource(R.string.MenuOrientationPositionBack),
            applyTitle = stringResource(R.string.MenuOrientationRotationApply),
            onEditX = { editAxis(OrientationAxis.X, R.string.MenuOrientationRotationX) },
            onEditY = { editAxis(OrientationAxis.Y, R.string.MenuOrientationRotationY) },
            onEditZ = { editAxis(OrientationAxis.Z, R.string.MenuOrientationRotationZ) },
            onXChanged = { changeRuler(OrientationAxis.X, it) },
            onYChanged = { changeRuler(OrientationAxis.Y, it) },
            onZChanged = { changeRuler(OrientationAxis.Z, it) },
            onApply = ::applyRotation,
            onBack = ::dismiss,
        )
        ManualEditorContent()
    }

    internal override fun workspacePanelSize(): WorkspacePanelSize = WorkspacePanelSize(
        portrait = WorkspacePanelDimension.Fixed(436f),
        landscape = WorkspacePanelDimension.Fraction(0.5f),
    )

    override fun onCreate() {
        super.onCreate()
        SliceBeam.EVENT_BUS.registerListener(this)
        eventBusRegistered = true
        loadSelectionValues()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (eventBusRegistered) SliceBeam.EVENT_BUS.unregisterListener(this)
        eventBusRegistered = false
        stopPreview()
    }

    private fun loadSelectionValues() {
        val renderer = bed.glView?.renderer ?: return
        if (renderer.selectedObject < 0 || renderer.model == null) {
            hasSelection.value = false
            selectedObjectsForPreview = intArrayOf()
            return
        }
        selectedObjectsForPreview = renderer.selectedObjectIndices()
        glViewQueuePrepare(renderer)
        rotation.value = OrientationVector3(0.0, 0.0, 0.0)
        rulerValues.value = OrientationAxisValues(0, 0, 0)
        hasSelection.value = true
    }

    private fun stopPreview() {
        val glView = bed.glView ?: return
        glView.queueEvent {
            glView.renderer.setSelectionRotation(0.0, 0.0, 0.0)
            glView.requestRender()
        }
    }

    private fun glViewQueuePrepare(renderer: ru.ytkab0bp.slicebeam.render.GLRenderer) {
        val glView = bed.glView ?: return
        glView.queueEvent {
            if (glView.renderer === renderer) {
                renderer.prepareSelectionTransformPreview()
                renderer.setSelectionRotation(0.0, 0.0, 0.0)
                glView.requestRender()
            }
        }
    }

    override fun dismiss() {
        stopPreview()
        super.dismiss()
    }

    @EventHandler(runOnMainThread = true)
    fun onSelectedObjectChanged(event: SelectedObjectChangedEvent) {
        stopPreview()
        if (bed.glView?.renderer?.selectedObject == -1) dismiss() else loadSelectionValues()
    }
}

@Composable
private fun PositionEditorContent(
    xTitle: String,
    yTitle: String,
    zTitle: String,
    xValue: String,
    yValue: String,
    zValue: String,
    x: Int,
    y: Int,
    z: Int,
    xColor: Color,
    yColor: Color,
    zColor: Color,
    enabled: Boolean,
    backTitle: String,
    onEditX: () -> Unit,
    onEditY: () -> Unit,
    onEditZ: () -> Unit,
    onXChanged: (Int) -> Unit,
    onYChanged: (Int) -> Unit,
    onZChanged: (Int) -> Unit,
    onCommit: () -> Unit,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            OrientationAxisEditor(
                key = "position-x", title = xTitle, valueText = xValue, value = x, color = xColor,
                enabled = enabled, onEdit = onEditX, onValueChanged = onXChanged, onCommit = onCommit,
            )
            OrientationAxisEditor(
                key = "position-y", title = yTitle, valueText = yValue, value = y, color = yColor,
                enabled = enabled, onEdit = onEditY, onValueChanged = onYChanged, onCommit = onCommit,
            )
            OrientationAxisEditor(
                key = "position-z", title = zTitle, valueText = zValue, value = z, color = zColor,
                enabled = enabled, onEdit = onEditZ, onValueChanged = onZChanged, onCommit = onCommit,
            )
        }
        OrientationActions(backTitle = backTitle, onBack = onBack)
    }
}

@Composable
private fun RotationEditorContent(
    xTitle: String,
    yTitle: String,
    zTitle: String,
    xValue: String,
    yValue: String,
    zValue: String,
    x: Int,
    y: Int,
    z: Int,
    xColor: Color,
    yColor: Color,
    zColor: Color,
    enabled: Boolean,
    backTitle: String,
    applyTitle: String,
    onEditX: () -> Unit,
    onEditY: () -> Unit,
    onEditZ: () -> Unit,
    onXChanged: (Int) -> Unit,
    onYChanged: (Int) -> Unit,
    onZChanged: (Int) -> Unit,
    onApply: () -> Unit,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            OrientationAxisEditor(
                key = "rotation-x", title = xTitle, valueText = xValue, value = x, color = xColor,
                enabled = enabled, onEdit = onEditX, onValueChanged = onXChanged, onCommit = {},
            )
            OrientationAxisEditor(
                key = "rotation-y", title = yTitle, valueText = yValue, value = y, color = yColor,
                enabled = enabled, onEdit = onEditY, onValueChanged = onYChanged, onCommit = {},
            )
            OrientationAxisEditor(
                key = "rotation-z", title = zTitle, valueText = zValue, value = z, color = zColor,
                enabled = enabled, onEdit = onEditZ, onValueChanged = onZChanged, onCommit = {},
            )
        }
        OrientationActions(backTitle, onBack, applyTitle, onApply)
    }
}

@Composable
private fun OrientationAxisEditor(
    key: String,
    title: String,
    valueText: String,
    value: Int,
    color: Color,
    enabled: Boolean,
    onEdit: () -> Unit,
    onValueChanged: (Int) -> Unit,
    onCommit: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (enabled) 0.72f else 0.35f))
                .clickable(enabled = enabled, onClick = onEdit)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, color = color, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.width(10.dp))
            Text(
                valueText,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text("✎", color = MaterialTheme.colorScheme.primary, fontSize = 15.sp)
        }
        SteppedRuler(
            key = key,
            title = title,
            stateDescription = valueText,
            value = value,
            color = color,
            enabled = enabled,
            min = Int.MIN_VALUE,
            max = Int.MAX_VALUE,
            onValueChanged = onValueChanged,
            onCommit = onCommit,
        )
    }
}

@Composable
private fun OrientationActions(
    backTitle: String,
    onBack: () -> Unit,
    applyTitle: String? = null,
    onApply: (() -> Unit)? = null,
) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.weight(if (applyTitle == null) 1f else 0.8f).heightIn(min = 48.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(backTitle, maxLines = 1, fontSize = 14.sp)
        }
        if (applyTitle != null && onApply != null) {
            Button(
                onClick = onApply,
                modifier = Modifier.weight(1.2f).heightIn(min = 48.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(applyTitle, maxLines = 1, fontSize = 14.sp)
            }
        }
    }
}
