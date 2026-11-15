package ru.ytkab0bp.slicebeam

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.ui.platform.testTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference
import androidx.compose.ui.text.TextLayoutResult
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import ru.ytkab0bp.slicebeam.ui.workspace.WorkspaceProfileStatus
import ru.ytkab0bp.slicebeam.ui.workspace.WorkspaceShellLayoutPolicy

@RunWith(AndroidJUnit4::class)
class WorkspaceProfileStatusDeviceTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun longPrinterNameStaysInCompactAccessibleStatusAtNarrowLargeText() {
        val accessibilityLabel = "Anycubic Kobra 3 Combo printer profile, 0.4 mm nozzle"
        val titleLayout = AtomicReference<TextLayoutResult?>()
        val summaryLayout = AtomicReference<TextLayoutResult?>()
        compose.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(1f, 1.5f),
                LocalConfiguration provides Configuration().apply {
                    screenWidthDp = 320
                    screenHeightDp = 640
                },
            ) {
                BeamComposeTheme {
                    Box(Modifier.width(180.dp).heightIn(min = WorkspaceShellLayoutPolicy.workspaceHeaderHeightDp(1.5f).dp - 8.dp).testTag("profile-status-viewport")) {
                        WorkspaceProfileStatus(
                            title = "Anycubic Kobra 3 Combo printer profile",
                            summary = "0.4 mm nozzle · PLA · Standard profile",
                            accessibilityDescription = accessibilityLabel,
                            onOpen = {},
                            onTitleTextLayout = { titleLayout.set(it) },
                            onSummaryTextLayout = { summaryLayout.set(it) },
                        )
                    }
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
        assertTrue("Long printer title should wrap into multiple lines", titleHeightPx > 0f)
        titleNode.assert(SemanticsMatcher.expectValue(SemanticsProperties.Text,
            listOf(androidx.compose.ui.text.AnnotatedString("Anycubic Kobra 3 Combo printer profile"))))
        val viewportBottom = compose.onNodeWithTag("profile-status-viewport").fetchSemanticsNode().boundsInRoot.bottom
        assertTrue("Printer title must remain inside the profile header after wrapping",
            titleNode.fetchSemanticsNode().boundsInRoot.bottom <= viewportBottom)
        val summaryNode = compose.onNodeWithTag("workspace-profile-summary", useUnmergedTree = true)
        summaryNode.assertIsDisplayed()
        summaryNode.assert(SemanticsMatcher.expectValue(SemanticsProperties.Text,
            listOf(androidx.compose.ui.text.AnnotatedString("0.4 mm nozzle · PLA · Standard profile"))))
        assertFalse("Printer name must wrap without visual overflow", titleLayout.get()?.didOverflowHeight ?: true)
        assertFalse("Profile summary must wrap without visual overflow", summaryLayout.get()?.didOverflowHeight ?: true)
        assertTrue("Profile summary should fit inside the adaptive workspace header",
            summaryNode.fetchSemanticsNode().boundsInRoot.bottom <= viewportBottom)
    }
}
