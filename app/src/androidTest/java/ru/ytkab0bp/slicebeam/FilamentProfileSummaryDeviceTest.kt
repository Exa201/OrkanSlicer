package ru.ytkab0bp.slicebeam

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.config.ProfileSettingsRepository
import ru.ytkab0bp.slicebeam.config.TemporaryProjectSettings
import ru.ytkab0bp.slicebeam.fragment.ProfileConfigValueController
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import ru.ytkab0bp.slicebeam.ui.settings.FilamentProfileSummary
import ru.ytkab0bp.slicebeam.utils.Prefs

@RunWith(AndroidJUnit4::class)
class FilamentProfileSummaryDeviceTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private lateinit var profile: ConfigObject

    @Before
    fun setUp() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as SliceBeam
        SliceBeam.INSTANCE = app
        Prefs.init(app)
        TemporaryProjectSettings.clear()
        ProfileSettingsRepository.clearAllDrafts()
        profile = ConfigObject("ColorFlow PETG")
        profile.profileListType = ConfigObject.PROFILE_LIST_FILAMENT
        profile.put("filament_type", "PETG")
        profile.put("filament_colour", "#123456")
        profile.put("nozzle_temperature_initial_layer", "235")
        profile.put("hot_plate_temp_initial_layer", "75")
        ProfileSettingsRepository.bindActiveProfile(ConfigObject.PROFILE_LIST_FILAMENT, profile.title)
    }

    @Test
    fun summaryShowsSelectedProfileMaterialSwatchAndTemperatures() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.setContent {
            BeamComposeTheme { FilamentProfileSummary(profile) }
        }

        compose.onNodeWithContentDescription(
            context.getString(R.string.FilamentSummaryDescription, "ColorFlow PETG", "PETG", "235", "75"),
        ).assertIsDisplayed()
        compose.onNodeWithText("Material: PETG").assertIsDisplayed()
        compose.onNodeWithText("Nozzle 235 °C · bed 75 °C").assertIsDisplayed()

        compose.runOnUiThread {
            ProfileConfigValueController.stageValue(
                ConfigObject.PROFILE_LIST_FILAMENT,
                profile,
                "nozzle_temperature_initial_layer",
                null,
                -1,
                "240",
                "235",
            )
        }
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Nozzle 240 °C · bed 75 °C").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Nozzle 240 °C · bed 75 °C").assertIsDisplayed()
        compose.onNodeWithContentDescription(
            context.getString(R.string.FilamentSummaryDescription, "ColorFlow PETG", "PETG", "240", "75"),
        ).assertIsDisplayed()
    }
}
