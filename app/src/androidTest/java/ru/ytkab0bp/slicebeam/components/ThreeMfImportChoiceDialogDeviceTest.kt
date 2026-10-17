package ru.ytkab0bp.slicebeam.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class ThreeMfImportChoiceDialogDeviceTest {
    @get:Rule val compose = createComposeRule()

    private data class Choice(
        val printer: String?,
        val useFilePrinter: Boolean,
        val usePrintSettings: Boolean,
        val useFilamentSettings: Boolean,
    )

    @Test fun printSettingsStartUncheckedAndCanBeSelectedAlone() {
        val applied = AtomicReference<Choice?>()
        compose.setContent {
            MaterialTheme {
                ThreeMfImportChoiceDialog(
                    title = "3MF settings",
                    explanation = "Choose what to use",
                    printerLabel = "Printer",
                    printerNames = listOf("Own printer", "File printer"),
                    initialPrinterIndex = 0,
                    filePrinterIndex = 1,
                    filePrinterLabel = "Use file printer",
                    unavailableFilePrinterLabel = "File printer unavailable",
                    printSettingsLabel = "Use print settings",
                    filamentSettingsLabel = "Use filament settings",
                    temporaryNote = "Temporary",
                    applyLabel = "Apply",
                    cancelLabel = "Cancel",
                    canUsePrintSettings = true,
                    canUseFilamentSettings = true,
                    onDismiss = {},
                    onApply = { printer, filePrinter, print, filament ->
                        applied.set(Choice(printer, filePrinter, print, filament))
                    },
                )
            }
        }

        compose.onNodeWithTag("3mf-import-print-settings").assertIsOff().performClick()
        compose.onNodeWithTag("3mf-import-print-settings").assertIsOn()
        compose.onNodeWithTag("3mf-import-filament-settings").assertIsOff()
        compose.onNodeWithTag("3mf-import-file-printer").assertIsOff()
        compose.onNodeWithTag("3mf-import-apply").performClick()

        assertEquals(Choice("Own printer", false, true, false), applied.get())
    }

    @Test fun filamentSettingsCanBeSelectedWithoutPrintOrPrinterSettings() {
        val applied = AtomicReference<Choice?>()
        compose.setContent {
            MaterialTheme {
                ThreeMfImportChoiceDialog(
                    title = "3MF settings",
                    explanation = "Choose what to use",
                    printerLabel = "Printer",
                    printerNames = listOf("Own printer", "File printer"),
                    initialPrinterIndex = 0,
                    filePrinterIndex = 1,
                    filePrinterLabel = "Use file printer",
                    unavailableFilePrinterLabel = "File printer unavailable",
                    printSettingsLabel = "Use print settings",
                    filamentSettingsLabel = "Use filament settings",
                    temporaryNote = "Temporary",
                    applyLabel = "Apply",
                    cancelLabel = "Cancel",
                    canUsePrintSettings = true,
                    canUseFilamentSettings = true,
                    onDismiss = {},
                    onApply = { printer, filePrinter, print, filament ->
                        applied.set(Choice(printer, filePrinter, print, filament))
                    },
                )
            }
        }

        compose.onNodeWithTag("3mf-import-filament-settings").assertIsOff().performClick()
        compose.onNodeWithTag("3mf-import-filament-settings").assertIsOn()
        compose.onNodeWithTag("3mf-import-print-settings").assertIsOff()
        compose.onNodeWithTag("3mf-import-file-printer").assertIsOff()
        compose.onNodeWithTag("3mf-import-apply").performClick()

        assertEquals(Choice("Own printer", false, false, true), applied.get())
    }
}
