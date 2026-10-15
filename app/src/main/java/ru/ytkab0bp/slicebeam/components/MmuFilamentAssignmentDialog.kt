package ru.ytkab0bp.slicebeam.components

import android.content.Context
import android.graphics.Color as AndroidColor
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import ru.ytkab0bp.slicebeam.utils.FilamentSlot
import ru.ytkab0bp.slicebeam.utils.MmuFilamentSync
import ru.ytkab0bp.slicebeam.utils.Prefs
import java.util.Locale

/** Lets each slicer tool route to a loaded physical ACE gate. */
object MmuFilamentAssignmentDialog {
    fun interface Callback {
        fun onSelected(gates: IntArray)
    }

    @JvmStatic
    fun show(context: Context, printerTitle: String, toolCount: Int, callback: Callback): Boolean {
        return showInternal(context, printerTitle, toolCount, null, false, R.string.MmuAssignmentContinue, null, callback)
    }

    fun showForModel(context: Context, printerTitle: String, toolCount: Int, usedTools: IntArray,
                     onSync: () -> Unit, callback: Callback): Boolean =
        showInternal(context, printerTitle, toolCount, usedTools, true, R.string.ModelColorSlotsApply, onSync, callback)

    fun showForActiveTools(context: Context, printerTitle: String, toolCount: Int,
                           usedTools: IntArray, callback: Callback): Boolean =
        showInternal(context, printerTitle, toolCount, usedTools, false,
            R.string.ModelColorSlotsApply, null, callback)

    internal fun availableGates(printerTitle: String, allowUnverified: Boolean): IntArray =
        if (MmuFilamentSync.getSnapshot(printerTitle) == null && allowUnverified) intArrayOf(0, 1, 2, 3)
        else MmuFilamentSync.loadedGates(printerTitle).filter {
            MmuFilamentSync.gateSlot(printerTitle, it) != null &&
                MmuFilamentSync.toolForGate(printerTitle, it) >= 0
        }.toIntArray()

    internal fun initialMapping(printerTitle: String, toolCount: Int, loadedGates: IntArray): IntArray {
        val existing = MmuFilamentSync.getSelectedGates(printerTitle, toolCount)
        val palette = Prefs.getFilamentSlots()
        val options = loadedGates.map { gate ->
            val slot = MmuFilamentSync.gateSlot(printerTitle, gate)
            MmuColorAssignmentPolicy.Slot(gate, slot?.color, slot?.type)
        }
        return IntArray(toolCount) { tool ->
            existing?.getOrNull(tool)?.takeIf { it in loadedGates }
                ?: MmuColorAssignmentPolicy.suggest(palette.getOrNull(tool)?.color, palette.getOrNull(tool)?.type,
                    MmuFilamentSync.defaultGateForTool(printerTitle, tool).takeIf { it >= 0 } ?: tool, options)
        }
    }

    internal fun hasMaterialMismatch(printerTitle: String, mapping: List<Int>, usedTools: List<Int>): Boolean {
        val palette = Prefs.getFilamentSlots()
        return usedTools.any { tool ->
            val source = palette.getOrNull(tool)?.type
            val target = mapping.getOrNull(tool)?.let { MmuFilamentSync.gateSlot(printerTitle, it)?.type }
            MmuColorAssignmentPolicy.materialMismatch(source, target)
        }
    }

    private fun showInternal(context: Context, printerTitle: String, toolCount: Int, usedTools: IntArray?,
                             allowUnverified: Boolean, positiveLabel: Int, onSync: (() -> Unit)?, callback: Callback): Boolean {
        val loadedGates = availableGates(printerTitle, allowUnverified)
        if (loadedGates.isEmpty()) return false
        val selectedGates = initialMapping(printerTitle, toolCount, loadedGates)
        val mapping = mutableStateOf(selectedGates.toList())
        val composeView = ComposeView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
            setContent {
                BeamComposeTheme {
                    MmuAssignmentContent(
                        printerTitle = printerTitle,
                        toolCount = toolCount,
                        loadedGates = loadedGates,
                        palette = Prefs.getFilamentSlots(),
                        mapping = mapping.value,
                        usedTools = usedTools,
                        onMappingChange = { mapping.value = it },
                    )
                }
            }
        }
        val dialog = BeamAlertDialogBuilder(context)
            .setTitle(R.string.MmuAssignmentTitle)
            .setView(composeView)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(positiveLabel, null)
            .apply { if (onSync != null) setNeutralButton(R.string.MmuSyncShort) { _, _ -> onSync() } }
            .show()

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val result = mapping.value.toIntArray()
            if (hasMaterialMismatch(printerTitle, mapping.value, usedTools?.toList() ?: (0 until toolCount).toList())) {
                Toast.makeText(context, R.string.ModelColorSlotsMaterialMismatch, Toast.LENGTH_LONG).show()
            }
            MmuFilamentSync.setSelectedGates(printerTitle, result)
            dialog.dismiss()
            callback.onSelected(result.clone())
        }
        return true
    }

}

