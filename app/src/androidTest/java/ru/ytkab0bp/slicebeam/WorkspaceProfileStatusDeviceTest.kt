package ru.ytkab0bp.slicebeam

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import ru.ytkab0bp.slicebeam.ui.workspace.WorkspaceProfileStatus

@RunWith(AndroidJUnit4::class)
class WorkspaceProfileStatusDeviceTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun longPrinterNameStaysInCompactAccessibleStatusAtNarrowLargeText() {
        val accessibilityLabel = "Anycubic Kobra 3 Combo printer profile, 0.4 mm nozzle"
        compose.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(1f, 1.5f),
                LocalConfiguration provides Configuration().apply {
                    screenWidthDp = 320
                    screenHeightDp = 640
                },
            ) {
                BeamComposeTheme {
                    WorkspaceProfileStatus(
                        title = "Anycubic Kobra 3 Combo",
                        summary = "0.4 mm nozzle · PLA · Standard",
                        accessibilityDescription = accessibilityLabel,
                        onOpen = {},
                    )
                }
            }
        }

        compose.onNodeWithContentDescription(accessibilityLabel)
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, androidx.compose.ui.semantics.Role.Button))
        val titleNode = compose.onNodeWithTag("workspace-profile-title", useUnmergedTree = true)
        titleNode.assertIsDisplayed()
        val titleHeightPx = titleNode.fetchSemanticsNode().boundsInRoot.height
        val oneLineAtLargeTextPx = with(Density(1f, 1.5f)) { 30.dp.toPx() }
        assertTrue("Printer title expanded beyond one large-text line", titleHeightPx <= oneLineAtLargeTextPx)
    }
}
