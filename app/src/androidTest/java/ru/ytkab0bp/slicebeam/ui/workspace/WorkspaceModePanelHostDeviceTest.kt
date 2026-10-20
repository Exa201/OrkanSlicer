package ru.ytkab0bp.slicebeam.ui.workspace

import android.content.res.Configuration
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
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
    fun landscapeToolPanelCapsHeightWhenAndroidParentMeasuresUnbounded() {
        compose.setContent {
            CompositionLocalProvider(
                LocalConfiguration provides Configuration().apply {
                    screenWidthDp = 720
                    screenHeightDp = 360
                },
            ) {
                MaterialTheme {
                    Column(
                        Modifier.requiredSize(720.dp, 360.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        WorkspaceModePanelHost(
                            onDismiss = {},
                            panelSize = WorkspacePanelSize(
                                portrait = WorkspacePanelDimension.Fixed(300f),
                                landscape = WorkspacePanelDimension.Fixed(300f),
                            ),
                        ) {
                            Column(
                                Modifier.fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                            ) {
                                repeat(12) { Text("Cut setting $it") }
                                Text("Cut apply action", Modifier.testTag("cut-apply-action"))
                            }
                        }
                    }
                }
            }
        }

        compose.onNodeWithTag("workspace-mode-panel").assertIsDisplayed()
        compose.onNodeWithTag("cut-apply-action").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun portraitToolPanelKeepsItsLastActionScrollableAtSmallWidthsAndLargeText() {
        var widthDp by mutableIntStateOf(320)
        var heightDp by mutableIntStateOf(640)
        var fontScale by mutableFloatStateOf(1f)

        compose.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(1f, fontScale),
                LocalConfiguration provides Configuration().apply {
                    screenWidthDp = widthDp
                    screenHeightDp = heightDp
                },
            ) {
                MaterialTheme {
                    Box(Modifier.requiredSize(widthDp.dp, heightDp.dp)) {
                        WorkspaceModePanelHost(
                            onDismiss = {},
                            panelSize = WorkspacePanelSize(
                                portrait = WorkspacePanelDimension.Fixed(heightDp * 0.6f),
                                landscape = WorkspacePanelDimension.Fixed(widthDp * 0.6f),
                            ),
                        ) {
                            Column(
                                Modifier.fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .testTag("tool-panel-scroll"),
                            ) {
                                Text("Tool panel heading")
                                repeat(18) { Text("Tool setting $it") }
                                Text("Last tool action")
                            }
                        }
                    }
                }
            }
        }

        val configurations = listOf(
            Triple(320, 640, 1f), Triple(320, 640, 1.3f), Triple(320, 640, 1.5f),
            Triple(360, 720, 1f), Triple(360, 720, 1.3f), Triple(360, 720, 1.5f),
            Triple(400, 800, 1f), Triple(400, 800, 1.3f), Triple(400, 800, 1.5f),
        )
        configurations.forEach { (width, height, scale) ->
            compose.runOnUiThread {
                widthDp = width
                heightDp = height
                fontScale = scale
            }
            compose.waitForIdle()
            compose.onNodeWithTag("workspace-mode-panel")
                .assertIsDisplayed()
                .assertHeightIsAtLeast(48.dp)
            compose.onNodeWithText("Last tool action")
                .performScrollTo()
                .assertIsDisplayed()
        }
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
