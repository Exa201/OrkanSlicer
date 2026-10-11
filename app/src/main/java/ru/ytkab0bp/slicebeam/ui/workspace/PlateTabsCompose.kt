package ru.ytkab0bp.slicebeam.ui.workspace

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.ytkab0bp.slicebeam.R

internal data class PlateCloseDecision(
    val selectedIndex: Int,
    val resetOnlyPlate: Boolean,
)

internal object PlateTabsPolicy {
    fun afterClose(plateCount: Int, selectedIndex: Int, closedIndex: Int): PlateCloseDecision? {
        if (plateCount <= 0 || selectedIndex !in 0 until plateCount || closedIndex !in 0 until plateCount) {
            return null
        }
        if (plateCount == 1) return PlateCloseDecision(selectedIndex = 0, resetOnlyPlate = true)

        val remainingCount = plateCount - 1
        val nextSelectedIndex = when {
            closedIndex == selectedIndex -> (closedIndex - 1).coerceIn(0, remainingCount - 1)
            closedIndex < selectedIndex -> selectedIndex - 1
            else -> selectedIndex
        }
        return PlateCloseDecision(selectedIndex = nextSelectedIndex, resetOnlyPlate = false)
    }
}

@Composable
internal fun PlateTabs(
    plateCount: Int,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onClose: (Int) -> Unit,
    onAdd: () -> Unit,
) {
    val listState = rememberLazyListState()
    val displayedPlateCount = plateCount.coerceAtLeast(0)
    LaunchedEffect(plateCount, selectedIndex) {
        if (displayedPlateCount > 0) listState.animateScrollToItem(selectedIndex.coerceIn(0, displayedPlateCount - 1))
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .width(64.dp)
            .fillMaxHeight(),
        contentPadding = PaddingValues(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(
            count = displayedPlateCount,
            key = { index -> "plate-$index" },
        ) { index ->
            PlateTab(
                index = index,
                selected = index == selectedIndex,
                onSelect = { onSelect(index) },
                onClose = { onClose(index) },
            )
        }
        item(key = "add-plate") {
            AddPlateButton(onClick = onAdd)
        }
    }
}

@Composable
private fun PlateTab(
    index: Int,
    selected: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit,
) {
    val number = (index + 1).toString()
    val label = stringResource(R.string.PlateNumber, number)
    val closeDescription = stringResource(R.string.PlateCloseDescription, label)
    val shape = RoundedCornerShape(8.dp)
    val background = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val foreground = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        shape = shape,
        color = background,
        contentColor = foreground,
    ) {
        Row(
            modifier = Modifier.width(64.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp)
                    .selectable(selected = selected, role = Role.Tab, onClick = onSelect)
                    .semantics { contentDescription = label }
                    .padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = number,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
            if (selected) IconButton(
                onClick = onClose,
                modifier = Modifier
                    .width(30.dp)
                    .defaultMinSize(minHeight = 48.dp)
                    .semantics { contentDescription = closeDescription },
            ) {
                Text(text = "×", fontSize = 18.sp, fontWeight = FontWeight.Normal)
            }
        }
    }
}

@Composable
private fun AddPlateButton(onClick: () -> Unit) {
    val description = stringResource(R.string.MenuAddPlate)
    val shape = RoundedCornerShape(8.dp)
    Surface(
        modifier = Modifier
            .width(64.dp)
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(shape)
            .semantics { contentDescription = description }
            .clickable(role = Role.Button, onClick = onClick),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.primary,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(text = "+", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
