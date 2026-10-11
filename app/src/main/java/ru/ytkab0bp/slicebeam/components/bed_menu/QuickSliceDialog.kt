package ru.ytkab0bp.slicebeam.components.bed_menu

import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
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
import ru.ytkab0bp.slicebeam.components.BeamAlertDialogBuilder
import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.config.TemporaryProjectSettings
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import java.math.BigDecimal

/** Quick slicing with larger, scrollable fields and validated profile values. */
object QuickSliceDialog {
    @JvmStatic
    fun show(fragment: BedFragment, anchor: android.view.View) {
        val context = anchor.context
        val config = SliceBeam.requireConfig()
        val presets = config.presets ?: return
        val printerNames = config.printerConfigs.map { it.title.orEmpty() }
        val projectFilaments = TemporaryProjectSettings.slots()
        val filamentNames = if (projectFilaments != null) emptyList()
            else config.filamentConfigs.map { it.title.orEmpty() }
        val projectFilamentSummary = projectFilaments?.mapIndexed { index, slot ->
            "${index + 1}: ${slot.type.orEmpty()} #${"%06X".format(slot.color and 0xFFFFFF)}"
        }?.joinToString(" · ")
        val qualityOptions = context.resources.getStringArray(R.array.QuickSliceQualities).toList()
        val currentPrint = SliceBeam.buildCurrentConfigObject()
        val originalLayerHeight = currentPrint?.get("layer_height") ?: "0.20"
        val selectedPrinter = mutableIntStateOf(printerNames.indexOf(presets["printer"]).coerceAtLeast(0))
        val selectedFilament = mutableIntStateOf(filamentNames.indexOf(presets["filament"]).coerceAtLeast(0))
        val selectedQuality = mutableIntStateOf(1.coerceAtMost((qualityOptions.size - 1).coerceAtLeast(0)))
        val layerHeight = mutableStateOf(originalLayerHeight)
        val infill = mutableStateOf(currentPrint?.get("fill_density")?.replace("%", "") ?: "15")
        val supports = mutableStateOf(currentPrint?.get("support_material") == "1")
        val brim = mutableStateOf((currentPrint?.get("brim_width")?.toDoubleOrNull() ?: 0.0) > 0.0)
        val invalidLayerHeight = mutableStateOf(false)
        val invalidInfill = mutableStateOf(false)

        val composeView = ComposeView(context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
            setContent {
                BeamComposeTheme {
                    QuickSliceContent(
                        printerNames = printerNames,
                        filamentNames = filamentNames,
                        projectFilamentSummary = projectFilamentSummary,
                        qualityOptions = qualityOptions,
                        selectedPrinter = selectedPrinter.intValue,
                        selectedFilament = selectedFilament.intValue,
                        selectedQuality = selectedQuality.intValue,
                        onPrinterSelected = { selectedPrinter.intValue = it },
                        onFilamentSelected = { selectedFilament.intValue = it },
                        onQualitySelected = { selectedQuality.intValue = it },
                        layerHeight = layerHeight.value,
                        onLayerHeightChanged = { layerHeight.value = it; invalidLayerHeight.value = false },
                        infill = infill.value,
                        onInfillChanged = { infill.value = it; invalidInfill.value = false },
                        supports = supports.value,
                        onSupportsChanged = { supports.value = it },
                        brim = brim.value,
                        onBrimChanged = { brim.value = it },
                        invalidLayerHeight = invalidLayerHeight.value,
                        invalidInfill = invalidInfill.value,
                    )
                }
            }
        }
        val dialog = BeamAlertDialogBuilder(context)
            .setTitle(R.string.QuickSliceTitle)
            .setView(composeView)
            .setPositiveButton(R.string.QuickSliceStart, null)
            .setNegativeButton(android.R.string.cancel, null)
            .show()

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val values = QuickSliceProfileValues.resolve(
                qualityIndex = selectedQuality.intValue,
                originalLayerHeight = originalLayerHeight,
                enteredLayerHeight = layerHeight.value,
                enteredInfill = infill.value,
                supports = supports.value,
                brim = brim.value,
            )
            invalidLayerHeight.value = values.invalidLayerHeight
            invalidInfill.value = values.invalidInfill
            if (values.settings == null) return@setOnClickListener

            val selectedPrinterName = printerNames.getOrNull(selectedPrinter.intValue)
            val selectedFilamentName = filamentNames.getOrNull(selectedFilament.intValue)
            val temporary = TemporaryProjectSettings.active
            val printer = selectedPrinterName.takeIf { !temporary || it != presets["printer"] }
            val filament = selectedFilamentName.takeIf { !temporary || it != presets["filament"] }
            if ((printer != null || filament != null) && !SliceBeam.applyProfileSelection(printer, filament)) {
                Toast.makeText(context, R.string.ConfigSaveFailed, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            when (QuickSliceProfileTransaction.apply(
                config,
                QuickSliceProfileTransaction.Values(
                    layerHeight = values.settings.layerHeight,
                    infillPercent = values.settings.infillPercent,
                    supports = values.settings.supports,
                    brim = values.settings.brim,
                ),
                SliceBeam::saveConfig,
            )) {
                QuickSliceProfileTransaction.Result.SAVED -> Unit
                QuickSliceProfileTransaction.Result.PRINT_PROFILE_MISSING -> {
                    Toast.makeText(context, R.string.QuickSliceProfileMissing, Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                QuickSliceProfileTransaction.Result.SAVE_FAILED -> {
                    fragment.refreshProfileStatus()
                    Toast.makeText(context, R.string.ConfigSaveFailed, Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
            }
            fragment.refreshProfileStatus()
            dialog.dismiss()
            fragment.startSlicing()
        }
    }
}

@Composable
private fun QuickSliceContent(
    printerNames: List<String>,
    filamentNames: List<String>,
    projectFilamentSummary: String?,
    qualityOptions: List<String>,
    selectedPrinter: Int,
    selectedFilament: Int,
    selectedQuality: Int,
    onPrinterSelected: (Int) -> Unit,
    onFilamentSelected: (Int) -> Unit,
    onQualitySelected: (Int) -> Unit,
    layerHeight: String,
    onLayerHeightChanged: (String) -> Unit,
    infill: String,
    onInfillChanged: (String) -> Unit,
    supports: Boolean,
    onSupportsChanged: (Boolean) -> Unit,
    brim: Boolean,
    onBrimChanged: (Boolean) -> Unit,
    invalidLayerHeight: Boolean,
    invalidInfill: Boolean,
) {
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.76f
    Column(
        modifier = Modifier.fillMaxWidth().heightIn(max = maxHeight).verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(if (projectFilamentSummary == null) R.string.QuickSliceHelp
            else R.string.QuickSliceProjectHelp),
            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        if (printerNames.isNotEmpty()) SliceChoiceField(
            stringResource(R.string.QuickSlicePrinter), selectedPrinter, printerNames, onPrinterSelected,
        )
        if (filamentNames.isNotEmpty()) SliceChoiceField(
            stringResource(R.string.QuickSliceFilament), selectedFilament, filamentNames, onFilamentSelected,
        )
        if (projectFilamentSummary != null) Text(
            stringResource(R.string.QuickSliceProjectFilaments, projectFilamentSummary),
            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp,
        )
        SliceChoiceField(stringResource(R.string.QuickSliceQuality), selectedQuality, qualityOptions, onQualitySelected)
        OutlinedTextField(
            value = layerHeight,
            onValueChange = onLayerHeightChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.QuickSliceLayerHeight)) },
            supportingText = if (invalidLayerHeight) ({ Text(stringResource(R.string.QuickSliceInvalidLayerHeight)) }) else null,
            isError = invalidLayerHeight,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        OutlinedTextField(
            value = infill,
            onValueChange = onInfillChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.QuickSliceInfill)) },
            supportingText = if (invalidInfill) ({ Text(stringResource(R.string.QuickSliceInvalidInfill)) }) else null,
            isError = invalidInfill,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        QuickSliceToggle(stringResource(R.string.QuickSliceSupport), supports, onSupportsChanged)
        QuickSliceToggle(stringResource(R.string.QuickSliceBrim), brim, onBrimChanged)
    }
}

@Composable
private fun QuickSliceToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
    }
}

