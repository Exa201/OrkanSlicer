package ru.ytkab0bp.slicebeam.components

import android.os.Process
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.components.bed_menu.SliceChoiceField
import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.smartslice.GeometryAnalysis
import ru.ytkab0bp.slicebeam.smartslice.SmartSliceEngine
import ru.ytkab0bp.slicebeam.smartslice.SmartSliceIntent
import ru.ytkab0bp.slicebeam.smartslice.SmartSliceResult
import ru.ytkab0bp.slicebeam.utils.ViewUtils
import java.util.HashMap
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean

/** Smart slicing setup, analysis progress, and result actions. */
class SmartSliceDialog private constructor() {
    companion object {
        private val ANALYSIS_EXECUTOR = Executors.newSingleThreadExecutor { task ->
            Thread(task, "OrkanSlicer-SmartSlice").apply { priority = Thread.NORM_PRIORITY - 1 }
        }

        /** Kept static for the existing Java menu and activity call sites. */
        @JvmStatic
        fun show(fragment: BedFragment) {
            val context = fragment.getContext() ?: return
            val model = fragment.currentModel
            if (model == null || model.objectsCount == 0) {
                Toast.makeText(context, R.string.SmartSliceNoModel, Toast.LENGTH_SHORT).show()
                return
            }

            val printerNames = SliceBeam.requireConfig().printerConfigs.mapNotNull { it.title }
            val activePrinter = SliceBeam.requireConfig().presets?.get("printer")
            val purposeOptions = context.resources.getStringArray(R.array.SmartSlicePurposes).toList()
            val priorityOptions = context.resources.getStringArray(R.array.SmartSlicePriorities).toList()
            val options = SmartSliceFormOptions(
                printerNames = printerNames,
                purposeOptions = purposeOptions,
                priorityOptions = priorityOptions,
                selectedPrinter = printerNames.indexOf(activePrinter).coerceAtLeast(0),
            )
            fragment.showWorkspaceComposeDialog { dismissForm ->
                SmartSliceFormDialog(options, onDismiss = dismissForm) { submission ->
                    printerNames.getOrNull(submission.selectedPrinter)?.let { printer ->
                        if (!SliceBeam.applyProfileSelection(printer = printer)) {
                            Toast.makeText(context, R.string.ConfigSaveFailed, Toast.LENGTH_LONG).show()
                            return@SmartSliceFormDialog
                        }
                        fragment.refreshProfileStatus()
                    }
                    val intent = SmartSliceIntent(
                        SmartSliceIntent.Purpose.values().getOrElse(submission.selectedPurpose) { SmartSliceIntent.Purpose.DECORATION },
                        SmartSliceIntent.Priority.values().getOrElse(submission.selectedPriority) { SmartSliceIntent.Priority.BALANCED },
                        submission.values.wallMm,
                        submission.values.layerMm,
                    )
                    val configSnapshot = ConfigObject(SliceBeam.buildCurrentConfigObject())
                    val before = HashMap(configSnapshot.values)
                    val cancelled = AtomicBoolean()
                    dismissForm()
                    val dismissProgress = fragment.showWorkspaceComposeDialog(
                        onExternalDismiss = { cancelled.set(true) },
                    ) { dismiss ->
                        SmartSliceProgressDialog(onCancel = {
                            cancelled.set(true)
                            dismiss()
                        })
                    }
                    analyzeModel(fragment, context, intent, configSnapshot, before, cancelled, dismissProgress)
                }
            }
        }

        private fun analyzeModel(
            fragment: BedFragment,
            context: android.content.Context,
            intent: SmartSliceIntent,
            configSnapshot: ConfigObject,
            before: Map<String, String>,
            cancelled: AtomicBoolean,
            dismissProgress: () -> Unit,
        ) {
            fragment.glView?.queueEvent {
                if (cancelled.get()) return@queueEvent
                val model = fragment.currentModel
                if (model == null) {
                    ViewUtils.postOnMainThread {
                        dismissProgress()
                        Toast.makeText(context, R.string.SmartSliceNoModel, Toast.LENGTH_SHORT).show()
                    }
                    return@queueEvent
                }
                val modelKey = model.key
                val geometry = try {
                    GeometryAnalysis.analyze(model)
                } catch (_: RuntimeException) {
                    ViewUtils.postOnMainThread {
                        dismissProgress()
                        Toast.makeText(context, R.string.SmartSliceFailed, Toast.LENGTH_LONG).show()
                    }
                    return@queueEvent
                }
                if (cancelled.get()) return@queueEvent
                try {
                    ANALYSIS_EXECUTOR.execute {
                        if (cancelled.get()) return@execute
                        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
                        val result: SmartSliceResult = try {
                            SmartSliceEngine.analyze(geometry, intent, configSnapshot)
                        } catch (_: RuntimeException) {
                            ViewUtils.postOnMainThread {
                                dismissProgress()
                                if (!cancelled.get()) Toast.makeText(context, R.string.SmartSliceFailed, Toast.LENGTH_LONG).show()
                            }
                            return@execute
                        }
                        ViewUtils.postOnMainThread {
                            dismissProgress()
                            if (cancelled.get()) return@postOnMainThread
                            val activeModel = fragment.currentModel
                            if (activeModel == null || modelKey != activeModel.key ||
                                geometry.modelRevision != activeModel.geometryRevision
                            ) {
                                Toast.makeText(context, R.string.SmartSliceModelChanged, Toast.LENGTH_LONG).show()
                                return@postOnMainThread
                            }
                            showResult(fragment, result, before)
                        }
                    }
                } catch (_: RejectedExecutionException) {
                    ViewUtils.postOnMainThread {
                        dismissProgress()
                        Toast.makeText(context, R.string.SmartSliceBusy, Toast.LENGTH_SHORT).show()
                    }
                }
            } ?: run {
                dismissProgress()
                Toast.makeText(context, R.string.SmartSliceFailed, Toast.LENGTH_SHORT).show()
            }
        }

        private fun showResult(fragment: BedFragment, result: SmartSliceResult, before: Map<String, String>) {
            val context = fragment.getContext() ?: return
            val summary = context.getString(
                R.string.SmartSliceResultWithChanges,
                result.summary(),
                result.changeSummary(before),
            )
            fragment.showWorkspaceComposeDialog { dismiss ->
                SmartSliceResultDialog(
                    summary = summary,
                    onDismiss = dismiss,
                    onApplyAndSlice = {
                        SmartSliceEngine.apply(result)
                        fragment.refreshProfileStatus()
                        dismiss()
                        fragment.startSlicing()
                    },
                    onApplyEdit = {
                        SmartSliceEngine.apply(result)
                        fragment.refreshProfileStatus()
                        Toast.makeText(context, R.string.SmartSliceApplied, Toast.LENGTH_LONG).show()
                        dismiss()
                    },
                )
            }
        }
    }
}

