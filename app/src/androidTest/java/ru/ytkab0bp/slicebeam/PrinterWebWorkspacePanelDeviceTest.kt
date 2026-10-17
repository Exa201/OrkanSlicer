package ru.ytkab0bp.slicebeam

import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
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
}