internal data class QuickSliceProfileSettings(
    val layerHeight: String,
    val infillPercent: String,
    val supports: Boolean,
    val brim: Boolean,
)

internal data class QuickSliceProfileResolution(
    val settings: QuickSliceProfileSettings?,
    val invalidLayerHeight: Boolean,
    val invalidInfill: Boolean,
)

/** Pure validation keeps quick-slice UI values safe before they are written into Orca profiles. */
internal object QuickSliceProfileValues {
    fun resolve(
        qualityIndex: Int,
        originalLayerHeight: String,
        enteredLayerHeight: String,
        enteredInfill: String,
        supports: Boolean,
        brim: Boolean,
    ): QuickSliceProfileResolution {
        val qualityDefault = when (qualityIndex) {
            0 -> "0.28"
            2 -> "0.12"
            else -> "0.20"
        }
        val enteredLayer = enteredLayerHeight.trim().replace(',', '.')
        val original = originalLayerHeight.trim().replace(',', '.').toBigDecimalOrNull()
        val enteredLayerNumber = enteredLayer.toBigDecimalOrNull()
        val useQualityDefault = enteredLayer.isEmpty() ||
            (original != null && enteredLayerNumber != null && original.compareTo(enteredLayerNumber) == 0)
        val layer = if (useQualityDefault) qualityDefault else enteredLayerNumber?.takeIf { it.signum() > 0 }?.canonical()
        val rawInfill = enteredInfill.trim().replace(',', '.').ifEmpty { "15" }
        val infillNumber = rawInfill.toBigDecimalOrNull()
        val infill = infillNumber?.takeIf { it >= BigDecimal.ZERO && it <= BigDecimal("100") }?.canonical()
        val invalidLayer = layer == null
        val invalidInfill = infill == null
        val settings = if (invalidLayer || invalidInfill) null else QuickSliceProfileSettings(
            layerHeight = layer!!,
            infillPercent = infill!!,
            supports = supports,
            brim = brim,
        )
        return QuickSliceProfileResolution(settings, invalidLayer, invalidInfill)
    }

    private fun BigDecimal.canonical(): String = stripTrailingZeros().toPlainString()
}
