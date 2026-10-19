package ru.ytkab0bp.slicebeam.components.bed_menu

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog as ComposeAlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.components.AcePhysicalSlotsBar
import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.utils.MmuFilamentSync
import ru.ytkab0bp.slicebeam.utils.AnycubicAceRoutingPolicy

internal sealed interface PrintPreparationWarning {
    data class UnavailableTool(val tool: Int, val gate: Int) : PrintPreparationWarning
    data class SharedAceGate(val tool: Int) : PrintPreparationWarning
    data class MaterialMismatch(val tool: Int, val source: String, val slot: Int, val loaded: String) : PrintPreparationWarning
    data class EmptyAceSlots(val slots: List<Int>) : PrintPreparationWarning
}

/** Shows printer, auto-leveling and ACE slot checks before sending a sliced job. */
object SlicePrintPreparationDialog {
    fun interface PrinterProfileAction {
        fun activate(printer: ConfigObject): Boolean
    }

    @JvmStatic
    fun isAnycubicKobra(printer: ConfigObject?): Boolean =
        SlicePreparationTools.isAnycubicKobra3Model(printer?.get("printer_model"))

    @JvmStatic
    internal fun show(
        fragment: BedFragment,
        printer: ConfigObject,
        print: Boolean,
        gcodeToolCount: Int,
        activeTools: IntArray,
        firstToolUse: IntArray,
        sourceFilamentTypes: List<String>,
        pressureAdvance: SlicePreparationTools.PressureAdvanceCommands,
        activateProfile: PrinterProfileAction,
        reopenAfterReslice: (ConfigObject) -> Unit,
        sendAction: SliceMenuItems.SendAction,
    ) {
        val context = fragment.getContext() ?: return
        val jobName = fragment.glView?.renderer?.gcodeResult?.getRecommendedName().orEmpty()
        val availablePrinters = runCatching {
            SliceBeam.requireConfig().printerConfigs.filter { candidate ->
                SlicePreparationTools.isSendTarget(candidate.get("host_type"), candidate.get("print_host"))
            }
        }.getOrDefault(listOf(printer))
        fun warningFor(targetPrinter: ConfigObject): PrintPreparationWarning? {
            if (isAnycubicKobra(targetPrinter) && activeTools.isNotEmpty()) {
                val title = targetPrinter.title.orEmpty()
                val routing = AnycubicAceRoutingPolicy.resolve(
                    gcodeToolCount,
                    activeTools,
                    MmuFilamentSync.loadedGates(title),
                    IntArray(gcodeToolCount) { tool -> MmuFilamentSync.defaultGateForTool(title, tool) },
                    MmuFilamentSync.getSelectedGates(title, gcodeToolCount),
                )
                val invalidTool = routing.unavailableTool
                if (invalidTool != null) {
                    return PrintPreparationWarning.UnavailableTool(invalidTool, routing.gates[invalidTool])
                }
                routing.sharedGateTool?.let { return PrintPreparationWarning.SharedAceGate(it) }
            }
            if (print && isAnycubicKobra(targetPrinter)) {
                val title = targetPrinter.title.orEmpty()
                val selectedGates = MmuFilamentSync.getSelectedGates(title, gcodeToolCount)
                val mapping = AnycubicAceRoutingPolicy.resolve(
                    gcodeToolCount, activeTools, MmuFilamentSync.loadedGates(title),
                    IntArray(gcodeToolCount) { tool -> MmuFilamentSync.defaultGateForTool(title, tool) },
                    selectedGates,
                )
                val mismatch = firstMaterialMismatch(title, activeTools, sourceFilamentTypes, mapping.gates)
                if (mismatch != null) {
                    return PrintPreparationWarning.MaterialMismatch(
                        mismatch.first, mismatch.second, mismatch.third, mismatch.fourth,
                    )
                }
                val emptyAceSlots = SlicePreparationTools.emptyAceSlotsBeforeUsedGate(
                    activeTools,
                    mapping.gates,
                    MmuFilamentSync.loadedGates(title),
                )
                if (emptyAceSlots.isNotEmpty()) {
                    return PrintPreparationWarning.EmptyAceSlots(emptyAceSlots.toList())
                }
            }
            return null
        }

        fun send(targetPrinter: ConfigObject, shouldPrint: Boolean) {
            val targetType = targetPrinter.get("host_type") ?: "octoprint"
            val targetHost = targetPrinter.get("print_host") ?: return
            val targetApiKey = targetPrinter.get("printhost_apikey")
            sendAction.send(targetType, targetHost, targetApiKey, targetPrinter, shouldPrint)
        }

        fragment.showWorkspaceComposeDialog { dismiss ->
            PrintPreparationDialog(
                jobName = jobName,
                initialPrinter = printer,
                availablePrinters = availablePrinters,
                print = print,
                gcodeToolCount = gcodeToolCount,
                activeTools = activeTools,
                firstToolUse = firstToolUse,
                sourceFilamentTypes = sourceFilamentTypes,
                pressureAdvance = pressureAdvance,
                onDismiss = dismiss,
                onSubmit = { targetPrinter, printerChanged, showWarning ->
                    val targetHost = targetPrinter.get("print_host") ?: return@PrintPreparationDialog
                    if (printerChanged) {
                        when (SlicePreparationTools.activateChangedPrinter(printer.title, targetPrinter.title) {
                            activateProfile.activate(targetPrinter)
                        }) {
                            SlicePreparationTools.PrinterActivationResult.FAILED -> return@PrintPreparationDialog
                            SlicePreparationTools.PrinterActivationResult.ACTIVATED -> {
                                dismiss()
                                Toast.makeText(context, R.string.PrintPrepareReslicing, Toast.LENGTH_LONG).show()
                                fragment.startSlicing { reopenAfterReslice(targetPrinter) }
                                return@PrintPreparationDialog
                            }
                            SlicePreparationTools.PrinterActivationResult.UNCHANGED -> Unit
                        }
                    }
                    val warning = warningFor(targetPrinter)
                    if (warning != null) showWarning(warning)
                    else {
                        dismiss()
                        send(targetPrinter, print)
                    }
                },
                onWarningUploadOnly = { targetPrinter ->
                    dismiss()
                    send(targetPrinter, shouldPrint = false)
                },
                onWarningFirmwareChecked = { targetPrinter ->
                    dismiss()
                    send(targetPrinter, shouldPrint = true)
                },
            )
        }
    }

