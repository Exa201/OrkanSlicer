package ru.ytkab0bp.slicebeam

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.fragment.PrintConfigFragment
import ru.ytkab0bp.slicebeam.navigation.MobileNavigationDelegate
import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.slic3r.Slic3rConfigWrapper
import ru.ytkab0bp.slicebeam.utils.Prefs

/** Exercises the real process-profile filter inside its profile screen while the IME is open. */
@RunWith(AndroidJUnit4::class)
class ProfileSettingsImeDeviceTest {
    @get:Rule
    val compose = createInitializedActivityRule()

    @Test
    fun profileFiltersRemainUsableWithKeyboardVisible() {
        val activity = compose.activity
        activity.runOnUiThread {
            (activity.navigationDelegate as MobileNavigationDelegate).switchToSlot(2)
        }
        compose.waitUntil(timeoutMillis = 15_000) {
            activity.navigationDelegate.getCurrentFragment() is PrintConfigFragment
        }

        val context = activity
        val searchToggle = context.getString(R.string.ProfileSettingsSearchAndFilter)
        compose.onNodeWithContentDescription(searchToggle).assertIsDisplayed().performClick()

        val searchHint = context.getString(R.string.SettingsSearchHint)
        compose.onNodeWithText(searchHint).assertIsDisplayed().performClick()
            .performTextInput("layer height")
        compose.waitUntil(timeoutMillis = 5_000) {
            ViewCompat.getRootWindowInsets(activity.window.decorView)
                ?.isVisible(WindowInsetsCompat.Type.ime()) == true
        }
        compose.onNodeWithText(searchHint).assertIsDisplayed()

        // Opening the level dropdown while the real profile screen's IME is visible proves that
        // its filter controls are not hidden behind the keyboard or trapped in the profile list.
        val levels = context.resources.getStringArray(R.array.SettingsLevels)
        val levelBefore = Prefs.getPrefs()?.getInt("profile_settings_level", 1)
            ?.coerceIn(0, levels.lastIndex) ?: 1.coerceIn(0, levels.lastIndex)
        compose.onNodeWithText(levels[levelBefore]).assertIsDisplayed().performClick()
        val otherLevel = if (levelBefore == 0) 1 else 0
        compose.onNodeWithText(levels[otherLevel]).assertIsDisplayed()
        // Select the currently active level so the test exercises the menu without changing the
        // user's profile visibility preference.
        val currentLevelNodes = compose.onAllNodesWithText(levels[levelBefore])
        currentLevelNodes[currentLevelNodes.fetchSemanticsNodes().lastIndex].performClick()

        val favoritesLabel = context.getString(R.string.ProfileSettingsFavoritesToggle)
        val favoriteToggle = compose.onNodeWithContentDescription(favoritesLabel)
        favoriteToggle.assertIsDisplayed().performClick()
        favoriteToggle.performClick()

        compose.onNodeWithText(searchHint).performTextClearance()
        compose.onNodeWithContentDescription(searchToggle).assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals("Profile screen remains the active destination", true,
                activity.navigationDelegate.getCurrentFragment() is PrintConfigFragment)
        }
    }

    private fun createInitializedActivityRule() = run {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val application = context.applicationContext as SliceBeam
        SliceBeam.INSTANCE = application
        Prefs.init(application)
        Prefs.getPrefs()?.edit()?.putBoolean("converter_storage_first_prompt", true)?.commit()
        Prefs.setLastCommit()
        SliceBeam.clearLiveDiffs()

        val config = context.assets.open("orca_profiles/Anycubic.ini").use(::Slic3rConfigWrapper)
        val printer = requireNotNull(config.findPrinter("Anycubic Kobra 3 0.4 nozzle"))
        val process = requireNotNull(config.findPrint("0.16mm Standard"))
        val filament = requireNotNull(config.findFilament("Anycubic Generic PLA"))
        config.presets = ConfigObject("presets").apply {
            put("printer", printer.title)
            put("print", process.title)
            put("filament", filament.title)
        }
        SliceBeam.CONFIG = config
        createAndroidComposeRule<MainActivity>()
    }
}
