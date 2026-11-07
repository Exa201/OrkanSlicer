package ru.ytkab0bp.slicebeam

import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import ru.ytkab0bp.slicebeam.ui.workspace.PrinterWebErrorPanel
import ru.ytkab0bp.slicebeam.ui.workspace.PrinterWebWorkspacePanel

@RunWith(AndroidJUnit4::class)
class PrinterWebWorkspacePanelDeviceTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun technicalPrinterViewIsAttachedOnlyWhileComposeWorkspacePanelIsVisible() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val panelView = FrameLayout(context)
        val visible = mutableStateOf(true)

        compose.setContent {
            Box(Modifier.fillMaxSize()) {
                if (visible.value) {
                    BeamComposeTheme {
                        PrinterWebWorkspacePanel(
                            panelView = panelView,
                            loading = true,
                            errorDescription = null,
                            onRetry = {},
                            onOpenInBrowser = {},
                            onClose = { visible.value = false },
                        )
                    }
                }
            }
        }

        compose.runOnIdle { assertNotNull("The active Compose panel hosts its Android view", panelView.parent) }
        compose.runOnIdle { visible.value = false }
        compose.waitForIdle()
        compose.runOnIdle { assertNull("Closing the panel detaches the Android view", panelView.parent) }
    }

    @Test
    fun composePanelProvidesErrorRetryExternalOpenAndCloseActions() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val panelView = FrameLayout(context)
        var retries = 0
        var externalOpens = 0
        var closes = 0

        compose.setContent {
            BeamComposeTheme {
                PrinterWebWorkspacePanel(
                    panelView = panelView,
                    loading = false,
                    errorDescription = "Printer page unavailable",
                    onRetry = { retries++ },
                    onOpenInBrowser = { externalOpens++ },
                    onClose = { closes++ },
                )
            }
        }

        compose.onNodeWithText("Printer page unavailable").assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.PrinterWebRetry)).performClick()
        compose.onNodeWithText(context.getString(R.string.PrinterWebOpenInBrowser)).performClick()
        compose.onNodeWithContentDescription(context.getString(R.string.ClosePanel)).performClick()
        compose.runOnIdle {
            assertEquals(1, retries)
            assertEquals(1, externalOpens)
            assertEquals(1, closes)
        }
    }

    @Test
    fun errorActionsRemainReachableOnNarrowScreensAndLargeFonts() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val retryLabel = context.getString(R.string.PrinterWebRetry)
        val browserLabel = context.getString(R.string.PrinterWebOpenInBrowser)
        val configurations = listOf(320 to 1f, 360 to 1.3f, 320 to 1.5f)
        var widthDp by mutableIntStateOf(configurations.first().first)
        var fontScale by mutableFloatStateOf(configurations.first().second)

        compose.setContent {
            val currentDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(currentDensity.density, fontScale),
            ) {
                Box(Modifier.width(widthDp.dp).height(480.dp).testTag("printer-web-error-root")) {
                    BeamComposeTheme {
                        PrinterWebErrorPanel(
                            description = "Printer page unavailable",
                            onRetry = {},
                            onOpenInBrowser = {},
                        )
                    }
                }
            }
        }

        configurations.forEach { (testWidth, testFontScale) ->
            compose.runOnIdle {
                widthDp = testWidth
                fontScale = testFontScale
            }
            compose.waitForIdle()
            val root = compose.onNodeWithTag("printer-web-error-root").fetchSemanticsNode().boundsInRoot
            listOf(retryLabel, browserLabel).forEach { label ->
                val actionNode = compose.onNodeWithText(label).assertIsDisplayed()
                    .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
                val action = actionNode.fetchSemanticsNode().boundsInRoot
                assertTrue("$label must remain within the $widthDp dp panel", action.left >= root.left)
                assertTrue("$label must remain within the $widthDp dp panel", action.right <= root.right)
                assertTrue("$label must have visible height", action.height > 0f)
            }
        }
    }
}
