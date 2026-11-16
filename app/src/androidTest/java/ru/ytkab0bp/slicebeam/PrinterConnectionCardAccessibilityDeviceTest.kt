package ru.ytkab0bp.slicebeam

import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.platform.app.InstrumentationRegistry
import ru.ytkab0bp.slicebeam.components.PrinterConnectionCard
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme

@RunWith(AndroidJUnit4::class)
class PrinterConnectionCardAccessibilityDeviceTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun missingHostMessageAndWebActionRemainVisibleAt480DpAnd150Percent() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var opened = 0
        val missingHost = context.getString(R.string.PrinterWebHostMissing)
        val openWebDescription = context.getString(R.string.PrinterOpenWebDescription)

        compose.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(1f, 1.5f),
                LocalConfiguration provides Configuration().apply {
                    screenWidthDp = 480
                    screenHeightDp = 900
                },
            ) {
                BeamComposeTheme {
                    Box(Modifier.width(480.dp)) {
                        PrinterConnectionCard(configuredHost = null, onOpenWeb = { opened++ })
                    }
                }
            }
        }

        compose.onNodeWithText(missingHost, substring = false)
            .assertIsDisplayed()
        compose.onNodeWithContentDescription(openWebDescription)
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        compose.runOnIdle { assertEquals(1, opened) }
    }

    @Test
    fun connectionCardRemainsReadableAcrossTwelveWidthAndFontConfigurations() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var widthDp by mutableIntStateOf(320)
        var fontScale by mutableFloatStateOf(1f)
        val missingHost = context.getString(R.string.PrinterWebHostMissing)
        val openWebDescription = context.getString(R.string.PrinterOpenWebDescription)

        compose.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(1f, fontScale),
                LocalConfiguration provides Configuration().apply {
                    screenWidthDp = widthDp
                    screenHeightDp = 900
                },
            ) {
                BeamComposeTheme {
                    Box(Modifier.width(widthDp.dp)) {
                        PrinterConnectionCard(configuredHost = null, onOpenWeb = {})
                    }
                }
            }
        }

        var configurations = 0
        for (width in listOf(320, 360, 400, 480)) {
            for (scale in listOf(1f, 1.3f, 1.5f)) {
                compose.runOnIdle {
                    widthDp = width
                    fontScale = scale
                }
                compose.waitForIdle()
                compose.onNodeWithText(missingHost, substring = false).assertIsDisplayed()
                compose.onNodeWithContentDescription(openWebDescription)
                    .assertIsDisplayed()
                    .assertHeightIsAtLeast(48.dp)
                configurations++
            }
        }
        assertEquals("All responsive width/font combinations are exercised", 12, configurations)
    }
}
