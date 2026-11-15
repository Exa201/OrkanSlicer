package ru.ytkab0bp.slicebeam.components.bed_menu

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ModelObjectsAdaptiveDialogDeviceTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun longNamesAndActionsRemainVisibleAtLargeFontScale() {
        val longName = "Catan Hafenhaus mit besonders langem Dateinamen und Zusatzinformationen 01.3mf"
        var assigned = -1
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1.5f)) {
                MaterialTheme {
                    ModelObjectsDialog(
                        title = "Geladene Objekte",
                        cancelLabel = "Schließen",
                        assignLabel = "Filament zuordnen",
                        rows = listOf(
                            ModelObjectRow(
                                name = longName,
                                tool = 0,
                                material = "PETG",
                                color = 0xFF25D4F2.toInt(),
                                selected = true,
                                assignEnabled = true,
                                objectIndex = 7,
                            ),
                        ),
                        onDismiss = {},
                        onSelect = {},
                        onAssign = { assigned = it },
                        onAssignPart = { _, _ -> },
                        onTransformPart = { _, _ -> },
                        onEditPart = { _, _ -> },
                        onApplySelection = {},
                    )
                }
            }
        }

        compose.onNodeWithText(longName, substring = false).assertIsDisplayed()
        compose.onNodeWithTag("model-object-row-0").assertIsDisplayed().assertHasClickAction()
        compose.onNodeWithText("Filament zuordnen").assertIsDisplayed().assertHeightIsAtLeast(48.dp).performClick()
        compose.runOnIdle { assertEquals(7, assigned) }
    }

    @Test
    fun searchFindsObjectByPartMaterialAndShowsEmptyState() {
        compose.setContent {
            MaterialTheme {
                ModelObjectsDialog(
                    title = "Geladene Objekte",
                    cancelLabel = "Schließen",
                    assignLabel = "Filament zuordnen",
                    rows = listOf(
                        ModelObjectRow(
                            name = "Gehäuse",
                            tool = 0,
                            material = "PLA",
                            color = 0xFF25D4F2.toInt(),
                            selected = false,
                            assignEnabled = true,
                            parts = listOf(
                                ModelObjectPartRow("Deckel", 1, "PETG Weiß", 0xFFFFFFFF.toInt()),
                                ModelObjectPartRow("Innensteg", 0, "PLA", 0xFF25D4F2.toInt()),
                            ),
                            objectIndex = 4,
                        ),
                    ),
                    onDismiss = {},
                    onSelect = {},
                    onAssign = {},
                    onAssignPart = { _, _ -> },
                    onTransformPart = { _, _ -> },
                    onEditPart = { _, _ -> },
                    onApplySelection = {},
                )
            }
        }

        compose.onNodeWithTag("model-objects-search").performTextInput("PETG Weiß")
        compose.onNodeWithTag("model-object-row-0").assertIsDisplayed()
        compose.onNodeWithTag("model-objects-group-selection-toggle").performClick()
        compose.onNodeWithTag("model-objects-dialog-rows").performTouchInput { swipeUp() }
        compose.onNodeWithText("Deckel", substring = false).assertIsDisplayed()
        compose.onNodeWithTag("model-objects-search").performTextInput(" unmatched")
        compose.onNodeWithTag("model-objects-search-empty").assertIsDisplayed()
        compose.onNodeWithTag("model-object-row-0").assertDoesNotExist()
    }

    @Test
    fun groupSelectionExposesCheckboxStateAndAppliesSelectedObjects() {
        var applied: IntArray = intArrayOf()
        compose.setContent {
            MaterialTheme {
                ModelObjectsDialog(
                    title = "Geladene Objekte",
                    cancelLabel = "Schließen",
                    assignLabel = "Filament zuordnen",
                    rows = listOf(
                        ModelObjectRow(
                            name = "Gehäuse",
                            tool = 0,
                            material = "PLA",
                            color = 0xFF25D4F2.toInt(),
                            selected = false,
                            assignEnabled = true,
                            objectIndex = 4,
                        ),
                    ),
                    onDismiss = {},
                    onSelect = {},
                    onAssign = {},
                    onAssignPart = { _, _ -> },
                    onTransformPart = { _, _ -> },
                    onEditPart = { _, _ -> },
                    onApplySelection = { applied = it },
                )
            }
        }

        compose.onNodeWithTag("model-objects-group-selection-toggle").performClick()
        compose.onNodeWithTag("model-object-row-0").assertIsOff().performClick().assertIsOn()
        compose.onNodeWithTag("model-objects-selection-confirm").performClick()
        compose.runOnIdle { assertEquals(listOf(4), applied.toList()) }
    }

    @Test
    fun multipartActionsStayReachableFromAccessibleOverflowMenu() {
        val invokedActions = mutableListOf<String>()
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1.5f)) {
                MaterialTheme {
                    ModelObjectsDialog(
                    title = "Geladene Objekte",
                    cancelLabel = "Schließen",
                    assignLabel = "Filament zuordnen",
                    rows = listOf(
                        ModelObjectRow(
                            name = "Baugruppe",
                            tool = 0,
                            material = "PLA",
                            color = 0xFF000000.toInt(),
                            selected = false,
                            assignEnabled = true,
                            parts = listOf(
                                ModelObjectPartRow("Deckel – 長い部品名 für Baugruppe 12", 1, "PETG Weiß", 0xFFFFFFFF.toInt()),
                                ModelObjectPartRow("Boden", 0, "PLA", 0xFF000000.toInt()),
                            ),
                            objectIndex = 12,
                        ),
                    ),
                    onDismiss = {},
                    onSelect = {},
                    onAssign = {},
                    onAssignPart = { objectIndex, partIndex -> invokedActions += "filament:$objectIndex:$partIndex" },
                    onEditPartProcessSettings = { objectIndex, partIndex -> invokedActions += "settings:$objectIndex:$partIndex" },
                    onTransformPart = { objectIndex, partIndex -> invokedActions += "transform:$objectIndex:$partIndex" },
                    onEditPart = { objectIndex, partIndex -> invokedActions += "separate:$objectIndex:$partIndex" },
                    onApplySelection = {},
                    )
                }
            }
        }

        compose.onNodeWithText("Deckel – 長い部品名 für Baugruppe 12", substring = false).assertIsDisplayed()
        compose.onNodeWithContentDescription("PETG Weiß, T1, #FFFFFF", substring = false).assertExists()
        val actions = listOf(
            "model-object-assign-part-0-0" to "filament:12:0",
            "model-object-part-process-settings-0-0" to "settings:12:0",
            "model-object-transform-part-0-0" to "transform:12:0",
            "model-object-edit-part-0-0" to "separate:12:0",
        )
        actions.forEach { (actionTag, expected) ->
            compose.onNodeWithTag("model-object-part-actions-0-0").assertHeightIsAtLeast(48.dp).performClick()
            compose.onNodeWithTag(actionTag).assertIsDisplayed().assertHeightIsAtLeast(48.dp).performClick()
            compose.runOnIdle { assertEquals(expected, invokedActions.last()) }
        }
    }
}
