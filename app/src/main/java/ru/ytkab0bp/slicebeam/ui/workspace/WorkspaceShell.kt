package ru.ytkab0bp.slicebeam.ui.workspace

import android.view.View
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import ru.ytkab0bp.slicebeam.ui.OrkanUiTokens
import androidx.compose.material3.MaterialTheme
import ru.ytkab0bp.slicebeam.ui.navigation.AdaptiveNavigationPolicy
import ru.ytkab0bp.slicebeam.ui.navigation.AdaptiveWorkspaceNavigation
import ru.ytkab0bp.slicebeam.ui.navigation.WorkspaceDestination
import ru.ytkab0bp.slicebeam.ui.navigation.WorkspaceNavigationItem
import ru.ytkab0bp.slicebeam.ui.navigation.WorkspaceToolPanelPolicy
import kotlinx.coroutines.delay
import androidx.compose.animation.core.tween

/** Compose owns the workspace geometry; legacy views are mounted only at explicit boundaries. */
@Composable
internal fun WorkspaceShell(
    portrait: Boolean,
    toolPanelExpanded: Boolean,
    toolPanelNeedsTwoRows: Boolean,
    navigationItemCount: Int,
    menuWidthDp: Int,
    rendererView: View,
    selectedNavigationSlot: Int,
    toolsExpanded: Boolean,
    onNavigationSelected: (Int) -> Unit,
    activeMenuSlot: Int,
    menuComposeContentForSlot: @Composable (Int) -> Unit,
    onMenuTransitionSettled: () -> Unit,
    projectSettingsContent: @Composable () -> Unit,
    profileStatusContent: @Composable () -> Unit,
    filamentButtonContent: @Composable () -> Unit,
    platePickerContent: @Composable () -> Unit,
    printerWebButtonContent: @Composable () -> Unit,
    modePanelContent: (@Composable () -> Unit)?,
    modePanelFraction: Float?,
    onModePanelDismiss: () -> Unit,
    transientPanelContent: (@Composable () -> Unit)?,
) {
    val toolPanelHeightDp = WorkspaceToolPanelPolicy.menuHeightDp(
        toolPanelNeedsTwoRows,
        LocalDensity.current.fontScale,
    )
    val navigationHeightDp = AdaptiveNavigationPolicy.navigationBarHeightDp(
        LocalDensity.current.fontScale,
        navigationItemCount,
    )
    val background = MaterialTheme.colorScheme.background
    val twoRowNavigation = AdaptiveNavigationPolicy.usesTwoRowBottomNavigation(
        LocalDensity.current.fontScale,
        WorkspaceDestination.values().size,
    )
    val compactLabels = !portrait || (
        AdaptiveNavigationPolicy.usesCompactBottomLabels(
            LocalConfiguration.current.screenWidthDp,
            LocalDensity.current.fontScale,
        ) && !twoRowNavigation
    )
    val navigationItems = WorkspaceDestination.values().map { destination ->
        val compactTitle = when (destination) {
            WorkspaceDestination.PROJECT -> ru.ytkab0bp.slicebeam.R.string.MenuFileCompact
            WorkspaceDestination.MODEL -> ru.ytkab0bp.slicebeam.R.string.MenuModelCompact
            WorkspaceDestination.EDIT -> ru.ytkab0bp.slicebeam.R.string.MenuToolbarNavigationCompact
            WorkspaceDestination.TOOLS -> ru.ytkab0bp.slicebeam.R.string.MenuOptionsCompact
            else -> destination.navigationTitleRes
        }
        WorkspaceNavigationItem(
            slot = destination.ordinal,
            label = stringResource(if (compactLabels) compactTitle else destination.navigationTitleRes),
            icon = destination.iconRes,
            accessibilityLabel = stringResource(destination.navigationTitleRes),
            compactLabel = if (compactLabels) stringResource(compactTitle) else null,
        )
    }
    val header: @Composable (Modifier) -> Unit = { modifier ->
        Row(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = OrkanUiTokens.touchTarget)
                .padding(horizontal = 6.dp, vertical = 4.dp),
        ) {
            Box(
                modifier = Modifier.weight(1f).widthIn(min = 0.dp).heightIn(min = OrkanUiTokens.touchTarget),
            ) { profileStatusContent() }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(OrkanUiTokens.compactGap),
            ) {
                WorkspaceActionControl(filamentButtonContent)
                WorkspaceActionControl(platePickerContent)
                WorkspaceActionControl(printerWebButtonContent)
            }
        }
    }

    Box(Modifier.fillMaxSize().background(background)) {
      if (portrait) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().heightIn(min = 0.dp)) { projectSettingsContent() }
            header(Modifier)
            AndroidView(
                factory = { rendererView },
                modifier = Modifier.fillMaxWidth().weight(1f).heightIn(min = 0.dp),
            )
            if (toolPanelExpanded) {
                WorkspaceMenuHost(
                    slot = activeMenuSlot,
                    composeContentForSlot = menuComposeContentForSlot,
                    onTransitionSettled = onMenuTransitionSettled,
                    modifier = Modifier.fillMaxWidth().height(toolPanelHeightDp.coerceAtLeast(0).dp),
                )
                Spacer(Modifier.fillMaxWidth().height(1.dp).background(background.copy(alpha = 0.35f)))
            }
            AdaptiveWorkspaceNavigation(
                modifier = Modifier.fillMaxWidth().height(navigationHeightDp.dp),
                items = navigationItems,
                selectedSlot = selectedNavigationSlot,
                isBottomNavigation = true,
                toolsExpanded = toolsExpanded,
                panelOpenDescription = stringResource(ru.ytkab0bp.slicebeam.R.string.WorkspaceToolsOpen),
                panelClosedDescription = stringResource(ru.ytkab0bp.slicebeam.R.string.WorkspaceToolsClosed),
                announcePanelState = true,
                showPanelToggleIndicator = true,
                onSelected = onNavigationSelected,
            )
        }
      } else {
        Row(Modifier.fillMaxSize()) {
            AdaptiveWorkspaceNavigation(
                modifier = Modifier.width(AdaptiveNavigationPolicy.navigationRailWidthDp(LocalDensity.current.fontScale).dp).fillMaxHeight(),
                items = navigationItems,
                selectedSlot = selectedNavigationSlot,
                isBottomNavigation = false,
                toolsExpanded = true,
                panelOpenDescription = stringResource(ru.ytkab0bp.slicebeam.R.string.WorkspaceToolsOpen),
                panelClosedDescription = stringResource(ru.ytkab0bp.slicebeam.R.string.WorkspaceToolsClosed),
                announcePanelState = true,
                showPanelToggleIndicator = false,
                onSelected = onNavigationSelected,
            )
            Spacer(Modifier.width(1.dp).fillMaxHeight().background(background.copy(alpha = 0.35f)))
            WorkspaceMenuHost(
                slot = activeMenuSlot,
                composeContentForSlot = menuComposeContentForSlot,
                onTransitionSettled = onMenuTransitionSettled,
                modifier = Modifier.width(menuWidthDp.dp).fillMaxHeight(),
            )
            Column(Modifier.weight(1f).fillMaxHeight()) {
                Box(Modifier.fillMaxWidth().heightIn(min = 0.dp)) { projectSettingsContent() }
                header(Modifier)
                AndroidView(
                    factory = { rendererView },
                    modifier = Modifier.fillMaxSize().weight(1f),
                )
            }
        }
      }
      if (modePanelContent != null) {
          WorkspaceModePanelHost(
              onDismiss = onModePanelDismiss,
              panelFraction = modePanelFraction,
              content = modePanelContent,
          )
      }
      transientPanelContent?.let { panelContent ->
          // Replacing one transient dialog with another must dispose the old Compose
          // Dialog window instead of reusing its back callback for the new content.
          key(panelContent) { panelContent() }
      }
    }
}

