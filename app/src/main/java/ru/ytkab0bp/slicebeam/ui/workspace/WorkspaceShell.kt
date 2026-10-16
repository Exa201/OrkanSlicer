package ru.ytkab0bp.slicebeam.ui.workspace

import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import ru.ytkab0bp.slicebeam.ui.OrkanUiTokens
import androidx.compose.material3.MaterialTheme
import ru.ytkab0bp.slicebeam.ui.navigation.AdaptiveNavigationPolicy
import ru.ytkab0bp.slicebeam.ui.navigation.WorkspaceToolPanelPolicy

/** Compose owns the workspace geometry; legacy views are mounted only at explicit boundaries. */
@Composable
internal fun WorkspaceShell(
    portrait: Boolean,
    toolPanelExpanded: Boolean,
    toolPanelNeedsTwoRows: Boolean,
    navigationWidthDp: Int,
    menuWidthDp: Int,
    rendererView: View,
    navigationView: View,
    menuView: View,
    projectSettingsView: View,
    profileStatusView: View,
    filamentButtonView: View,
    platePickerView: View,
    printerWebButtonView: View,
) {
    val toolPanelHeightDp = WorkspaceToolPanelPolicy.menuHeightDp(
        toolPanelNeedsTwoRows,
        LocalDensity.current.fontScale,
    )
    val background = MaterialTheme.colorScheme.background
    val header: @Composable (Modifier) -> Unit = { modifier ->
        Row(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = OrkanUiTokens.touchTarget)
                .padding(horizontal = 6.dp, vertical = 4.dp),
        ) {
            AndroidView(
                factory = { profileStatusView },
                modifier = Modifier.weight(1f).widthIn(min = 0.dp).heightIn(min = OrkanUiTokens.touchTarget),
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(OrkanUiTokens.compactGap),
            ) {
                WorkspaceActionControl(filamentButtonView)
                WorkspaceActionControl(platePickerView)
                WorkspaceActionControl(printerWebButtonView)
            }
        }
    }

    if (portrait) {
        Column(Modifier.fillMaxSize().background(background)) {
            AndroidView(
                factory = { projectSettingsView },
                modifier = Modifier.fillMaxWidth().heightIn(min = 0.dp),
            )
            header(Modifier)
            AndroidView(
                factory = { rendererView },
                modifier = Modifier.fillMaxWidth().weight(1f).heightIn(min = 0.dp),
            )
            if (toolPanelExpanded) {
                AndroidView(
                    factory = { menuView },
                    modifier = Modifier.fillMaxWidth().height(toolPanelHeightDp.coerceAtLeast(0).dp),
                    update = { it.visibility = View.VISIBLE },
                )
                Spacer(Modifier.fillMaxWidth().height(1.dp).background(background.copy(alpha = 0.35f)))
            }
            AndroidView(
                factory = { navigationView },
                modifier = Modifier.fillMaxWidth().heightIn(min = AdaptiveNavigationPolicy.NAVIGATION_BAR_HEIGHT_DP.dp),
            )
        }
    } else {
        Row(Modifier.fillMaxSize().background(background)) {
            AndroidView(
                factory = { navigationView },
                modifier = Modifier.width(navigationWidthDp.dp).fillMaxHeight(),
            )
            Spacer(Modifier.width(1.dp).fillMaxHeight().background(background.copy(alpha = 0.35f)))
            AndroidView(
                factory = { menuView },
                modifier = Modifier.width(menuWidthDp.dp).fillMaxHeight(),
            )
            Column(Modifier.weight(1f).fillMaxHeight()) {
                AndroidView(
                    factory = { projectSettingsView },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 0.dp),
                )
                header(Modifier)
                AndroidView(
                    factory = { rendererView },
                    modifier = Modifier.fillMaxSize().weight(1f),
                )
            }
        }
    }
}

@Composable
private fun WorkspaceActionControl(view: View) {
    AndroidView(
        factory = { view },
        modifier = Modifier.width(OrkanUiTokens.touchTarget).height(OrkanUiTokens.touchTarget),
    )
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