internal data class SmartSliceFormOptions(
    val printerNames: List<String>,
    val purposeOptions: List<String>,
    val priorityOptions: List<String>,
    val selectedPrinter: Int,
)

internal data class SmartSliceFormSubmission(
    val selectedPrinter: Int,
    val selectedPurpose: Int,
    val selectedPriority: Int,
    val values: SmartSliceNumericValues,
)

@Composable
internal fun SmartSliceFormDialog(
    options: SmartSliceFormOptions,
    onDismiss: () -> Unit,
    onAnalyze: (SmartSliceFormSubmission) -> Unit,
) {
    var selectedPrinter by remember(options) { mutableIntStateOf(options.selectedPrinter) }
    var selectedPurpose by remember(options) { mutableIntStateOf(0) }
    var selectedPriority by remember(options) { mutableIntStateOf(1) }
    var wall by remember(options) { mutableStateOf("") }
    var layer by remember(options) { mutableStateOf("") }
    var invalidWall by remember(options) { mutableStateOf(false) }
    var invalidLayer by remember(options) { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.SmartSliceTitle)) },
        text = {
            SmartSliceFormContent(
                printerNames = options.printerNames,
                purposeOptions = options.purposeOptions,
                priorityOptions = options.priorityOptions,
                selectedPrinter = selectedPrinter,
                selectedPurpose = selectedPurpose,
                selectedPriority = selectedPriority,
                onPrinterSelected = { selectedPrinter = it },
                onPurposeSelected = { selectedPurpose = it },
                onPrioritySelected = { selectedPriority = it },
                wall = wall,
                onWallChanged = { wall = it; invalidWall = false },
                layer = layer,
                onLayerChanged = { layer = it; invalidLayer = false },
                invalidWall = invalidWall,
                invalidLayer = invalidLayer,
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val resolution = SmartSliceInputValues.resolve(wall, layer)
                    invalidWall = resolution.invalidWall
                    invalidLayer = resolution.invalidLayer
                    resolution.values?.let { values ->
                        onAnalyze(SmartSliceFormSubmission(selectedPrinter, selectedPurpose, selectedPriority, values))
                    }
                },
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text(stringResource(R.string.SmartSliceAnalyze)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

@Composable
internal fun SmartSliceProgressDialog(onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.SmartSliceTitle)) },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                CircularProgressIndicator()
                Text(stringResource(R.string.SmartSliceAnalyzing), color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onCancel, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

@Composable
internal fun SmartSliceResultDialog(
    summary: String,
    onDismiss: () -> Unit,
    onApplyAndSlice: () -> Unit,
    onApplyEdit: () -> Unit,
) {
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.72f
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.SmartSliceResult)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = maxHeight).verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 12.dp),
            ) {
                Text(summary, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, lineHeight = 22.sp)
            }
        },
        confirmButton = {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Bottom),
            ) {
                TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(android.R.string.cancel))
                }
                TextButton(onClick = onApplyEdit, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.SmartSliceApplyEdit))
                }
                TextButton(onClick = onApplyAndSlice, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.SmartSliceApplyAndSlice))
                }
            }
        },
        dismissButton = {},
    )
}

