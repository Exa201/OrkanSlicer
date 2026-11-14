package ru.ytkab0bp.slicebeam

import android.content.res.Configuration
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
import ru.ytkab0bp.slicebeam.components.bed_menu.BedComposeMenuAction
import ru.ytkab0bp.slicebeam.components.bed_menu.BedMenuActionsContent
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import ru.ytkab0bp.slicebeam.ui.OrkanSection

/** Keeps long workspace action labels readable without changing their accessible names. */
@RunWith(AndroidJUnit4::class)
class WorkspaceActionTileAccessibilityDeviceTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun scaleToBuildVolumeUsesShortVisibleLabelAtLargeTextScale() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var actions = 0
        compose.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(1f, 1.5f),
                LocalConfiguration provides Configuration().apply {
                    screenWidthDp = 320
                    screenHeightDp = 640
                },
            ) {
                BeamComposeTheme(section = OrkanSection.WORKSPACE) {
                    BedMenuActionsContent(
                        actions = listOf(BedComposeMenuAction(
                            titleRes = R.string.ModelScaleToBuildVolume,
                            iconRes = R.drawable.arrow_up_right_corner_outline_24,
                            onClick = { actions++ },
                        )),
                        portrait = true,
                        modifier = Modifier.width(320.dp).height(116.dp),
                    )
                }
            }
        }

        compose.onNodeWithText(context.getString(R.string.ModelScaleToBuildVolumeShort)).assertIsDisplayed()
        compose.onNodeWithTag("workspace-menu-action-${R.string.ModelScaleToBuildVolume}")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        compose.runOnIdle { assertEquals(1, actions) }
    }
}
