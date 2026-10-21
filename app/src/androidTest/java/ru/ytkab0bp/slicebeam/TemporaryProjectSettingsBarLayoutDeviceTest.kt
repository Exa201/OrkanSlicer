package ru.ytkab0bp.slicebeam.ui.settings

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.config.ThreeMfProjectSettings
import ru.ytkab0bp.slicebeam.config.TemporaryProjectSettings
import ru.ytkab0bp.slicebeam.ui.LocalBeamLayoutFontScale

@RunWith(AndroidJUnit4::class)
class TemporaryProjectSettingsBarLayoutDeviceTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun activeProjectBannerAndWorkspaceReservationStayStableAcrossPhoneSizesAndAppZoom() {
        TemporaryProjectSettings.activate(
            ThreeMfProjectSettings(
                sourceName = "responsive-layout-fixture.3mf",
                process = mapOf("layer_height" to "0.2"),
                filaments = emptyList(),
                filamentTypes = emptyMap(),
                printSettingsImported = false,
                filamentSettingsImported = false,
                printerSettingsImported = false,
            ),
        )
        val widthDp = mutableIntStateOf(320)
        val heightDp = mutableIntStateOf(640)
        val systemFontScale = mutableFloatStateOf(1f)
        val appTextScale = mutableFloatStateOf(0.9f)
        try {
            compose.setContent {
                val configuration = Configuration().apply {
                    screenWidthDp = widthDp.intValue
                    screenHeightDp = heightDp.intValue
                    orientation = if (widthDp.intValue >= heightDp.intValue) {
                        Configuration.ORIENTATION_LANDSCAPE
                    } else Configuration.ORIENTATION_PORTRAIT
                }
                CompositionLocalProvider(
                    LocalConfiguration provides configuration,
                    LocalDensity provides Density(1f, systemFontScale.floatValue * appTextScale.floatValue),
                    LocalBeamLayoutFontScale provides systemFontScale.floatValue,
                ) {
                    MaterialTheme {
                        Box(Modifier.width(widthDp.intValue.dp)) {
                            TemporaryProjectSettingsBar()
                        }
                    }
                }
            }

            listOf(320 to 640, 360 to 800, 400 to 850, 640 to 320, 800 to 400).forEach { (width, height) ->
                widthDp.intValue = width
                heightDp.intValue = height
                listOf(1f, 1.3f, 1.5f).forEach { systemScale ->
                    systemFontScale.floatValue = systemScale
                    var baselineHeight: Int? = null
                    listOf(0.9f, 1f, 1.5f).forEach { appScale ->
                        appTextScale.floatValue = appScale
                        compose.waitForIdle()
                        val banner = compose.onNodeWithTag("temporary-project-settings-banner")
                            .assertIsDisplayed().fetchSemanticsNode()
                        val height = banner.size.height
                        val expectedHeight = TemporaryProjectSettingsBarLayoutPolicy.heightDp(systemScale)
                        assertEquals("Banner uses the pre-reserved height at ${width}dp, system $systemScale, app $appScale",
                            expectedHeight, height)
                        assertEquals("App zoom must not change banner geometry at ${width}dp and system $systemScale",
                            baselineHeight ?: height, height)
                        baselineHeight = height
                    }
                }
            }
        } finally {
            TemporaryProjectSettings.clear()
        }
    }
}
