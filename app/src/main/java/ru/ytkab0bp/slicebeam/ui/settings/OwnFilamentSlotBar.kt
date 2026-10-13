package ru.ytkab0bp.slicebeam.ui.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.config.SlotFilamentProfiles
import ru.ytkab0bp.slicebeam.utils.Prefs

/** Selects which of the user's own T-slots is being edited in the normal filament screen. */
@Composable
fun OwnFilamentSlotBar(profileTitle: String?, onSelect: (Int) -> Unit) {
    val slots = Prefs.getFilamentSlots()
    var selected by remember { mutableIntStateOf(SlotFilamentProfiles.selectedSlot.coerceIn(0, slots.lastIndex.coerceAtLeast(0))) }
    var helpExpanded by remember { androidx.compose.runtime.mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                slots.forEachIndexed { index, slot ->
                    FilterChip(
                        selected = selected == index,
                        onClick = { selected = index; onSelect(index) },
                        label = { Text("T$index ${slot.type.orEmpty()}", maxLines = 1) },
                    )
                }
            }
            IconButton(onClick = { helpExpanded = !helpExpanded }) {
                Icon(painterResource(R.drawable.info_outline_28), contentDescription = stringResource(R.string.OwnFilamentSlotHelp))
            }
        }
        if (helpExpanded) Text(stringResource(R.string.OwnFilamentSlotHelp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        val material = slots.getOrNull(selected)?.type.orEmpty()
        val profileMaterial = SliceBeam.CONFIG?.findFilament(profileTitle)?.get("filament_type")
            ?.substringBefore(';')?.substringBefore(',').orEmpty()
        if (material.isNotBlank() && profileMaterial.isNotBlank() &&
            !material.equals(profileMaterial, ignoreCase = true)) {
            Text(stringResource(R.string.OwnFilamentMaterialNotice, material, profileMaterial),
                color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
        }
    }
}
