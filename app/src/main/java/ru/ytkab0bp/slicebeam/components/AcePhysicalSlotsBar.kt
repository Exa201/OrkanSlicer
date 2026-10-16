package ru.ytkab0bp.slicebeam.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.utils.MmuFilamentSync

/** Printer-side gate positions stay visible even when a gate has no filament. */
@Composable
fun AcePhysicalSlotsBar(gates: List<MmuFilamentSync.Companion.PhysicalGate>, modifier: Modifier = Modifier) {
    if (gates.isEmpty()) return
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.AcePhysicalSlotsTitle), color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp)
        val scrollState = rememberScrollState()
        val coroutineScope = rememberCoroutineScope()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier.weight(1f).horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                gates.forEach { gate ->
                    val foreground = MaterialTheme.colorScheme.onSurface
                    Column(
                        modifier = Modifier
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .padding(horizontal = 9.dp, vertical = 6.dp),
                    ) {
                        Text(
                            if (gate.printerTool >= 0) stringResource(R.string.AcePhysicalSlotTool,
                                gate.index + 1, gate.printerTool)
                            else stringResource(R.string.AcePhysicalSlotOnly, gate.index + 1),
                            color = foreground, fontSize = 12.sp,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            gate.filament?.let { filament ->
                                Box(Modifier.size(11.dp)
                                    .background(Color(filament.color or 0xFF000000.toInt()), CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape))
                            }
                            Text(
                                when {
                                    !gate.occupied -> stringResource(R.string.AcePhysicalSlotEmpty)
                                    gate.filament == null -> stringResource(R.string.AcePhysicalSlotIncomplete)
                                    else -> gate.filament.type.orEmpty()
                                },
                                color = if (gate.occupied && gate.filament == null) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            }
            if (scrollState.maxValue > 0) {
                IconButton(
                    onClick = { coroutineScope.launch { scrollState.animateScrollTo(scrollState.maxValue) } },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.dropdown_24),
                        contentDescription = stringResource(R.string.AcePhysicalSlotsScrollForward),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp).rotate(-90f),
                    )
                }
            }
        }
    }
}
