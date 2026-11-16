package ru.ytkab0bp.slicebeam.ui.workspace

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme

@RunWith(AndroidJUnit4::class)
class PrinterWebOpenButtonDeviceTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun configuredButUnreachablePrinterShowsOfflineState() {
        compose.setContent {
            BeamComposeTheme {
                Box(Modifier.requiredSize(56.dp).testTag("button-host")) {
                    PrinterWebOpenButton(hostConfigured = true, connected = false, onOpen = {})
                }
            }
        }
        compose.onNodeWithTag("printer-web-connection")
            .assertIsDisplayed()
            .assert(hasStateDescription(InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.PrinterWebDisconnected)))
    }

    @Test
    fun reachablePrinterShowsConnectedState() {
        compose.setContent {
            BeamComposeTheme {
                Box(Modifier.requiredSize(56.dp).testTag("button-host")) {
                    PrinterWebOpenButton(hostConfigured = true, connected = true, onOpen = {})
                }
            }
        }
        compose.onNodeWithTag("printer-web-connection")
            .assertIsDisplayed()
            .assert(hasStateDescription(InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.PrinterWebConnected)))
    }
}
