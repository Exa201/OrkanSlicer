package ru.ytkab0bp.slicebeam.components.bed_menu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.slic3r.GCodePreviewPolicy

@Composable
internal fun SliceLayerPartControls(
    range: SliceLayerRange,
    size: Int,
    onPart: (Int) -> Unit,
    onSize: (Int) -> Unit,
    onAll: () -> Unit,
) {
    val count = GCodePreviewPolicy.blockCount(range.maximum, size)
    val current = GCodePreviewPolicy.blockAt(range.from, range.maximum, size)
    val expected = GCodePreviewPolicy.blockRange(current, range.maximum, size)
    val isPart = range.from == expected.from && range.to == expected.to
    var choosingPart by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.GCodePreviewOnly), fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onPart(current - 1) }, enabled = current > 1) {
                Icon(painterResource(R.drawable.arrow_left_outline_28), stringResource(R.string.GCodePreviewPreviousPart))
            }
            OutlinedButton(onClick = { choosingPart = true }, modifier = Modifier.weight(1f)) {
                Text(if (isPart) stringResource(R.string.GCodePreviewPartFormat, current, count, range.from, range.to)
                    else stringResource(R.string.GCodePreviewCustomRange, range.from, range.to), fontSize = 14.sp)
            }
            IconButton(onClick = { onPart(current + 1) }, enabled = current < count) {
                Icon(painterResource(R.drawable.arrow_left_outline_28), stringResource(R.string.GCodePreviewNextPart),
                    modifier = Modifier.graphicsLayer(rotationZ = 180f))
            }
        }
        Text(stringResource(R.string.GCodePreviewPartSize), fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(25, 50, 100, 200).forEach { block ->
                FilterChip(selected = block == size, onClick = { onSize(block) }, label = { Text(block.toString()) })
            }
            AssistChip(onClick = onAll, label = { Text(stringResource(R.string.GCodePreviewAll)) })
        }
    }
    if (choosingPart) {
        AlertDialog(
            onDismissRequest = { choosingPart = false },
            title = { Text(stringResource(R.string.GCodePreviewChoosePart)) },
            text = {
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 320.dp)) {
                    items(count) { index ->
                        val part = index + 1
                        val selection = GCodePreviewPolicy.blockRange(part, range.maximum, size)
                        Row(Modifier.fillMaxWidth().clickable { onPart(part); choosingPart = false }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = isPart && part == current, onClick = { onPart(part); choosingPart = false })
                            Text(stringResource(R.string.GCodePreviewPartLabel, part, selection.from, selection.to), fontSize = 16.sp)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { choosingPart = false }) { Text(stringResource(android.R.string.cancel)) } },
        )
    }
}
