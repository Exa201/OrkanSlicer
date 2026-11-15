package ru.ytkab0bp.slicebeam

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.components.PrinterConnectionCard
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme

@RunWith(AndroidJUnit4::class)
class PrinterProfileSummaryDeviceTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun printerConnectionCardShowsOnlyConfiguredTechnicalValues() {
        compose.setContent {
            BeamComposeTheme {
                PrinterConnectionCard(
                    configuredHost = "http://192.168.1.24",
                    machineModel = "Kobra 3 V2",
                    nozzleDiameter = "0.4",
                    maxPrintHeight = "260",
                    onOpenWeb = {},
                )
            }
        }

        compose.onNodeWithText("Kobra 3 V2 · Ø 0.4 mm nozzle · Z 260 mm").assertIsDisplayed()
        compose.onNodeWithText("http://192.168.1.24", substring = true).assertIsDisplayed()
    }
}
