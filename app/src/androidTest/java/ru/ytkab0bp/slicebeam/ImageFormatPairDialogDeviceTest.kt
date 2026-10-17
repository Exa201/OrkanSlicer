package ru.ytkab0bp.slicebeam

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.components.ImageFormatPairDialog
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme

@RunWith(AndroidJUnit4::class)
class ImageFormatPairDialogDeviceTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun selectedFormatsAreReturnedWhenChoosingSource() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val visible = mutableStateOf(true)
        var selectedSource = -1
        var selectedTarget = -1
        compose.setContent {
            BeamComposeTheme {
                if (visible.value) ImageFormatPairDialog(
                    onDismiss = { visible.value = false },
                    onChooseSource = { source, target ->
                        selectedSource = source
                        selectedTarget = target
                        visible.value = false
                    },
                )
            }
        }

        compose.onNodeWithTag("image-source-format").assertIsDisplayed()
        compose.onNodeWithTag("image-source-format").performClick()
        compose.onNodeWithText(context.resources.getStringArray(R.array.ConverterSources)[1]).performClick()
        compose.onNodeWithTag("image-target-format").performClick()
        compose.onNodeWithText(context.resources.getStringArray(R.array.ConverterTargets)[4]).performClick()
        compose.onNodeWithText(context.getString(R.string.ConverterChooseSourceFile)).performClick()

        compose.runOnIdle {
            assertEquals(1, selectedSource)
            assertEquals(4, selectedTarget)
            assertFalse(visible.value)
        }
    }

    @Test
    fun androidBackCancelsWithoutSelectingFormats() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val visible = mutableStateOf(true)
        var selected = false
        compose.setContent {
            BeamComposeTheme {
                if (visible.value) ImageFormatPairDialog(
                    onDismiss = { visible.value = false },
                    onChooseSource = { _, _ -> selected = true },
                )
            }
        }

        compose.onNodeWithText(context.getString(R.string.ConverterTitle)).assertIsDisplayed()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        compose.runOnIdle {
            assertFalse(visible.value)
            assertFalse(selected)
        }
    }
}
