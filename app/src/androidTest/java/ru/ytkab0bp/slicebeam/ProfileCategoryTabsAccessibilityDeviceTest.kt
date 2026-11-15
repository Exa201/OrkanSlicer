package ru.ytkab0bp.slicebeam

import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import ru.ytkab0bp.slicebeam.ui.settings.ProfileCategoryTabs

@RunWith(AndroidJUnit4::class)
class ProfileCategoryTabsAccessibilityDeviceTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun longLocalizedCategoryNamesRemainVisibleAndSelectableAt200Percent() {
        val labels = listOf(
            "Qualitätsprofil mit erweiterten Druckoptionen",
            "Stützstrukturen und mehrsprachige Einstellungen",
            "Geschwindigkeit",
        )
        var selected = -1
        compose.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(1f, 2f),
                LocalConfiguration provides Configuration().apply {
                    screenWidthDp = 320
                    screenHeightDp = 640
                },
            ) {
                BeamComposeTheme {
                    Box(Modifier.width(320.dp)) {
                        ProfileCategoryTabs(labels, selectedIndex = selected) { selected = it }
                    }
                }
            }
        }

        compose.onNodeWithText(labels[0], substring = false)
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        compose.runOnIdle { assertEquals(0, selected) }
        compose.onNodeWithText(labels[1], substring = false).assertIsDisplayed()
        compose.onNodeWithText(labels[2], substring = false).assertIsDisplayed()
    }
}
