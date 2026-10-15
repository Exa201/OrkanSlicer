package ru.ytkab0bp.slicebeam.view

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.core.graphics.ColorUtils
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.components.FilamentSlotSheet
import ru.ytkab0bp.slicebeam.components.AcePhysicalSlotsBar
import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.utils.FilamentSlot
import ru.ytkab0bp.slicebeam.utils.MmuFilamentSync
import ru.ytkab0bp.slicebeam.config.TemporaryProjectSettings
import ru.ytkab0bp.slicebeam.config.SlotFilamentProfiles
import ru.ytkab0bp.slicebeam.utils.Prefs

/** Slot-count rules shared by the Compose palette and its regression tests. */
internal object FilamentPalettePolicy {
    fun canAddSlot(slotCount: Int): Boolean = slotCount in 0 until Prefs.MAX_FILAMENT_COLORS

    fun canDeleteSlot(slotCount: Int): Boolean = slotCount > 1

    fun canDeleteAt(index: Int, slotCount: Int): Boolean =
        canDeleteSlot(slotCount) && index in 0 until slotCount
}

private data class PaletteSlotSnapshot(val color: Int, val type: String?)

/** Compose palette controller for printer tools T0–T15 and persistent filament slots. */
class FilamentPaletteController {
    private var slots: MutableList<FilamentSlot> = Prefs.getFilamentSlots()
    private var paletteRevision by mutableIntStateOf(0)

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        val context = LocalContext.current
        val snapshots = remember(paletteRevision) {
            slots.map { PaletteSlotSnapshot(it.color, it.type) }
        }
        val printerTitle = SliceBeam.CONFIG?.presets?.get("printer").orEmpty()
        val syncedGates = remember(paletteRevision, printerTitle) {
            if (Prefs.isMmuSyncedPrinter(printerTitle))
                MmuFilamentSync.getSelectedGates(printerTitle, snapshots.size) else null
        }
        val physicalGates = remember(paletteRevision, printerTitle) {
            MmuFilamentSync.physicalGates(printerTitle)
        }
        Column(modifier = modifier) {
            if (physicalGates.isNotEmpty()) {
                AcePhysicalSlotsBar(physicalGates, Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                Text(stringResource(R.string.AceModelColorsTitle),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
            FilamentPaletteContent(
                slots = snapshots,
                selectedGates = syncedGates,
                onSlotClick = { index -> showSlotEditor(context, index) },
                onAddClick = { showNewSlotEditor(context) },
            )
        }
    }

    fun reload() {
        slots = Prefs.getFilamentSlots()
        paletteRevision++
    }

    private fun showSlotEditor(context: Context, index: Int) {
        val slot = slots.getOrNull(index) ?: return
        FilamentSlotSheet(
            context,
            slot,
            FilamentPalettePolicy.canDeleteSlot(slots.size),
            object : FilamentSlotSheet.Listener {
                override fun onChanged(slot: FilamentSlot) {
                    persistAndReload()
                }

                override fun onDelete() {
                    if (!FilamentPalettePolicy.canDeleteAt(index, slots.size)) return
                    MmuFilamentSync.removeSelectedGate(index, slots.size)
                    if (TemporaryProjectSettings.active) TemporaryProjectSettings.removeFilament(index)
                    else SlotFilamentProfiles.removeSlot(index)
                    slots.removeAt(index)
                    persistAndReload()
                }
            },
        ).show()
    }

    private fun showNewSlotEditor(context: Context) {
        if (!FilamentPalettePolicy.canAddSlot(slots.size)) return
        val slot = FilamentSlot(0xFF1AC5A2.toInt(), "PLA")
        FilamentSlotSheet(
            context,
            slot,
            false,
            object : FilamentSlotSheet.Listener {
                private var added = false

                override fun onChanged(slot: FilamentSlot) {
                    if (!added) {
                        slots.add(slot)
                        added = true
                    }
                    persistAndReload()
                }

                override fun onDelete() = Unit
            },
        ).show()
    }

    private fun persistAndReload() {
        Prefs.setFilamentSlots(slots)
        slots = Prefs.getFilamentSlots()
        paletteRevision++
    }
}

@Composable
private fun FilamentPaletteContent(
    slots: List<PaletteSlotSnapshot>,
    selectedGates: IntArray?,
    modifier: Modifier = Modifier,
    onSlotClick: (Int) -> Unit,
    onAddClick: () -> Unit,
) {
    Row(
        modifier = modifier
            // During adaptive navigation rotation the legacy profile host can briefly measure
            // weighted children with an unbounded width. Bound the viewport before applying
            // horizontal scrolling so Compose never measures this scroller at infinity.
            .widthIn(max = 1200.dp)
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        slots.forEachIndexed { index, slot ->
            FilamentPaletteSlot(
                index = index,
                slot = slot,
                physicalGate = selectedGates?.getOrNull(index),
                onClick = { onSlotClick(index) },
            )
        }
        if (FilamentPalettePolicy.canAddSlot(slots.size)) {
            FilamentPaletteAddButton(onClick = onAddClick)
        }
    }
}

@Composable
private fun FilamentPaletteSlot(
    index: Int,
    slot: PaletteSlotSnapshot,
    physicalGate: Int?,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    val needsOutline = ColorUtils.calculateLuminance(slot.color) > 0.85
    val accessibilityLabel = stringResource(
        R.string.FilamentPaletteSlotDescription,
        index,
        slot.type.orEmpty(),
    )
    Column(
        modifier = Modifier
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = accessibilityLabel
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(shape)
                .background(Color(slot.color))
                .then(
                    if (needsOutline) Modifier.border(1.dp, MaterialTheme.colorScheme.outline, shape)
                    else Modifier
                ),
            contentAlignment = Alignment.Center,
        ) {
            val textColor = if (ColorUtils.calculateLuminance(slot.color) > 0.5) Color.Black else Color.White
            Text(
                text = stringResource(R.string.FilamentToolLabel, index),
                color = textColor,
                style = MaterialTheme.typography.labelLarge,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
        }
        Text(
            text = slot.type?.takeIf { it.isNotBlank() } ?: " ",
            modifier = Modifier.padding(top = 2.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (physicalGate != null) Text(
            text = stringResource(R.string.AcePaletteAssignment, physicalGate + 1),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun FilamentPaletteAddButton(onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    val accessibilityLabel = stringResource(R.string.FilamentPaletteAdd)
    Column(
        modifier = Modifier
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = accessibilityLabel
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(shape)
                .border(2.dp, MaterialTheme.colorScheme.outline, shape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "+",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.headlineSmall,
            )
        }
        Text(
            text = " ",
            modifier = Modifier.padding(top = 2.dp),
            style = MaterialTheme.typography.labelMedium,
            fontSize = 13.sp,
        )
    }
}
