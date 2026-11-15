package ru.ytkab0bp.slicebeam.components.bed_menu

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
        compose.onNodeWithText("Deckel", substring = false).assertIsDisplayed()
        compose.onNodeWithTag("model-objects-search").performTextInput(" unmatched")
        compose.onNodeWithTag("model-objects-search-empty").assertIsDisplayed()
        compose.onNodeWithTag("model-object-row-0").assertDoesNotExist()
    }
}
