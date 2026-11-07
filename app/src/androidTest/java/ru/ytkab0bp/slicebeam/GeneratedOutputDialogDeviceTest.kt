package ru.ytkab0bp.slicebeam

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.components.GeneratedModelOutputDialog
import ru.ytkab0bp.slicebeam.components.GeneratedPairOutputDialog
import ru.ytkab0bp.slicebeam.components.GeneratedRasterPreviewDialog
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme

@RunWith(AndroidJUnit4::class)
class GeneratedOutputDialogDeviceTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun generatedModelUseCallsOnlyUseAction() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val visible = mutableStateOf(true)
        var action = ""
        compose.setContent {
            BeamComposeTheme {
                if (visible.value) GeneratedModelOutputDialog(
                    message = "converted.stl",
                    onDismiss = { visible.value = false },
                    onDiscard = { action = "discard"; visible.value = false },
                    onSave = { action = "save"; visible.value = false },
                    onUse = { action = "use"; visible.value = false },
                )
            }
        }

        compose.onNodeWithText("converted.stl").assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.GeneratedOutputUse)).performClick()
        compose.runOnIdle {
            assertEquals("use", action)
            assertFalse(visible.value)
        }
    }

    @Test
    fun generatedPairCanChooseOnlyAccessoryOrReturnToPairActions() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val visible = mutableStateOf(true)
        var selected = ""
        compose.setContent {
            BeamComposeTheme {
                if (visible.value) GeneratedPairOutputDialog(
                    onDismiss = { visible.value = false },
                    onLoadModelOnly = { selected = "model"; visible.value = false },
                    onLoadBoth = { selected = "both"; visible.value = false },
                    onSaveModel = { selected = "save-model"; visible.value = false },
                    onSaveAccessory = { selected = "save-accessory"; visible.value = false },
                    onSaveBoth = { selected = "save-both"; visible.value = false },
                )
            }
        }

        compose.onNodeWithText(context.getString(R.string.ConverterOnlySave)).performClick()
        compose.onNodeWithText(context.getString(R.string.LithophaneSaveAccessoryOnly)).performClick()
        compose.runOnIdle {
            assertEquals("save-accessory", selected)
            assertFalse(visible.value)
        }
    }

    @Test
    fun generatedPairCanLoadBothParts() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val visible = mutableStateOf(true)
        var loadBoth = false
        compose.setContent {
            BeamComposeTheme {
                if (visible.value) GeneratedPairOutputDialog(
                    onDismiss = { visible.value = false },
                    onLoadModelOnly = {},
                    onLoadBoth = { loadBoth = true; visible.value = false },
                    onSaveModel = {},
                    onSaveAccessory = {},
                    onSaveBoth = {},
                )
            }
        }

        compose.onNodeWithText(context.getString(R.string.ConverterLoad)).performClick()
        compose.runOnIdle {
            assertTrue(loadBoth)
            assertFalse(visible.value)
        }
    }

    @Test
    fun rasterPreviewSaveDismissesAndReleasesBitmap() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        val visible = mutableStateOf(true)
        var saved = false
        compose.setContent {
            BeamComposeTheme {
                if (visible.value) GeneratedRasterPreviewDialog(
                    bitmap = bitmap,
                    onDismiss = { visible.value = false },
                    onDiscard = { visible.value = false },
                    onSave = { saved = true; visible.value = false },
                )
            }
        }

        compose.onNodeWithText(context.getString(R.string.ConverterOutputPreview)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.ConverterSaveNow)).performClick()
        compose.waitForIdle()
        compose.runOnIdle {
            assertTrue(saved)
            assertFalse(visible.value)
            assertTrue(bitmap.isRecycled)
        }
    }
}
