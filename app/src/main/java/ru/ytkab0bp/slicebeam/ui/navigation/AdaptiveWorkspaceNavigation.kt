package ru.ytkab0bp.slicebeam.ui.navigation

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class WorkspaceNavigationItem(
    val slot: Int,
    val label: String,
    @DrawableRes val icon: Int,
    val accessibilityLabel: String,
)

/** Compact destinations keep their labels and full touch areas in both window shapes. */
@Composable
fun AdaptiveWorkspaceNavigation(
    items: List<WorkspaceNavigationItem>,
    selectedSlot: Int,
    isBottomNavigation: Boolean,
    onSelected: (Int) -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        if (isBottomNavigation) {
            Row(Modifier.fillMaxWidth().selectableGroup()) {
                items.forEach { item ->
                    Destination(item, item.slot == selectedSlot, Modifier.weight(1f), onSelected)
                }
            }
        } else {
            Column(
                Modifier.width(AdaptiveNavigationPolicy.NAVIGATION_RAIL_WIDTH_DP.dp)
                    .heightIn(max = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp)
                    .fillMaxHeight().verticalScroll(rememberScrollState()).selectableGroup(),
            ) {
                items.forEach { item ->
                    Destination(item, item.slot == selectedSlot, Modifier.fillMaxWidth(), onSelected, compactRail = true)
                }
            }
        }
    }
}

@Composable
private fun Destination(
    item: WorkspaceNavigationItem,
    selected: Boolean,
    modifier: Modifier,
    onSelected: (Int) -> Unit,
    compactRail: Boolean = false,
) {
    Column(
        modifier.heightIn(min = AdaptiveNavigationPolicy.NAVIGATION_BAR_HEIGHT_DP.dp)
            .selectable(selected = selected, role = Role.Tab,
                onClick = { if (!selected) onSelected(item.slot) })
            .semantics { contentDescription = item.accessibilityLabel }
            .padding(horizontal = 2.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (selected) MaterialTheme.colorScheme.primary
                else androidx.compose.ui.graphics.Color.Transparent,
        ) {
            Icon(
                painterResource(item.icon), contentDescription = null,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp).size(20.dp),
                tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            item.label, maxLines = 2, softWrap = true, overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center, fontSize = 11.sp, lineHeight = 13.sp,
            modifier = Modifier.fillMaxWidth(),
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}