@Composable
internal fun MmuAssignmentContent(
    printerTitle: String,
    toolCount: Int,
    loadedGates: IntArray,
    palette: List<FilamentSlot>,
    mapping: List<Int>,
    usedTools: IntArray? = null,
    showHelp: Boolean = true,
    onMappingChange: (List<Int>) -> Unit,
) {
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.78f
    val primary = MaterialTheme.colorScheme.onSurface
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant
    val rowShape = RoundedCornerShape(12.dp)
    var choosingTool by remember { mutableIntStateOf(-1) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AcePhysicalSlotsBar(MmuFilamentSync.physicalGates(printerTitle))
        if (showHelp) Text(
            stringResource(R.string.MmuAssignmentHelp),
            color = secondary,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
        if (MmuFilamentSync.getSnapshot(printerTitle) == null) Text(
            stringResource(R.string.ModelColorSlotsUnverified), color = secondary, fontSize = 13.sp,
        )
        val tools = usedTools?.filter { it in 0 until toolCount } ?: (0 until toolCount).toList()
        tools.forEach { tool ->
            val slot = palette.getOrNull(tool)
            val material = slot?.type?.takeIf(String::isNotBlank) ?: stringResource(R.string.FilamentType)
            val colorHex = slot?.let { String.format(Locale.ROOT, "#%06X", it.color and COLOR_MASK) }.orEmpty()
            val gate = mapping.getOrElse(tool) { loadedGates.first() }
            val gateLabel = physicalSlotLabel(printerTitle, gate)
            val label = stringResource(R.string.MmuAssignmentRow, tool, material, colorHex, gateLabel)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(rowShape)
                    .clickable(role = Role.Button) { choosingTool = tool }
                    .padding(horizontal = 10.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color((slot?.color ?: AndroidColor.DKGRAY) or 0xFF000000.toInt()))
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                )
                Text(
                    label,
                    modifier = Modifier.weight(1f),
                    color = primary,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                )
                val physical = MmuFilamentSync.gateSlot(printerTitle, gate)
                if (physical != null) Box(Modifier.size(24.dp).clip(CircleShape).background(Color(physical.color or 0xFF000000.toInt()))
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape))
            }
        }
    }

    if (choosingTool >= 0) {
        val tool = choosingTool
        val checkedGate = mapping.getOrElse(tool) { loadedGates.first() }
        AlertDialog(
            onDismissRequest = { choosingTool = -1 },
            title = { Text(stringResource(R.string.MmuAssignmentTool, tool)) },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = LocalConfiguration.current.screenHeightDp.dp * 0.55f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    loadedGates.forEach { gate ->
                        val selected = gate == checkedGate
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(rowShape)
                                .clickable(role = Role.RadioButton) {
                                    onMappingChange(mapping.toMutableList().also { it[tool] = gate })
                                    choosingTool = -1
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = selected, onClick = null)
                                Text(
                                physicalSlotLabel(printerTitle, gate),
                                modifier = Modifier.weight(1f),
                                color = if (selected) MaterialTheme.colorScheme.primary else primary,
                                fontSize = 16.sp,
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { choosingTool = -1 }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}

private const val COLOR_MASK = 0xFFFFFF

@Composable
internal fun physicalSlotLabel(printerTitle: String, gate: Int): String {
    val slot = MmuFilamentSync.gateSlot(printerTitle, gate)
    return if (slot == null) stringResource(R.string.ModelColorSlotsUnknownSlot, gate + 1)
    else stringResource(R.string.ModelColorSlotsKnownSlot, gate + 1, slot.type.orEmpty(),
        String.format(Locale.ROOT, "#%06X", slot.color and COLOR_MASK))
}
