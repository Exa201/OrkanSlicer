package ru.ytkab0bp.slicebeam.components.bed_menu

import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import ru.ytkab0bp.eventbus.EventHandler
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.components.UnfoldMenu
import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.config.ProfileSettingsRepository
import ru.ytkab0bp.slicebeam.events.NeedDismissCalibrationsMenu
import ru.ytkab0bp.slicebeam.events.NeedSnackbarEvent
import ru.ytkab0bp.slicebeam.events.ObjectsListChangedEvent
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.fragment.ProfileConfigValueController
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import ru.ytkab0bp.slicebeam.ui.ThemeAwareComposeView
import ru.ytkab0bp.slicebeam.utils.ViewUtils
import java.io.File
import java.io.FileOutputStream

/** Calibration actions and test models opened from the File menu. */
class CalibrationsMenu : UnfoldMenu() {
    override fun getRequestedSize(into: android.widget.FrameLayout, portrait: Boolean): Int =
        (if (portrait) into.height * 0.56f else into.width * 0.6f).toInt()

    @Composable
    override fun ComposeContent(portrait: Boolean) {
        val context = LocalContext.current
        val bed = fragment
        if (bed == null) {
            Spacer(Modifier.fillMaxSize())
        } else {
            var selectedCalibration by remember { mutableStateOf<CalibrationPreset?>(null) }
            var recordResult by remember { mutableStateOf(false) }
            CalibrationsMenuContent(
                onCalibration = { selectedCalibration = it },
                onRecordResult = { recordResult = true },
                onModels = {
                    dismiss()
                    ViewUtils.postOnMainThread({
                        bed.glView?.let { bed.showUnfoldMenu(CalibrationModelsMenu(), it) }
                    }, 180)
                },
                onBack = { dismiss() },
            )
            selectedCalibration?.let { preset ->
                CalibrationConfigurationDialog(
                    preset = preset,
                    onDismiss = { selectedCalibration = null },
                    onStart = { start, endOrStep, step ->
                        selectedCalibration = null
                        startCalibration(context, bed, preset, start, endOrStep, step)
                    },
                )
            }
            if (recordResult) {
                CalibrationResultDialog(
                    onDismiss = { recordResult = false },
                    onSave = { mode, rawValue ->
                        val target = CalibrationResultPolicy.targetFor(mode)
                        val value = target?.let { CalibrationResultPolicy.parseValue(rawValue, it) }
                        if (target == null || value == null) {
                            false
                        } else {
                            val config = SliceBeam.requireConfig()
                            val profileKey = if (target.profileType == ConfigObject.PROFILE_LIST_FILAMENT)
                                "filament" else "print"
                            val profileName = config.presets?.get(profileKey)
                            val profile = when (target.profileType) {
                                ConfigObject.PROFILE_LIST_FILAMENT -> config.findFilament(profileName)
                                ConfigObject.PROFILE_LIST_PRINT -> config.findPrint(profileName)
                                else -> null
                            } ?: return@CalibrationResultDialog false
                            ProfileConfigValueController.stageValue(
                                target.profileType,
                                profile,
                                target.key,
                                guiType = null,
                                index = target.vectorIndex,
                                editedValue = value,
                                defaultValue = profile.get(target.key),
                            )
                            Toast.makeText(context, R.string.CalibrationResultStaged, Toast.LENGTH_LONG).show()
                            true
                        }
                    },
                )
            }
        }
    }

    private fun startCalibration(
        context: Context,
        bed: BedFragment,
        preset: CalibrationPreset,
        start: Double,
        endOrStep: Double,
        step: Double?,
    ) {
        val bedGeometry = bed.glView?.renderer?.bed
        if (bedGeometry == null || !bedGeometry.isValid()) {
            Toast.makeText(context, R.string.BedConfigurationError, Toast.LENGTH_SHORT).show()
            return
        }
        SliceBeam.PENDING_CALIB_MODE = preset.mode
        SliceBeam.PENDING_CALIB_START = start
        SliceBeam.PENDING_CALIB_END = if (preset.inputMode != CalibrationInputMode.START_AND_STEP) endOrStep else 0.0
        SliceBeam.PENDING_CALIB_STEP = step ?: preset.step ?: endOrStep
        val model = bed.glView?.renderer?.model
        if (model == null || model.objectsCount == 0) {
            if (preset.mode != CalibrationPresets.PA_LINE) {
                clearPendingCalibration()
                Toast.makeText(context, R.string.CalibrationNeedsModel, Toast.LENGTH_LONG).show()
                return
            }
            ensurePlaceholderModel(bed) {
                finishCalibrationSelection(context)
            }
        } else {
            finishCalibrationSelection(context)
        }
    }