    private data class MaterialMismatch(val first: Int, val second: String,
                                        val third: Int, val fourth: String)

    private fun firstMaterialMismatch(printerTitle: String,
                                      activeTools: IntArray, sourceFilamentTypes: List<String>,
                                      selectedGates: IntArray): MaterialMismatch? {
        for (tool in activeTools) {
            val source = sourceFilamentTypes.getOrNull(tool)?.trim().orEmpty()
            val gate = selectedGates.getOrNull(tool) ?: continue
            val actual = MmuFilamentSync.gateSlot(printerTitle, gate)?.type?.trim().orEmpty()
            if (!SlicePreparationTools.materialMatchesSlicedFile(source, actual)) {
                return MaterialMismatch(tool, source.ifBlank { "?" }, gate + 1, actual.ifBlank { "?" })
            }
        }
        return null
    }

    internal fun formatToolMapping(context: Context, printerTitle: String,
                                   firstToolUse: IntArray, selectedGates: IntArray? = null): String {
        return buildString {
            append(context.getString(R.string.PrintPrepareToolOrder,
                firstToolUse.joinToString(" → ") { tool -> "T$tool" }))
            firstToolUse.forEach { tool ->
                append("\nT").append(tool).append(" → ")
                    .append(MmuFilamentSync.gateLabel(printerTitle, selectedGates?.getOrNull(tool) ?: tool))
            }
        }
    }
}

