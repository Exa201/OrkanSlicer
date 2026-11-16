package ru.ytkab0bp.slicebeam.components.bed_menu

import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
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
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale
import ru.ytkab0bp.slicebeam.R

@RunWith(AndroidJUnit4::class)
class ModelObjectsAdaptiveDialogDeviceTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun objectRowsExposePreviewDimensionsAndPlate() {
        compose.setContent {
            MaterialTheme {
                ModelObjectsDialog(
                    title = "Geladene Objekte",
                    cancelLabel = "Schließen",
                    assignLabel = "Filament zuordnen",
                    rows = listOf(
                        ModelObjectRow(
                            name = "Rahmen",
                            tool = 0,
                            material = "PLA",
                            color = 0xFF25D4F2.toInt(),
                            selected = false,
                            assignEnabled = true,
                            objectIndex = 0,
                            widthMm = 42.0,
                            depthMm = 28.5,
                            heightMm = 9.0,
                            plateNumber = 2,
                            plateCount = 4,
                            previewMesh = floatArrayOf(
                                0f, 0f, 0f, 42f, 0f, 0f, 0f, 28.5f, 9f,
                            ),
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

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val width = String.format(Locale.getDefault(), "%.1f", 42.0)
        val depth = String.format(Locale.getDefault(), "%.1f", 28.5)
        val height = String.format(Locale.getDefault(), "%.1f", 9.0)
        compose.onNodeWithContentDescription(
            context.getString(R.string.ModelObjectPreviewDescription, width, depth, height),
        ).assertIsDisplayed().assertHeightIsAtLeast(112.dp)
        compose.onNodeWithText(
            context.getString(R.string.ModelObjectDimensionsAndPlate, width, depth, height, 2, 4),
        ).assertIsDisplayed()
    }

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
    fun objectActionsRemainScrollableAtTwoHundredPercentText() {
        val longName = "Catan Hafenhaus mit besonders langem Dateinamen und Zusatzinformationen 01.3mf"
        compose.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(1f, 2f),
                LocalConfiguration provides Configuration().apply {
                    screenWidthDp = 480
                    screenHeightDp = 1050
                },
            ) {
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
                        onAssign = {},
                        onAssignPart = { _, _ -> },
                        onTransformPart = { _, _ -> },
                        onEditPart = { _, _ -> },
                        onApplySelection = {},
                    )
                }
            }
        }

        compose.onNodeWithText(longName, substring = false).assertIsDisplayed()
        compose.onNodeWithTag("model-objects-dialog-rows").performTouchInput { swipeUp() }
        compose.onNodeWithText("Filament zuordnen")
            .performScrollTo()
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
        compose.onNodeWithText("Schließen").assertIsDisplayed()
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
        compose.onNodeWithText("Deckel", substring = false).performScrollTo().assertIsDisplayed()
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

        compose.onNodeWithText("Deckel – 長い部品名 für Baugruppe 12", substring = false)
            .performScrollTo().assertIsDisplayed()
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