    private fun finishCalibrationSelection(context: Context) {
        Toast.makeText(context, R.string.CalibrationReadyToSlice, Toast.LENGTH_LONG).show()
        SliceBeam.EVENT_BUS.fireEvent(NeedDismissCalibrationsMenu())
        dismiss()
    }

    private fun clearPendingCalibration() {
        SliceBeam.PENDING_CALIB_MODE = 0
        SliceBeam.PENDING_CALIB_START = 0.0
        SliceBeam.PENDING_CALIB_END = 0.0
        SliceBeam.PENDING_CALIB_STEP = 0.0
    }

    private fun ensurePlaceholderModel(bed: BedFragment, onLoaded: () -> Unit) {
        val file = File(SliceBeam.getModelCacheDir(), "calibration_pa_test.stl")
        Thread({
            try {
                SliceBeam.requireInstance().assets.open("models/pa_test.stl").use { input ->
                    FileOutputStream(file).use { output -> input.copyTo(output) }
                }
                ViewUtils.postOnMainThread {
                    bed.loadModel(file, false, 1, object : BedFragment.ModelLoadCallback {
                        override fun onLoaded(loadedModel: ru.ytkab0bp.slicebeam.slic3r.Model, firstNewObject: Int, addedObjects: Int) {
                            ViewUtils.postOnMainThread {
                                SliceBeam.EVENT_BUS.fireEvent(ObjectsListChangedEvent())
                                onLoaded()
                            }
                        }

                        override fun onFailed(error: Exception) {
                            clearPendingCalibration()
                            showLoadError(bed, error)
                        }
                    })
                }
            } catch (error: Exception) {
                android.util.Log.e("FileMenu", "Failed to load PA placeholder model", error)
                clearPendingCalibration()
                ViewUtils.postOnMainThread { showLoadError(bed, error) }
            }
        }, "CalibrationPlaceholderLoader").start()
    }

    private fun showLoadError(bed: BedFragment, error: Exception) {
        val context = bed.getContext() ?: return
        bed.showWorkspaceMessageDialog(
            title = context.getString(R.string.MenuFileOpenFileFailed),
            message = error.toString(),
            confirmLabel = context.getString(android.R.string.ok),
        )
    }

    @EventHandler(runOnMainThread = true)
    fun onDismiss(event: NeedDismissCalibrationsMenu) {
        dismiss()
    }

    override fun onCreate() {
        super.onCreate()
        SliceBeam.EVENT_BUS.registerListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        SliceBeam.EVENT_BUS.unregisterListener(this)
    }
}

class CalibrationModelsMenu : UnfoldMenu() {
    override fun getRequestedSize(into: android.widget.FrameLayout, portrait: Boolean): Int =
        if (portrait) into.height - into.paddingTop - into.paddingBottom else into.width

    @Composable
    override fun ComposeContent(portrait: Boolean) {
        val context = LocalContext.current
        val bed = fragment
        val models = remember(context) {
            listOf(
                CalibrationModel("Orca Cube", R.drawable.model_thumb_orcacube_v2, listOf("OrcaCube_v2.stl", "OrcaPlug_v2.stl")),
                CalibrationModel("Orca Tolerance Test", R.drawable.model_thumb_orcatolerancetest, listOf("OrcaToleranceTest.stl")),
                CalibrationModel(context.getString(R.string.MenuFileCalibrationsModels3DBenchy), R.drawable.model_thumb_3dbenchy, listOf("3dbenchy.stl")),
                CalibrationModel("Cali Cat", R.drawable.model_thumb_calicat, listOf("calicat.stl")),
                CalibrationModel("Autodesk FDM Test", R.drawable.model_thumb_ksr_fdmtest_v4, listOf("ksr_fdmtest_v4.stl")),
                CalibrationModel("Voron Cube", R.drawable.model_thumb_voron_design_cube_v7, listOf("Voron_Design_Cube_v7.stl")),
                CalibrationModel("Stanford Bunny", R.drawable.model_thumb_stanford_bunny, listOf("Stanford_Bunny.stl")),
                CalibrationModel("Orca String Hell", R.drawable.model_thumb_orca_stringhell, listOf("Orca_stringhell.stl")),
            )
        }
        if (bed == null) {
            Spacer(Modifier.fillMaxSize())
        } else {
            CalibrationModelsContent(
                models = models,
                onBack = { dismiss() },
                onSelect = { model -> loadModels(bed, model.assets) },
            )
        }
    }

