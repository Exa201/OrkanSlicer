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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import ru.ytkab0bp.slicebeam.R
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    toolsExpanded: Boolean = true,
    panelOpenDescription: String = "Tools open",
    panelClosedDescription: String = "Tools closed",
    announcePanelState: Boolean = false,
    showPanelToggleIndicator: Boolean = false,
    onSelected: (Int) -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        if (isBottomNavigation) {
            Row(Modifier.fillMaxWidth().selectableGroup()) {
                items.forEach { item ->
                    Destination(item, item.slot == selectedSlot, Modifier.weight(1f), onSelected,
                        bottomLabel = item.compactLabel ?: item.label,
                        panelOpen = toolsExpanded, panelOpenDescription = panelOpenDescription,
                        panelClosedDescription = panelClosedDescription, announcePanelState = announcePanelState,
                        showPanelToggleIndicator = showPanelToggleIndicator)
                }
            }
        } else {
            Column(
                Modifier.width(AdaptiveNavigationPolicy.navigationRailWidthDp(LocalDensity.current.fontScale).dp)
                    .heightIn(max = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp)
                    .fillMaxHeight().verticalScroll(rememberScrollState()).selectableGroup(),
            ) {
                items.forEach { item ->
                    Destination(item, item.slot == selectedSlot, Modifier.fillMaxWidth(), onSelected,
                        compactRail = true, panelOpen = true, panelOpenDescription = panelOpenDescription,
                        panelClosedDescription = panelClosedDescription, announcePanelState = announcePanelState,
                        showPanelToggleIndicator = false)
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
    bottomLabel: String = item.label,
    panelOpen: Boolean = true,
    panelOpenDescription: String,
    panelClosedDescription: String,
    announcePanelState: Boolean,
    showPanelToggleIndicator: Boolean,
) {
    Column(
        modifier.heightIn(min = AdaptiveNavigationPolicy.NAVIGATION_BAR_HEIGHT_DP.dp)
            .selectable(selected = selected, role = Role.Tab,
                onClick = { onSelected(item.slot) })
            .semantics {
                contentDescription = item.accessibilityLabel
                if (selected && announcePanelState) {
                    stateDescription = if (panelOpen) panelOpenDescription else panelClosedDescription
                }
            }
            .padding(horizontal = 2.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
    ) {
        Surface(
            shape = RoundedCornerShape(OrkanUiTokens.compactCornerRadius),
            color = if (selected) MaterialTheme.colorScheme.primary
                else androidx.compose.ui.graphics.Color.Transparent,
        ) {
            Box(Modifier.padding(horizontal = 10.dp, vertical = 3.dp).size(OrkanUiTokens.navigationIconSize)) {
                Icon(
                    painterResource(item.icon), contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (selected && showPanelToggleIndicator) {
                    Icon(
                        painter = painterResource(R.drawable.dropdown_24),
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.BottomEnd).size(12.dp)
                            .rotate(if (panelOpen) 180f else 0f),
                        tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Text(
            if (compactRail) item.compactLabel ?: item.label else bottomLabel,
            maxLines = 2,
            softWrap = true, overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center, fontSize = OrkanUiTokens.navigationLabelSize,
            modifier = Modifier.fillMaxWidth(),
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}