@Composable
private fun PrintPreparationDialog(
    jobName: String,
    initialPrinter: ConfigObject,
    availablePrinters: List<ConfigObject>,
    print: Boolean,
    gcodeToolCount: Int,
    activeTools: IntArray,
    firstToolUse: IntArray,
    sourceFilamentTypes: List<String>,
    pressureAdvance: SlicePreparationTools.PressureAdvanceCommands,
    onDismiss: () -> Unit,
    onSubmit: (ConfigObject, Boolean, (PrintPreparationWarning) -> Unit) -> Unit,
    onWarningUploadOnly: (ConfigObject) -> Unit,
    onWarningFirmwareChecked: (ConfigObject) -> Unit,
) {
    var printer by remember(initialPrinter) { mutableStateOf(initialPrinter) }
    var resliceRequired by remember(initialPrinter.title) { mutableStateOf(false) }
    var warning by remember { mutableStateOf<PrintPreparationWarning?>(null) }
    val context = LocalContext.current
    val selectedGates = MmuFilamentSync.getSelectedGates(printer.title.orEmpty(), gcodeToolCount)
        ?: IntArray(gcodeToolCount) { tool -> MmuFilamentSync.defaultGateForTool(printer.title.orEmpty(), tool) }
    val mappingSummary = SlicePrintPreparationDialog.formatToolMapping(
        context, printer.title.orEmpty(), firstToolUse, selectedGates,
    )
    val emptyAceSlots = remember(printer.title, selectedGates.contentHashCode()) {
        SlicePreparationTools.emptyAceSlotsBeforeUsedGate(
            activeTools,
            selectedGates,
            MmuFilamentSync.loadedGates(printer.title.orEmpty()),
        )
    }
    ComposeAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.PrintPrepareTitle), modifier = Modifier.testTag("slice-preparation-title")) },
        text = {
            PrintPreparationContent(
                jobName = jobName,
                printer = printer,
                availablePrinters = availablePrinters,
                resliceRequired = resliceRequired,
                mappingSummary = mappingSummary.takeIf {
                    !resliceRequired && SlicePrintPreparationDialog.isAnycubicKobra(printer) && activeTools.isNotEmpty()
                },
                emptyAceSlots = emptyAceSlots.takeIf {
                    !resliceRequired && SlicePrintPreparationDialog.isAnycubicKobra(printer) && it.isNotEmpty()
                },
                pressureAdvance = pressureAdvance,
                onSelectPrinter = { chosen ->
                    printer = chosen
                    resliceRequired = SlicePreparationTools.didPrinterChange(
                        initialPrinter.title,
                        chosen.title,
                    )
                },
            )
        },
        confirmButton = {
            val label = when {
                resliceRequired -> R.string.PrintPrepareResliceButton
                print -> R.string.MenuSliceSendToPrinterAndPrint
                else -> R.string.MenuSliceSendToPrinter
            }
            TextButton(
                onClick = { onSubmit(printer, resliceRequired) { warning = it } },
                modifier = Modifier.heightIn(min = 48.dp).testTag("slice-preparation-confirm"),
            ) { Text(stringResource(label)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp).testTag("slice-preparation-cancel")) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
    warning?.let { activeWarning ->
        PrintPreparationWarningDialog(
            warning = activeWarning,
            onDismiss = { warning = null },
            onAcknowledge = { warning = null },
            onUploadOnly = {
                warning = null
                onWarningUploadOnly(printer)
            },
            onFirmwareChecked = {
                warning = null
                onWarningFirmwareChecked(printer)
            },
        )
    }
}