@Composable
private fun WorkspaceMenuHost(
    slot: Int,
    composeContentForSlot: @Composable (Int) -> Unit,
    onTransitionSettled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = slot,
        modifier = modifier,
        transitionSpec = {
            val direction = if (targetState > initialState) 1 else -1
            (slideInHorizontally(tween(220)) { direction * it / 3 } + fadeIn(tween(180))) togetherWith
                (slideOutHorizontally(tween(220)) { -direction * it / 3 } + fadeOut(tween(160)))
        },
        label = "workspace-menu-route",
    ) { targetSlot ->
        key(targetSlot) {
            composeContentForSlot(targetSlot)
        }
    }
    LaunchedEffect(slot) {
        // Keep BedFragment's action guard aligned with the Compose route transition.
        delay(260)
        onTransitionSettled()
    }
}

@Composable
private fun WorkspaceActionControl(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.size(OrkanUiTokens.touchTarget),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) { content() }
}

/** Measurable policy for the portion of a compact workspace reserved for the actual build plate. */
internal object WorkspaceShellLayoutPolicy {
    /** availableHeightDp is the content height after the activity's system-bar insets. */
    @JvmStatic
    fun plateViewportHeightDp(
        availableHeightDp: Int,
        projectBarHeightDp: Int,
        workspaceHeaderHeightDp: Int,
        navigationHeightDp: Int,
        toolPanelHeightDp: Int,
        toolPanelExpanded: Boolean,
    ): Int {
        val reserved = projectBarHeightDp.coerceAtLeast(0) +
            workspaceHeaderHeightDp.coerceAtLeast(0) +
            navigationHeightDp.coerceAtLeast(0) +
            if (toolPanelExpanded) toolPanelHeightDp.coerceAtLeast(0) else 0
        return (availableHeightDp.coerceAtLeast(0) - reserved).coerceAtLeast(0)
    }
}
