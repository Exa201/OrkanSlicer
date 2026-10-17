package ru.ytkab0bp.slicebeam.ui.workspace

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class WorkspaceModePanelHostDeviceTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun portraitToolPanelUsesComposeDpConstraints() {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.requiredSize(360.dp, 720.dp)) {
                    WorkspaceModePanelHost(
                        onDismiss = {},
                        panelSize = WorkspacePanelSize(
                            portrait = WorkspacePanelDimension.Fixed(316f),
                            landscape = WorkspacePanelDimension.Fraction(0.5f),
                        ),
                    ) { Box(Modifier.fillMaxSize()) }
                }
            }
        }

        compose.onNodeWithTag("workspace-mode-panel").assertHeightIsEqualTo(316.dp)
    }

    @Test
    fun landscapeToolPanelUsesComposeAvailableWidth() {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.requiredSize(720.dp, 360.dp)) {
                    WorkspaceModePanelHost(
                        onDismiss = {},
                        panelSize = WorkspacePanelSize(
                            portrait = WorkspacePanelDimension.Fixed(316f),
                            landscape = WorkspacePanelDimension.Fraction(0.5f),
                        ),
                    ) { Box(Modifier.fillMaxSize()) }
                }
            }
        }

        compose.onNodeWithTag("workspace-mode-panel").assertWidthIsEqualTo(360.dp)
    }
}
