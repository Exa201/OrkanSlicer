package ru.ytkab0bp.slicebeam

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import ru.ytkab0bp.slicebeam.ui.settings.ComposeProfileEnumOption
import java.util.function.Consumer

/** Keeps enum choices and the dismiss action reachable on a narrow, large-text workspace. */
@RunWith(AndroidJUnit4::class)
class ProfileEnumOptionAccessibilityDeviceTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun largeEnumDialogCanScrollToLastChoiceAndStillDismissWithoutApplying() {
        val labels = arrayOf(
            "Smooth cold bed plate",
            "Technical plate",
            "Smooth plate",
            "High temperature plate",
            "Textured PEI plate",
            "Textured cold bed plate",
            "Cool plate (SuperTack)",
        )
        val values = arrayOf("cold_smooth", "engineering", "smooth", "high_temp", "textured_pei", "cold_textured", "super_tack")
        var dialogVisible by mutableStateOf(true)
        val changes = mutableListOf<String>()

        compose.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(1f, 1.5f),
                LocalConfiguration provides Configuration().apply {
                    this.screenWidthDp = 320
                    this.screenHeightDp = 640
                },
            ) {
                BeamComposeTheme {
                    if (dialogVisible) {
                        ComposeProfileEnumOption.Content(
                            title = "Bed surface",
                            initialValue = "high_temp",
                            labels = labels,
                            values = values,
                            onChanged = Consumer { value -> changes.add(value) },
                            onLongClick = Runnable {},
                        )
                    }
                }
            }
        }

        compose.onNodeWithText("Bed surface").assertIsDisplayed().performClick()
        compose.onNodeWithText("Cool plate (SuperTack)").performScrollTo().assertIsDisplayed()
        val cancelLabel = InstrumentationRegistry.getInstrumentation().targetContext
            .getString(android.R.string.cancel)
        compose.onNodeWithText(cancelLabel).assertIsDisplayed().performClick()
        compose.waitForIdle()

        assertEquals("Closing an enum list does not apply an unselected value", emptyList<String>(), changes)
        compose.onAllNodesWithText("Bed surface").assertCountEquals(1)
    }
}
