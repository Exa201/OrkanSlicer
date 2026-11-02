package ru.ytkab0bp.slicebeam.components.bed_menu

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
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
                    patternOptions = listOf("grid" to "Grid", "gyroid" to "Gyroid"),
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
            mapOf(
                "wall_loops" to "2", "sparse_infill_density" to "35%", "top_shell_layers" to "3",
                "sparse_infill_pattern" to "", "enable_support" to "", "support_type" to "",
            ),
            applied,
        )
    }

    @Test fun objectSupportOverrideHasInheritEnableAndDisableStates() {
        var applied: Map<String, String>? = null
        compose.setContent {
            MaterialTheme {
                ModelObjectProcessOverridesDialog(
                    objectName = "Object A",
                    initialValues = emptyMap(),
                    patternOptions = listOf("grid" to "Grid", "gyroid" to "Gyroid"),
                    onDismiss = {},
                    onApply = { applied = it },
                )
            }
        }

        compose.onNodeWithTag("model-object-process-support").performClick()
        compose.onNodeWithTag("model-object-process-support-1").performClick()
        assertEquals(null, applied)
        compose.onNodeWithTag("model-object-process-settings-apply").performClick()
        assertEquals("1", applied?.get("enable_support"))
    }

    @Test fun supportTypeUsesOrcaChoicesAndAppliesOnlyAfterConfirmation() {
        var applied: Map<String, String>? = null
        compose.setContent {
            MaterialTheme {
                ModelObjectProcessOverridesDialog(
                    objectName = "Object A",
                    initialValues = emptyMap(),
                    supportTypeOptions = listOf("normal(auto)" to "Normal (auto)", "tree(auto)" to "Tree (auto)"),
                    onDismiss = {},
                    onApply = { applied = it },
                )
            }
        }

        compose.onNodeWithTag("model-object-process-support-type").performClick()
        compose.onNodeWithTag("model-object-process-support-type-tree(auto)").performClick()
        assertEquals(null, applied)
        compose.onNodeWithTag("model-object-process-settings-apply").performClick()
        assertEquals("tree(auto)", applied?.get("support_type"))
    }

    @Test fun infillPatternUsesOrcaChoicesAndAppliesOnlyAfterConfirmation() {
        var applied: Map<String, String>? = null
        compose.setContent {
            MaterialTheme {
                ModelObjectProcessOverridesDialog(
                    objectName = "Part A",
                    initialValues = emptyMap(),
                    patternOptions = listOf("grid" to "Grid", "gyroid" to "Gyroid"),
                    onDismiss = {},
                    onApply = { applied = it },
                )
            }
        }

        compose.onNodeWithTag("model-object-process-setting-3").performClick()
        compose.onNodeWithTag("model-object-process-pattern-gyroid").performClick()
        assertEquals(null, applied)
        compose.onNodeWithTag("model-object-process-settings-apply").performClick()
        assertEquals("gyroid", applied?.get("sparse_infill_pattern"))
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

    @Test fun eachMultipartRowExposesAnIndependentProcessSettingsAction() {
        var editedPart: Pair<Int, Int>? = null
        compose.setContent {
            MaterialTheme {
                ModelObjectsDialog(
                    title = "Objects",
                    cancelLabel = "Cancel",
                    assignLabel = "Filament",
                    rows = listOf(
                        ModelObjectRow(
                            name = "Assembly", tool = 0, material = "PLA", color = 0xFF00FF00.toInt(),
                            selected = true, assignEnabled = true,
                            parts = listOf(
                                ModelObjectPartRow("Part 1", 0, "PLA", 0xFF00FF00.toInt()),
                                ModelObjectPartRow("Part 2", 1, "PETG", 0xFF0000FF.toInt()),
                            ),
                        ),
                    ),
                    onDismiss = {},
                    onSelect = {},
                    onAssign = {},
                    onAssignPart = { _, _ -> },
                    onEditPartProcessSettings = { objectIndex, partIndex -> editedPart = objectIndex to partIndex },
                    onTransformPart = { _, _ -> },
                    onEditPart = { _, _ -> },
                    onApplySelection = {},
                )
            }
        }
        compose.onNodeWithTag("model-objects-dialog-rows")
            .performScrollToNode(hasTestTag("model-object-part-process-settings-0-1"))
        compose.onNodeWithTag("model-object-part-process-settings-0-1").assertIsDisplayed().performClick()
        assertEquals(0 to 1, editedPart)
    }
}