@Composable
internal fun PrintPreparationWarningDialog(
    warning: PrintPreparationWarning,
    onDismiss: () -> Unit,
    onAcknowledge: () -> Unit,
    onUploadOnly: () -> Unit,
    onFirmwareChecked: () -> Unit,
) {
            val title = when (warning) {
        is PrintPreparationWarning.UnavailableTool -> R.string.MmuAssignmentTitle
        is PrintPreparationWarning.SharedAceGate -> R.string.MmuAssignmentTitle
        is PrintPreparationWarning.MaterialMismatch -> R.string.PrintPrepareMaterialMismatchTitle
        is PrintPreparationWarning.EmptyAceSlots -> R.string.PrintPrepareAceGapTitle
    }
    val message = when (warning) {
        is PrintPreparationWarning.UnavailableTool -> stringResource(
            R.string.MmuDirectSlotUnavailable, warning.tool, warning.gate + 1,
        )
        is PrintPreparationWarning.SharedAceGate -> stringResource(
            R.plurals.MmuAssignmentSharedAceGate, warning.tool + 1, warning.tool + 1,
        )
        is PrintPreparationWarning.MaterialMismatch -> stringResource(
            R.string.PrintPrepareMaterialMismatch, warning.tool, warning.source, warning.slot, warning.loaded,
        )
        is PrintPreparationWarning.EmptyAceSlots -> stringResource(
            R.string.PrintPrepareAceGapWarning, warning.slots.joinToString(", "),
        )
    }
    ComposeAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(title)) },
        text = { Text(message) },
        confirmButton = {
            val label = when (warning) {
                is PrintPreparationWarning.UnavailableTool -> android.R.string.ok
                is PrintPreparationWarning.SharedAceGate -> android.R.string.ok
                is PrintPreparationWarning.MaterialMismatch -> R.string.PrintPrepareUploadOnly
                is PrintPreparationWarning.EmptyAceSlots -> R.string.PrintPrepareAceGapFirmwareChecked
            }
            TextButton(
                onClick = when (warning) {
                    is PrintPreparationWarning.UnavailableTool -> onAcknowledge
                    is PrintPreparationWarning.SharedAceGate -> onAcknowledge
                    is PrintPreparationWarning.MaterialMismatch -> onUploadOnly
                    is PrintPreparationWarning.EmptyAceSlots -> onFirmwareChecked
                },
                modifier = Modifier.heightIn(min = 48.dp).then(
                    if (warning is PrintPreparationWarning.EmptyAceSlots) Modifier.testTag("ace-gap-confirm-print")
                    else Modifier,
                ),
            ) { Text(stringResource(label)) }
        },
        dismissButton = when (warning) {
            is PrintPreparationWarning.UnavailableTool -> null
            is PrintPreparationWarning.SharedAceGate -> null
            is PrintPreparationWarning.MaterialMismatch -> {
                { TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(android.R.string.cancel))
                } }
            }
            is PrintPreparationWarning.EmptyAceSlots -> {
                {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp).testTag("ace-gap-cancel")) {
                            Text(stringResource(android.R.string.cancel))
                        }
                        TextButton(onClick = onUploadOnly, modifier = Modifier.heightIn(min = 48.dp).testTag("ace-gap-upload-only")) {
                            Text(stringResource(R.string.PrintPrepareUploadOnly))
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun PrintPreparationContent(
    jobName: String,
    printer: ConfigObject,
    availablePrinters: List<ConfigObject>,
    resliceRequired: Boolean,
    mappingSummary: String?,
    emptyAceSlots: IntArray?,
    pressureAdvance: SlicePreparationTools.PressureAdvanceCommands,
    onSelectPrinter: (ConfigObject) -> Unit,
) {
    var showPrinterPicker by remember { mutableStateOf(false) }
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.78f
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    val primary = MaterialTheme.colorScheme.onSurface
    val rowShape = RoundedCornerShape(12.dp)
    val plate = printer.get("curr_bed_type")
        ?.takeIf(String::isNotEmpty)
        ?: stringResource(R.string.PrintPrepareDefaultPlate)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = jobName.ifBlank { stringResource(R.string.PrintPrepareTitle) },
            color = primary,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(rowShape)
                .clickable(role = Role.Button) { showPrinterPicker = true }
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.PrintPrepareSummary, printer.title.orEmpty(), plate),
                    color = primary,
                    fontSize = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(R.string.PrintPrepareSelectPrinter),
                    color = secondary,
                    fontSize = 13.sp,
                )
                Text(
                    stringResource(R.string.PrintPrepareTargetHost, printer.get("print_host").orEmpty()),
                    color = secondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
            }
            Text("▾", color = secondary, fontSize = 20.sp)
        }
        if (!resliceRequired && SlicePrintPreparationDialog.isAnycubicKobra(printer)) {
            AcePhysicalSlotsBar(MmuFilamentSync.physicalGates(printer.title.orEmpty()))
        }
        Text(
            stringResource(
                if (SlicePreparationTools.hasAutomaticBedLeveling(printer.get("machine_start_gcode"))) {
                    R.string.PrintPrepareAutoLevelEnabled
                } else {
                    R.string.PrintPrepareAutoLevelUnavailable
                },
            ),
            color = secondary,
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        if (resliceRequired) {
            Text(
                stringResource(R.string.PrintPrepareResliceHint),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        } else {
            val filamentPa = pressureAdvance.filament.joinToString(", ")
            val customPa = pressureAdvance.custom.joinToString(", ")
            Text(
                text = when {
                    filamentPa.isNotEmpty() -> stringResource(R.string.PrintPreparePressureAdvanceFilament, filamentPa)
                    customPa.isNotEmpty() -> stringResource(R.string.PrintPreparePressureAdvanceCustomOnly, customPa)
                    else -> stringResource(R.string.PrintPreparePressureAdvanceNone)
                },
                color = if (filamentPa.isEmpty()) MaterialTheme.colorScheme.error else secondary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            if (filamentPa.isNotEmpty() && customPa.isNotEmpty() && customPa != filamentPa) {
                Text(stringResource(R.string.PrintPreparePressureAdvanceAlsoCustom, customPa),
                    color = MaterialTheme.colorScheme.error, fontSize = 13.sp, lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 4.dp))
            }
        }
        if (mappingSummary != null) {
            Text(
                text = mappingSummary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(rowShape)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                color = primary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
        }
        if (emptyAceSlots != null) {
            Text(
                text = stringResource(R.string.PrintPrepareAceGapHint, emptyAceSlots.joinToString(", ")),
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }

    if (showPrinterPicker) {
        ComposeAlertDialog(
            onDismissRequest = { showPrinterPicker = false },
            title = { Text(stringResource(R.string.PrintPrepareSelectPrinter)) },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = LocalConfiguration.current.screenHeightDp.dp * 0.58f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    availablePrinters.forEach { candidate ->
                        TextButton(
                            onClick = {
                                onSelectPrinter(candidate)
                                showPrinterPicker = false
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        ) {
                            Text(
                                text = candidate.title.orEmpty(),
                                modifier = Modifier.fillMaxWidth(),
                                color = if (candidate.title == printer.title) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    primary
                                },
                                fontSize = 16.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPrinterPicker = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}
