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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.ytkab0bp.slicebeam.ui.OrkanUiTokens

data class WorkspaceNavigationItem(
    val slot: Int,
    val label: String,
    @DrawableRes val icon: Int,
    val accessibilityLabel: String,
    val compactLabel: String? = null,
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
    // Large system text still grows, but a small adjustment keeps short German labels whole.
    val largeText = LocalDensity.current.fontScale >= 1.2f
    val labelSize = when {
        compactRail && largeText -> 10.sp
        largeText -> 11.sp
        else -> OrkanUiTokens.navigationLabelSize
    }
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
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp).size(OrkanUiTokens.navigationIconSize),
                tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            if (compactRail) item.compactLabel ?: item.label else item.label,
            maxLines = if (compactRail) 1 else 2,
            softWrap = !compactRail, overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center, fontSize = labelSize, lineHeight = 14.sp,
            modifier = Modifier.fillMaxWidth(),
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}