    private fun loadModels(bed: BedFragment, assetNames: List<String>) {
        SliceBeam.EVENT_BUS.fireEvent(NeedDismissCalibrationsMenu())
        dismiss()
        ViewUtils.postOnMainThread({
            Thread({
                val files = mutableListOf<File>()
                try {
                    assetNames.forEach { assetName ->
                        val file = File(SliceBeam.getModelCacheDir(), "calibration_model_$assetName")
                        SliceBeam.requireInstance().assets.open("models/$assetName").use { input ->
                            FileOutputStream(file).use { output -> input.copyTo(output) }
                        }
                        files += file
                    }
                    ViewUtils.postOnMainThread { loadNextModel(bed, files, 0) }
                } catch (error: Exception) {
                    files.forEach(File::delete)
                    ViewUtils.postOnMainThread { showLoadError(bed, error) }
                }
            }, "CalibrationModelAssetLoader").start()
        }, 200)
    }

    /** Loads multi-part presets one at a time; BedFragment rejects overlapping native imports. */
    private fun loadNextModel(bed: BedFragment, files: List<File>, index: Int) {
        if (index >= files.size) {
            files.forEach(File::delete)
            SliceBeam.EVENT_BUS.fireEvent(ObjectsListChangedEvent())
            SliceBeam.EVENT_BUS.fireEvent(NeedSnackbarEvent(R.string.MenuFileOpenFileLoaded))
            return
        }
        try {
            bed.loadModel(files[index], false, 1, object : BedFragment.ModelLoadCallback {
                override fun onLoaded(loadedModel: ru.ytkab0bp.slicebeam.slic3r.Model, firstNewObject: Int, addedObjects: Int) {
                    ViewUtils.postOnMainThread { loadNextModel(bed, files, index + 1) }
                }

                override fun onFailed(error: Exception) {
                    files.forEach(File::delete)
                    showLoadError(bed, error)
                }
            })
        } catch (error: Exception) {
            files.forEach(File::delete)
            showLoadError(bed, error)
        }
    }

    private fun showLoadError(bed: BedFragment, error: Exception) {
        val context = bed.getContext() ?: return
        bed.showWorkspaceMessageDialog(
            title = context.getString(R.string.MenuFileOpenFileFailed),
            message = error.toString(),
            confirmLabel = context.getString(android.R.string.ok),
        )
    }

}

private data class CalibrationModel(val title: String, val icon: Int, val assets: List<String>)

