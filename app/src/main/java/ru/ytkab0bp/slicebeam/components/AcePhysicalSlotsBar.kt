package ru.ytkab0bp.slicebeam.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.utils.MmuFilamentSync

/** Printer-side gate positions stay visible, in physical order, without horizontal scrolling. */
@Composable
fun AcePhysicalSlotsBar(gates: List<MmuFilamentSync.Companion.PhysicalGate>, modifier: Modifier = Modifier) {
    if (gates.isEmpty()) return
    val largeFont = LocalDensity.current.fontScale >= 1.3f
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            stringResource(R.string.AcePhysicalSlotsTitle),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
        )
        gates.chunked(PHYSICAL_SLOTS_PER_ROW).forEach { rowGates ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Top,
            ) {
                rowGates.forEach { gate ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                    ) {
                        Text(
                            when {
                                gate.printerTool < 0 -> stringResource(R.string.AcePhysicalSlotOnly, gate.index + 1)
                                largeFont -> stringResource(
                                    R.string.AcePhysicalSlotCompactTool, gate.index + 1, gate.printerTool,
                                )
                                else -> stringResource(
                                    R.string.AcePhysicalSlotTool, gate.index + 1, gate.printerTool,
                                )
                            },
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            gate.filament?.let { filament ->
                                Box(
                                    Modifier.size(11.dp)
                                        .background(Color(filament.color or 0xFF000000.toInt()), CircleShape)
                                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                                )
                            }
                            Text(
                                when {
                                    !gate.occupied -> stringResource(R.string.AcePhysicalSlotEmpty)
                                    gate.filament == null -> stringResource(R.string.AcePhysicalSlotIncomplete)
                                    else -> gate.filament.type.orEmpty()
                                },
                                color = if (gate.occupied && gate.filament == null) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                if (rowGates.size < PHYSICAL_SLOTS_PER_ROW) {
                    repeat(PHYSICAL_SLOTS_PER_ROW - rowGates.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

private const val PHYSICAL_SLOTS_PER_ROW = 4
