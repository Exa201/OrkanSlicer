package ru.ytkab0bp.slicebeam.components

import android.os.Process
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
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
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
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
            val selectedPrinter = mutableIntStateOf(printerNames.indexOf(activePrinter).coerceAtLeast(0))
            val selectedPurpose = mutableIntStateOf(0)
            val selectedPriority = mutableIntStateOf(1)
            val wall = mutableStateOf("")
            val layer = mutableStateOf("")
            val invalidWall = mutableStateOf(false)
            val invalidLayer = mutableStateOf(false)
            val purposeOptions = context.resources.getStringArray(R.array.SmartSlicePurposes).toList()
            val priorityOptions = context.resources.getStringArray(R.array.SmartSlicePriorities).toList()

            val form = ComposeView(context).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
                setContent {
                    BeamComposeTheme {
                        SmartSliceFormContent(
                            printerNames = printerNames,
                            purposeOptions = purposeOptions,
                            priorityOptions = priorityOptions,
                            selectedPrinter = selectedPrinter.intValue,
                            selectedPurpose = selectedPurpose.intValue,
                            selectedPriority = selectedPriority.intValue,
                            onPrinterSelected = { selectedPrinter.intValue = it },
                            onPurposeSelected = { selectedPurpose.intValue = it },
                            onPrioritySelected = { selectedPriority.intValue = it },
                            wall = wall.value,
                            onWallChanged = { wall.value = it; invalidWall.value = false },
                            layer = layer.value,
                            onLayerChanged = { layer.value = it; invalidLayer.value = false },
                            invalidWall = invalidWall.value,
                            invalidLayer = invalidLayer.value,
                        )
                    }
                }
            }
            val dialog = BeamAlertDialogBuilder(context)
                .setTitle(R.string.SmartSliceTitle)
                .setView(form)
                .setPositiveButton(R.string.SmartSliceAnalyze, null)
                .setNegativeButton(android.R.string.cancel, null)
                .show()
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val resolved = SmartSliceInputValues.resolve(wall.value, layer.value)
                invalidWall.value = resolved.invalidWall
                invalidLayer.value = resolved.invalidLayer
                val numeric = resolved.values ?: return@setOnClickListener
                printerNames.getOrNull(selectedPrinter.intValue)?.let { printer ->
                    if (!SliceBeam.applyProfileSelection(printer = printer)) {
                        Toast.makeText(context, R.string.ConfigSaveFailed, Toast.LENGTH_LONG).show()
                        return@setOnClickListener
                    }
                    fragment.refreshProfileStatus()
                }
                val intent = SmartSliceIntent(
                    SmartSliceIntent.Purpose.values().getOrElse(selectedPurpose.intValue) { SmartSliceIntent.Purpose.DECORATION },
                    SmartSliceIntent.Priority.values().getOrElse(selectedPriority.intValue) { SmartSliceIntent.Priority.BALANCED },
                    numeric.wallMm,
                    numeric.layerMm,
                )
                val configSnapshot = ConfigObject(SliceBeam.buildCurrentConfigObject())
                val before = HashMap(configSnapshot.values)
                val cancelled = AtomicBoolean()
                val progress = BeamAlertDialogBuilder(context)
                    .setTitle(R.string.SmartSliceTitle)
                    .setView(smartSliceProgressView(context))
                    .setNegativeButton(android.R.string.cancel) { _, _ -> cancelled.set(true) }
                    .setOnCancelListener { cancelled.set(true) }
                    .show()
                dialog.dismiss()
                analyzeModel(fragment, context, intent, configSnapshot, before, cancelled, progress)
            }
        }

        private fun analyzeModel(
            fragment: BedFragment,
            context: android.content.Context,
            intent: SmartSliceIntent,
            configSnapshot: ConfigObject,
            before: Map<String, String>,
            cancelled: AtomicBoolean,
            progress: androidx.appcompat.app.AlertDialog,
        ) {
            fragment.glView?.queueEvent {
                if (cancelled.get()) return@queueEvent
                val model = fragment.currentModel
                if (model == null) {
                    ViewUtils.postOnMainThread {
                        progress.dismiss()
                        Toast.makeText(context, R.string.SmartSliceNoModel, Toast.LENGTH_SHORT).show()
                    }
                    return@queueEvent
                }
                val modelKey = model.key
                val geometry = try {
                    GeometryAnalysis.analyze(model)
                } catch (_: RuntimeException) {
                    ViewUtils.postOnMainThread {
                        progress.dismiss()
                        Toast.makeText(context, R.string.SmartSliceFailed, Toast.LENGTH_LONG).show()
                    }
                    return@queueEvent
                }
                try {
                    ANALYSIS_EXECUTOR.execute {
                        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
                        val result: SmartSliceResult = try {
                            SmartSliceEngine.analyze(geometry, intent, configSnapshot)
                        } catch (_: RuntimeException) {
                            ViewUtils.postOnMainThread {
                                progress.dismiss()
                                if (!cancelled.get()) Toast.makeText(context, R.string.SmartSliceFailed, Toast.LENGTH_LONG).show()
                            }
                            return@execute
                        }
                        ViewUtils.postOnMainThread {
                            progress.dismiss()
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
                        progress.dismiss()
                        Toast.makeText(context, R.string.SmartSliceBusy, Toast.LENGTH_SHORT).show()
                    }
                }
            } ?: run {
                progress.dismiss()
                Toast.makeText(context, R.string.SmartSliceFailed, Toast.LENGTH_SHORT).show()
            }
        }

        private fun showResult(fragment: BedFragment, result: SmartSliceResult, before: Map<String, String>) {
            val context = fragment.getContext() ?: return
            val summary = ComposeView(context).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
                setContent {
                    BeamComposeTheme {
                        val maxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.72f
                        Column(
                            modifier = Modifier.fillMaxWidth().heightIn(max = maxHeight).verticalScroll(rememberScrollState())
                                .padding(horizontal = 22.dp, vertical = 12.dp),
                        ) {
                            Text(
                                text = context.getString(R.string.SmartSliceResultWithChanges, result.summary(), result.changeSummary(before)),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                            )
                        }
                    }
                }
            }
            BeamAlertDialogBuilder(context)
                .setTitle(R.string.SmartSliceResult)
                .setView(summary)
                .setPositiveButton(R.string.SmartSliceApplyAndSlice) { _, _ ->
                    SmartSliceEngine.apply(result)
                    fragment.refreshProfileStatus()
                    fragment.startSlicing()
                }
                .setNeutralButton(R.string.SmartSliceApplyEdit) { _, _ ->
                    SmartSliceEngine.apply(result)
                    fragment.refreshProfileStatus()
                    Toast.makeText(context, R.string.SmartSliceApplied, Toast.LENGTH_LONG).show()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }
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
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.SmartSliceWallOptional)) },
            supportingText = if (invalidWall) ({ Text(stringResource(R.string.SmartSliceInvalidDimension)) }) else null,
            isError = invalidWall,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        OutlinedTextField(
            value = layer,
            onValueChange = onLayerChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.SmartSliceLayerOptional)) },
            supportingText = if (invalidLayer) ({ Text(stringResource(R.string.SmartSliceInvalidDimension)) }) else null,
            isError = invalidLayer,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
    }
}

private fun smartSliceProgressView(context: android.content.Context): ComposeView = ComposeView(context).apply {
    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
    setContent {
        BeamComposeTheme {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                CircularProgressIndicator()
                Text(stringResource(R.string.SmartSliceAnalyzing), color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
            }
        }
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
