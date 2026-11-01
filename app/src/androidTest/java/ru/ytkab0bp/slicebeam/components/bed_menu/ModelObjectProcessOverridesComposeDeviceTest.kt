package ru.ytkab0bp.slicebeam.components.bed_menu

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ModelObjectProcessOverridesComposeDeviceTest {
    @get:Rule val compose = createComposeRule()

    @Test fun validatesValuesAndAppliesOnlyAfterConfirmation() {
        var applied: Map<String, String>? = null
        compose.setContent {
            MaterialTheme {
                ModelObjectProcessOverridesDialog(
                    objectName = "Part A",
                    initialValues = emptyMap(),
                    onDismiss = {},
                    onApply = { applied = it },
                )
            }
        }

        compose.onNodeWithTag("model-object-process-setting-0").performTextInput("2")
        compose.onNodeWithTag("model-object-process-setting-1").performTextInput("35")
        compose.onNodeWithTag("model-object-process-setting-2").performTextInput("3")
        assertEquals(null, applied)
        compose.onNodeWithTag("model-object-process-settings-apply").assertIsEnabled().performClick()
        assertEquals(
            mapOf("wall_loops" to "2", "sparse_infill_density" to "35%", "top_shell_layers" to "3"),
            applied,
        )
    }

    @Test fun invalidValuesBlockSavingAndMixedPartsAreIdentified() {
        compose.setContent {
            MaterialTheme {
                ModelObjectProcessOverridesDialog(
                    objectName = "Grouped part",
                    initialValues = mapOf("wall_loops" to "<mixed>"),
                    onDismiss = {},
                    onApply = {},
                )
            }
        }

        compose.onNodeWithTag("model-object-process-setting-mixed-0", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithTag("model-object-process-setting-0").performTextInput("21")
        compose.onNodeWithTag("model-object-process-settings-error").assertIsDisplayed()
        compose.onNodeWithTag("model-object-process-settings-apply").assertIsNotEnabled()
    }

    @Test fun cancelDoesNotApplyAnyObjectSettings() {
        var dismissed = false
        var applied = false
        compose.setContent {
            MaterialTheme {
                ModelObjectProcessOverridesDialog(
                    objectName = "Part B",
                    initialValues = emptyMap(),
                    onDismiss = { dismissed = true },
                    onApply = { applied = true },
                )
            }
        }

        compose.onNodeWithTag("model-object-process-settings-cancel").performClick()
        assertEquals(true, dismissed)
        assertEquals(false, applied)
    }
}
