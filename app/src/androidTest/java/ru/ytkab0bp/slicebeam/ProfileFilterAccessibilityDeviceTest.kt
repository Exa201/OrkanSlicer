package ru.ytkab0bp.slicebeam

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import ru.ytkab0bp.slicebeam.ui.settings.ProfileFilterBar

/** Verifies profile search/filter actions and toggle state with large text on a narrow display. */
@RunWith(AndroidJUnit4::class)
class ProfileFilterAccessibilityDeviceTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun favoritesToggleAnnouncesStateAndProfileFiltersRemainReachable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var favoriteSelection = false
        var submittedQuery = ""
        var selectedLevel = -1
        compose.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(1f, 1.5f),
                LocalConfiguration provides Configuration().apply {
                    screenWidthDp = 320
                    screenHeightDp = 640
                },
            ) {
                BeamComposeTheme {
                    ProfileFilterBar(
                        initialQuery = "",
                        initialLevel = 0,
                        initialFavoritesOnly = false,
                        onQueryChanged = { submittedQuery = it },
                        onLevelChanged = { selectedLevel = it },
                        onFavoritesChanged = { favoriteSelection = it },
                    )
                }
            }
        }

        compose.onNodeWithText(context.getString(R.string.ProfileSettingsSearchAndFilter))
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        val favoriteLabel = context.getString(R.string.ProfileSettingsFavoritesToggle)
        val disabled = context.getString(R.string.ProfileSettingsFavoritesOnlyDisabled)
        val enabled = context.getString(R.string.ProfileSettingsFavoritesOnlyEnabled)
        compose.onNodeWithContentDescription(favoriteLabel)
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, disabled))
            .performClick()
        compose.onNodeWithContentDescription(favoriteLabel)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, enabled))
        compose.runOnIdle { assertEquals(true, favoriteSelection) }

        val levels = context.resources.getStringArray(R.array.SettingsLevels)
        compose.onNodeWithText(levels[0]).assertIsDisplayed().assertHeightIsAtLeast(48.dp).performClick()
        compose.onNodeWithText(levels[1]).assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, selectedLevel) }

        compose.onNodeWithText(context.getString(R.string.SettingsSearchHint))
            .assertIsDisplayed()
            .performTextInput("  layer height  ")
        compose.runOnIdle { assertEquals("layer height", submittedQuery) }
    }
}
