package ru.ytkab0bp.slicebeam.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
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

    @Test fun everyEmbeddedSettingsCombinationStartsCleanAndReturnsOnlySelectedValues() {
        val applied = AtomicReference<Choice?>()
        val session = mutableIntStateOf(0)
        compose.setContent {
            MaterialTheme {
                key(session.intValue) {
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
        }

        for (mask in 0 until 8) {
            compose.runOnIdle { session.intValue = mask + 1 }
            val useFilePrinter = mask and 1 != 0
            val usePrintSettings = mask and 2 != 0
            val useFilamentSettings = mask and 4 != 0
            if (useFilePrinter) compose.onNodeWithTag("3mf-import-file-printer").performClick()
            if (usePrintSettings) compose.onNodeWithTag("3mf-import-print-settings").performClick()
            if (useFilamentSettings) compose.onNodeWithTag("3mf-import-filament-settings").performClick()

            compose.onNodeWithTag("3mf-import-file-printer").let {
                if (useFilePrinter) it.assertIsOn() else it.assertIsOff()
            }
            compose.onNodeWithTag("3mf-import-print-settings").let {
                if (usePrintSettings) it.assertIsOn() else it.assertIsOff()
            }
            compose.onNodeWithTag("3mf-import-filament-settings").let {
                if (useFilamentSettings) it.assertIsOn() else it.assertIsOff()
            }
            compose.onNodeWithTag("3mf-import-apply").performClick()
            assertEquals(
                "Only the selected embedded settings should be applied for mask $mask",
                Choice(if (useFilePrinter) "File printer" else "Own printer", useFilePrinter,
                    usePrintSettings, useFilamentSettings),
                applied.get(),
            )
        }
    }
}