@Composable
private fun CalibrationsMenuContent(
    onCalibration: (CalibrationPreset) -> Unit,
    onRecordResult: () -> Unit,
    onModels: (View) -> Unit,
    onBack: () -> Unit,
) {
    val anchor = LocalView.current
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(CalibrationPresets.supported()) { preset ->
                CalibrationActionRow(
                    icon = if (preset.mode == CalibrationPresets.PA_LINE || preset.mode == CalibrationPresets.PA_TOWER) R.drawable.menu_calibrate_la_28 else R.drawable.deployed_code_24,
                    title = stringResource(preset.titleRes),
                    subtitle = stringResource(preset.descriptionRes),
                    onClick = { onCalibration(preset) },
                )
            }
            item {
                CalibrationActionRow(
                    icon = R.drawable.deployed_code_24,
                    title = stringResource(R.string.CalibrationRecordResult),
                    subtitle = stringResource(R.string.CalibrationRecordResultDescription),
                    onClick = onRecordResult,
                )
            }
            item {
                CalibrationActionRow(
                    icon = R.drawable.deployed_code_24,
                    title = stringResource(R.string.MenuFileCalibrationsModels),
                    subtitle = stringResource(R.string.MenuFileCalibrationsModelsDescription),
                    onClick = { onModels(anchor) },
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        CalibrationBackBar(onBack)
    }
}

@Composable
private fun CalibrationConfigurationDialog(
    preset: CalibrationPreset,
    onDismiss: () -> Unit,
    onStart: (Double, Double, Double?) -> Unit,
) {
    val config = remember(preset.mode) { SliceBeam.buildCurrentConfigObject() }
    val profileTemp = config["nozzle_temperature"]?.substringBefore(';')?.toDoubleOrNull()
        ?: config["nozzle_temperature_initial_layer"]?.substringBefore(';')?.toDoubleOrNull()
        ?: preset.start
    val initialStart = if (preset.mode == CalibrationPresets.TEMP_TOWER) profileTemp else preset.start
    val initialEnd = if (preset.mode == CalibrationPresets.TEMP_TOWER) profileTemp - 20.0 else preset.endOrStep
    var start by remember(preset.mode) { mutableStateOf(initialStart.toString()) }
    var endOrStep by remember(preset.mode) { mutableStateOf(initialEnd.toString()) }
    var step by remember(preset.mode) { mutableStateOf((preset.step ?: 0.0).toString()) }
    var invalid by remember(preset.mode) { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(preset.titleRes)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(if (preset.mode == CalibrationPresets.TEMP_TOWER) R.string.CalibrationTemperatureProfileHint else R.string.CalibrationParameterHint),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                CalibrationNumberField(stringResource(R.string.CalibrationStart), start, { start = it; invalid = false })
                if (preset.inputMode != CalibrationInputMode.START_AND_STEP) {
                    CalibrationNumberField(stringResource(R.string.CalibrationEnd), endOrStep, { endOrStep = it; invalid = false })
                }
                if (preset.inputMode == CalibrationInputMode.RANGE_AND_STEP) {
                    CalibrationNumberField(stringResource(R.string.CalibrationStep), step, { step = it; invalid = false })
                } else if (preset.inputMode == CalibrationInputMode.START_AND_STEP) {
                    CalibrationNumberField(stringResource(R.string.CalibrationIncrement), endOrStep, { endOrStep = it; invalid = false })
                }
                if (invalid) Text(stringResource(R.string.CalibrationInvalidParameters), color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val startValue = start.trim().replace(',', '.').toDoubleOrNull()
                val endValue = endOrStep.trim().replace(',', '.').toDoubleOrNull()
                val stepValue = if (preset.inputMode == CalibrationInputMode.RANGE_AND_STEP) step.trim().replace(',', '.').toDoubleOrNull() else null
                if (startValue == null || endValue == null ||
                    (preset.inputMode == CalibrationInputMode.RANGE_AND_STEP && stepValue == null) ||
                    !CalibrationPresets.validate(preset, startValue, endValue, stepValue)) {
                    invalid = true
                } else onStart(startValue, endValue, stepValue)
            }) { Text(stringResource(R.string.CalibrationPrepare)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
    )
}

@Composable
private fun CalibrationResultDialog(
    onDismiss: () -> Unit,
    onSave: (Int, String) -> Boolean,
) {
    val modes = CalibrationPresets.supported().map { it.mode }
        .distinctBy { CalibrationResultPolicy.targetFor(it) }
    var mode by remember { mutableStateOf(CalibrationPresets.PA_LINE) }
    var expanded by remember { mutableStateOf(false) }
    var value by remember { mutableStateOf("") }
    var invalid by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.CalibrationRecordResult)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.CalibrationRecordResultDescription),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                Box {
                    TextButton(onClick = { expanded = true }) {
                        Text(stringResource(calibrationTitle(mode)))
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        modes.forEach { candidate ->
                            DropdownMenuItem(
                                text = { Text(stringResource(calibrationTitle(candidate))) },
                                onClick = { mode = candidate; expanded = false; invalid = false },
                            )
                        }
                    }
                }
                CalibrationNumberField(stringResource(R.string.CalibrationMeasuredValue), value, {
                    value = it
                    invalid = false
                })
                if (invalid) Text(stringResource(R.string.CalibrationResultInvalid), color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(onClick = { if (onSave(mode, value)) onDismiss() else invalid = true }) {
                Text(stringResource(R.string.CalibrationSaveResult))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
    )
}

private fun calibrationTitle(mode: Int): Int = when (mode) {
    CalibrationPresets.PA_LINE, CalibrationPresets.PA_TOWER -> R.string.CalibrationPressureAdvanceShort
    CalibrationPresets.TEMP_TOWER -> R.string.CalibrationTemperatureTower
    CalibrationPresets.VOLUMETRIC_SPEED_TOWER -> R.string.CalibrationVolumetricSpeedTower
    CalibrationPresets.VFA_TOWER -> R.string.CalibrationVfaTower
    CalibrationPresets.RETRACTION_TOWER -> R.string.CalibrationRetractionTower
    else -> R.string.CalibrationPressureAdvanceShort
}

@Composable
private fun CalibrationNumberField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { candidate ->
            if (candidate.length <= 12 && candidate.all { it.isDigit() || it == '.' || it == ',' || it == '-' }) onValueChange(candidate)
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun CalibrationActionRow(
    icon: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
            .clickable(role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.size(24.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun CalibrationModelsContent(
    models: List<CalibrationModel>,
    onBack: () -> Unit,
    onSelect: (CalibrationModel) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        CalibrationBackBar(onBack)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(models) { model ->
                Surface(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                        .clickable(role = Role.Button) { onSelect(model) },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Image(
                            painter = painterResource(model.icon),
                            contentDescription = model.title,
                            modifier = Modifier.size(24.dp).clip(RoundedCornerShape(8.dp)),
                        )
                        Text(
                            text = model.title,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalibrationBackBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.MenuOrientationPositionBack), onClick = onBack)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.arrow_left_outline_28),
            contentDescription = null,
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant),
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = stringResource(R.string.MenuOrientationPositionBack),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
