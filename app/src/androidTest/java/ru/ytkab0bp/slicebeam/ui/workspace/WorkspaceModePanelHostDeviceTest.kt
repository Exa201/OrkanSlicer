package ru.ytkab0bp.slicebeam.ui.workspace

import android.content.res.Configuration
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme

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

    @Test
    fun largeFontPlateCounterGrowsWithoutStretchingAdjacentHeaderControls() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(1f, 1.5f),
                LocalConfiguration provides Configuration().apply {
                    screenWidthDp = 360
                    screenHeightDp = 800
                },
            ) {
                BeamComposeTheme {
                    Row {
                        WorkspaceActionControl(content = {
                            PlateTabs(
                                openState = androidx.compose.runtime.mutableStateOf(false),
                                plateCount = 1,
                                selectedIndex = 0,
                                objectCounts = listOf(0),
                                thumbnails = listOf(PlateThumbnail(emptyList())),
                                onOpen = {}, onDismiss = {}, onSelect = {}, onClose = {}, onAdd = {},
                            )
                        }, allowVerticalGrowth = true)
                        WorkspaceActionControl(content = {
                            Box(Modifier.requiredSize(48.dp).testTag("fixed-header-action"))
                        })
                    }
                }
            }
        }

        compose.onNodeWithContentDescription(context.getString(R.string.PlateNumber, "1"))
            .assertHeightIsAtLeast(52.dp)
        compose.onNodeWithTag("fixed-header-action").assertHeightIsEqualTo(48.dp)
    }
}
