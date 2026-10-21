package ru.ytkab0bp.slicebeam

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.fragment.SettingsContent
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme

/** Checks real app-settings accordion semantics and interaction at narrow, large-text bounds. */
@RunWith(AndroidJUnit4::class)
class SettingsAccessibilityDeviceTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun interfaceSectionAnnouncesExpansionAndKeepsSettingsActionsReachable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val expanded = mutableStateOf(emptySet<Int>())
        val screenWidthDp = mutableIntStateOf(320)
        val screenHeightDp = mutableIntStateOf(640)
        val fontScale = mutableFloatStateOf(1.5f)
        var themeActionCount = 0

        compose.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(1f, fontScale.floatValue),
                LocalConfiguration provides Configuration().apply {
                    this.screenWidthDp = screenWidthDp.intValue
                    this.screenHeightDp = screenHeightDp.intValue
                },
            ) {
                BeamComposeTheme {
                    SettingsContent(
                        expandedSections = expanded.value,
                        cloudAvailable = false,
                        boostyAvailable = false,
                        themeName = "Dark",
                        accentColor = "#FF123456",
                        uiTextScale = "150%",
                        converterRoot = "Default",
                        cameraProjection = "Orthographic",
                        cameraControlMode = "Rotate",
                        onToggleSection = { index ->
                            expanded.value = if (index in expanded.value) expanded.value - index else expanded.value + index
                        },
                        onMaker = {},
                        onTheme = { themeActionCount++ },
                        onAccent = {},
                        onUiTextScale = {},
                        onConverterFolder = {},
                        onCameraProjection = {},
                        onCameraControlMode = {},
                        onCameraPreset = {},
                        onCloud = {},
                        onAddPrinter = {},
                        onAbout = {},
                        onBoosty = {},
                        onReset = {},
                    )
                }
            }
        }

        val sectionTitle = context.getString(R.string.SettingsInterface)
        val expand = context.getString(R.string.MakerExpand)
        val collapse = context.getString(R.string.MakerCollapse)
        val section = compose.onNodeWithText(sectionTitle)
        section.assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, expand))
            .performClick()

        compose.runOnIdle { assertEquals(setOf(0), expanded.value) }
        compose.onNodeWithText(sectionTitle)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, collapse))
        compose.onNodeWithText(context.getString(R.string.SettingsInterfaceTheme))
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        compose.runOnIdle { assertEquals(1, themeActionCount) }

        compose.onNodeWithText(sectionTitle).performClick()
        compose.runOnIdle { assertEquals(emptySet<Int>(), expanded.value) }

        val configurations = listOf(
            Triple(320, 640, 1.0f),
            Triple(320, 640, 1.3f),
            Triple(320, 640, 1.5f),
            Triple(360, 800, 1.0f),
            Triple(360, 800, 1.3f),
            Triple(360, 800, 1.5f),
            Triple(400, 850, 1.0f),
            Triple(400, 850, 1.3f),
            Triple(400, 850, 1.5f),
            Triple(640, 320, 1.0f),
            Triple(640, 320, 1.3f),
            Triple(640, 320, 1.5f),
        )
        configurations.forEach { (width, height, scale) ->
            compose.runOnIdle {
                screenWidthDp.intValue = width
                screenHeightDp.intValue = height
                fontScale.floatValue = scale
            }
            compose.waitForIdle()
            compose.onNodeWithText(sectionTitle)
                .assertIsDisplayed()
                .assertHeightIsAtLeast(48.dp)
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, expand))
        }
    }
}
