package ru.ytkab0bp.slicebeam.components

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.components.bed_menu.ProfileExportSelectionDialog
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme

@RunWith(AndroidJUnit4::class)
class ProfileExportSelectionDialogDeviceTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun longCategoryListsCanScrollAndSelectionsSurviveBackNavigation() {
        val categories = (0..2).map { category -> (0 until 20).map { "Category $category profile $it" } }
        var completed: List<List<Int>>? = null
        var dismissed = false
        compose.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(1f, 1.5f),
                LocalConfiguration provides Configuration().apply {
                    screenWidthDp = 320
                    screenHeightDp = 640
                },
            ) {
                BeamComposeTheme {
                    ProfileExportSelectionDialog(
                        titles = listOf("Print profiles", "Filament profiles", "Printer profiles"),
                        categories = categories,
                        cancelLabel = "Cancel",
                        backLabel = "Back",
                        nextLabel = "Next",
                        exportLabel = "Export",
                        onDismiss = { dismissed = true },
                        onComplete = { completed = it },
                    )
                }
            }
        }

        compose.onNodeWithText("Print profiles").assertIsDisplayed()
        compose.onNodeWithTag("profile-export-selection-0-0").performClick()
        compose.onNodeWithTag("profile-export-selection-0-0").assertIsOff()
        compose.onNodeWithTag("profile-export-category-list").performScrollToIndex(19)
        compose.onNodeWithText("Category 0 profile 19").assertIsDisplayed()
        compose.onNodeWithText("Next").performClick()
        compose.onNodeWithText("Filament profiles").assertIsDisplayed()
        compose.onNodeWithText("Back").performClick()
        compose.onNodeWithText("Print profiles").assertIsDisplayed()
        compose.onNodeWithTag("profile-export-category-list").performScrollToIndex(0)
        compose.onNodeWithTag("profile-export-selection-0-0").assertIsOff()

        compose.onNodeWithText("Next").performClick()
        compose.onNodeWithTag("profile-export-category-list").performScrollToIndex(0)
        compose.onNodeWithText("Next").performClick()
        compose.onNodeWithText("Printer profiles").assertIsDisplayed()
        compose.onNodeWithTag("profile-export-category-list").performScrollToIndex(0)
        compose.onNodeWithTag("profile-export-selection-2-0").performClick()
        compose.onNodeWithText("Export").performClick()
        compose.runOnIdle {
            assertEquals(listOf((1..19).toList(), (0..19).toList(), (1..19).toList()), completed)
            assertEquals(false, dismissed)
        }
    }

    @Test
    fun systemBackDismissesWithoutCompletingExport() {
        var completed = false
        var dismissed = false
        compose.setContent {
            BeamComposeTheme {
                ProfileExportSelectionDialog(
                    titles = listOf("Print", "Filament", "Printer"),
                    categories = listOf(listOf("Standard"), listOf("PLA"), listOf("Printer")),
                    cancelLabel = "Cancel",
                    backLabel = "Back",
                    nextLabel = "Next",
                    exportLabel = "Export",
                    onDismiss = { dismissed = true },
                    onComplete = { completed = true },
                )
            }
        }

        compose.onNodeWithText("Print").assertIsDisplayed()
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
            .sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(true, dismissed)
            assertEquals(false, completed)
        }
    }
}
