package ru.ytkab0bp.slicebeam.components.bed_menu

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme

/** Checks the calibration row's large-text behavior in a constrained Compose window. */
class CalibrationActionRowAccessibilityDeviceTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun longCalibrationTextGetsMoreLinesAndFullAccessibleDescriptionAt150Percent() {
        val title = "Druckvorlauf (Pressure Advance)"
        val subtitle = "Pressure-Advance-Test über die Ebenen mit dem aktuellen Modell"
        val fontScale = mutableFloatStateOf(1f)
        val rowHeightPx = mutableIntStateOf(0)

        compose.setContent {
            val density = remember(fontScale.floatValue) { Density(1f, fontScale.floatValue) }
            androidx.compose.runtime.CompositionLocalProvider(LocalDensity provides density) {
                BeamComposeTheme {
                    Box(Modifier.width(320.dp)) {
                        CalibrationActionRow(
                            icon = R.drawable.menu_calibrate_la_28,
                            title = title,
                            subtitle = subtitle,
                            testTag = "calibration-large-text-row",
                            onClick = {},
                            modifier = Modifier.onGloballyPositioned {
                                if (rowHeightPx.intValue != it.size.height) rowHeightPx.intValue = it.size.height
                            },
                        )
                    }
                }
            }
        }
        compose.waitForIdle()
        val normalHeight = rowHeightPx.intValue
        compose.onNodeWithTag("calibration-large-text-row").assertIsDisplayed()

        compose.runOnUiThread { fontScale.floatValue = 1.5f }
        compose.waitForIdle()

        compose.onNodeWithContentDescription("$title. $subtitle").assertIsDisplayed()
        assertTrue(
            "At 150% text, the calibration action should grow to show additional wrapped text",
            rowHeightPx.intValue > normalHeight,
        )
    }
}
