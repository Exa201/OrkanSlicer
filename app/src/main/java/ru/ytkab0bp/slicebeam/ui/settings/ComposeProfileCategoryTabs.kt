package ru.ytkab0bp.slicebeam.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProfileCategoryTabs(labels: List<String>, selectedIndex: Int, onSelected: (Int) -> Unit) {
    if (labels.isEmpty()) return
    // Ordinary phone widths use one scrollable row; large accessibility text can wrap.
    val stacked = ru.ytkab0bp.slicebeam.ui.LocalBeamLayoutFontScale.current >= 1.35f
    if (stacked) {
        Column(
            modifier = Modifier.widthIn(max = 1200.dp).fillMaxWidth(),
        ) {
            labels.chunked(2).forEachIndexed { rowIndex, rowLabels ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    rowLabels.forEachIndexed { columnIndex, label ->
                        val index = rowIndex * 2 + columnIndex
                        ProfileCategoryTab(
                            index = index,
                            label = label,
                            selected = selectedIndex == index,
                            modifier = Modifier.weight(1f),
                            onSelected = onSelected,
                        )
                    }
                    if (rowLabels.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
        return
    }

    PrimaryScrollableTabRow(
        selectedTabIndex = selectedIndex,
        // The host is a legacy LinearLayout that temporarily measures weighted children with
        // unbounded width during a landscape reflow. Cap the Compose viewport before making it
        // fill its parent so the horizontal scroller always receives finite constraints.
        modifier = Modifier.widthIn(max = 1200.dp).fillMaxWidth(),
        edgePadding = 6.dp,
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.primary,
        divider = {},
    ) {
        labels.forEachIndexed { index, label ->
            ProfileCategoryTab(
                index = index,
                label = label,
                selected = selectedIndex == index,
                onSelected = onSelected,
            )
        }
    }
}

@Composable
private fun ProfileCategoryTab(
    index: Int,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onSelected: (Int) -> Unit,
) {
    Tab(
        selected = selected,
        onClick = { onSelected(index) },
        modifier = modifier,
        text = {
            Text(
                text = label,
                maxLines = 2,
                softWrap = true,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 13.sp,
                lineHeight = 17.sp,
            )
        },
    )
}
