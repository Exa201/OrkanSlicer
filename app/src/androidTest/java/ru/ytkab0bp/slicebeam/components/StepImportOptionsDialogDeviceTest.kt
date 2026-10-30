package ru.ytkab0bp.slicebeam.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.slic3r.StepImportOptions
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class StepImportOptionsDialogDeviceTest {
    @get:Rule val compose = createComposeRule()

    @Test fun exposesMeshingOptionsAndAppliesOnlyAfterConfirmation() {
        val applied = AtomicReference<Pair<StepImportOptions, Boolean>?>(null)
        compose.setContent {
            MaterialTheme {
                StepImportOptionsDialog(
                    initial = StepImportOptions(),
                    onDismiss = {},
                    onImport = { options, save -> applied.set(options to save) },
                )
            }
        }

        compose.onNodeWithTag("step-split-compound").performScrollTo().assertIsOff().performClick()
        compose.onNodeWithTag("step-split-compound").assertIsOn()
        compose.onNodeWithTag("step-save-defaults").performScrollTo().assertIsOff().performClick()
        compose.onNodeWithTag("step-save-defaults").assertIsOn()
        assertEquals(null, applied.get())
        compose.onNodeWithTag("step-import-confirm").performClick()

        val result = requireNotNull(applied.get())
        assertEquals(StepImportOptions(0.003, 0.5, splitCompound = true), result.first)
        assertEquals(true, result.second)
    }

    @Test fun cancellingNeverStartsImport() {
        var dismissed = false
        var imported = false
        compose.setContent {
            MaterialTheme {
                StepImportOptionsDialog(
                    initial = StepImportOptions(),
                    onDismiss = { dismissed = true },
                    onImport = { _, _ -> imported = true },
                )
            }
        }
        compose.onNodeWithTag("step-import-cancel").performClick()
        assertEquals(true, dismissed)
        assertFalse(imported)
    }
}
