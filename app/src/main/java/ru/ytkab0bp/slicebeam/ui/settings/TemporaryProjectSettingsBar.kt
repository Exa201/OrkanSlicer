package ru.ytkab0bp.slicebeam.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.config.TemporaryProjectSettings
import ru.ytkab0bp.slicebeam.utils.Prefs

@Composable
fun TemporaryProjectSettingsBar(modifier: Modifier = Modifier, showFilamentSelector: Boolean = false) {
    val project by TemporaryProjectSettings.uiState.collectAsState()
    if (!project.active) return
    Column(modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp)) {
        Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
            Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.ProjectSettingsTemporarySource, project.sourceName),
                    modifier = Modifier.weight(1f), fontSize = 12.sp, lineHeight = 15.sp,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                TextButton(onClick = {
                    TemporaryProjectSettings.clear()
                }) {
                    Text(stringResource(R.string.ProjectSettingsUseOwn), fontSize = 12.sp)
                }
            }
        }
        val imported = buildList {
            if (project.printSettingsImported) add(stringResource(R.string.ProjectSettingsImportPrint))
            if (project.filamentSettingsImported) add(stringResource(R.string.ProjectSettingsImportFilament))
            if (project.printerSettingsImported) add(stringResource(R.string.ProjectSettingsImportPrinter))
        }
        Text(
            text = if (imported.isEmpty()) stringResource(R.string.ProjectSettingsColorsOnly)
                else stringResource(R.string.ProjectSettingsImportedAreas, imported.joinToString(" · ")),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp),
        )
        if (showFilamentSelector) {
            val slots = Prefs.getFilamentSlots()
            Row(Modifier.widthIn(max = 1200.dp).fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                slots.forEachIndexed { index, slot ->
                    FilterChip(
                        selected = index == project.selectedFilament,
                        onClick = { TemporaryProjectSettings.selectFilament(index) },
                        label = { Text("T$index · ${slot.type.orEmpty()}", fontSize = 12.sp) },
                        leadingIcon = { Box(Modifier.size(14.dp).background(Color(slot.color), CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)) },
                    )
                }
            }
        }
    }
}