@Composable
private fun SmartSliceFormContent(
    printerNames: List<String>,
    purposeOptions: List<String>,
    priorityOptions: List<String>,
    selectedPrinter: Int,
    selectedPurpose: Int,
    selectedPriority: Int,
    onPrinterSelected: (Int) -> Unit,
    onPurposeSelected: (Int) -> Unit,
    onPrioritySelected: (Int) -> Unit,
    wall: String,
    onWallChanged: (String) -> Unit,
    layer: String,
    onLayerChanged: (String) -> Unit,
    invalidWall: Boolean,
    invalidLayer: Boolean,
) {
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.76f
    Column(
        modifier = Modifier.fillMaxWidth().heightIn(max = maxHeight).verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.SmartSliceIntro), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, lineHeight = 20.sp)
        if (printerNames.isNotEmpty()) SliceChoiceField(
            stringResource(R.string.SmartSlicePrinter), selectedPrinter, printerNames, onPrinterSelected,
        )
        SliceChoiceField(stringResource(R.string.SmartSlicePurpose), selectedPurpose, purposeOptions, onPurposeSelected)
        SliceChoiceField(stringResource(R.string.SmartSlicePriority), selectedPriority, priorityOptions, onPrioritySelected)
        OutlinedTextField(
            value = wall,
            onValueChange = onWallChanged,
            modifier = Modifier.fillMaxWidth().testTag("smart-slice-wall"),
            label = { Text(stringResource(R.string.SmartSliceWallOptional)) },
            supportingText = if (invalidWall) ({ Text(stringResource(R.string.SmartSliceInvalidDimension)) }) else null,
            isError = invalidWall,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        OutlinedTextField(
            value = layer,
            onValueChange = onLayerChanged,
            modifier = Modifier.fillMaxWidth().testTag("smart-slice-layer"),
            label = { Text(stringResource(R.string.SmartSliceLayerOptional)) },
            supportingText = if (invalidLayer) ({ Text(stringResource(R.string.SmartSliceInvalidDimension)) }) else null,
            isError = invalidLayer,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
    }
}

internal data class SmartSliceNumericValues(val wallMm: Double?, val layerMm: Double?)

internal data class SmartSliceInputResolution(
    val values: SmartSliceNumericValues?,
    val invalidWall: Boolean,
    val invalidLayer: Boolean,
)

/** Optional dimensions accept locale decimal commas and reject malformed/non-positive values. */
internal object SmartSliceInputValues {
    fun resolve(wall: String, layer: String): SmartSliceInputResolution {
        val parsedWall = optionalPositive(wall)
        val parsedLayer = optionalPositive(layer)
        val invalidWall = wall.isNotBlank() && parsedWall == null
        val invalidLayer = layer.isNotBlank() && parsedLayer == null
        val values = if (invalidWall || invalidLayer) null else SmartSliceNumericValues(parsedWall, parsedLayer)
        return SmartSliceInputResolution(values, invalidWall, invalidLayer)
    }

    private fun optionalPositive(input: String): Double? {
        if (input.isBlank()) return null
        return input.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it > 0.0 }
    }
}
