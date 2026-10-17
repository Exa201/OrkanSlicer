package ru.ytkab0bp.slicebeam.components.bed_menu

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.MainActivity
import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.slic3r.Slic3rConfigWrapper
import ru.ytkab0bp.slicebeam.utils.Prefs

/** Exercises the read-only G-code result workspaces without starting a slice or printer operation. */
@RunWith(AndroidJUnit4::class)
class SliceResultWorkspaceDeviceTest {
    @get:Rule
    val compose = createInitializedActivityRule()

    @Test
    fun resultAndLayerRoutesUseTheExclusiveWorkspaceHost() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment

        activity.runOnUiThread { showSliceInfoWorkspace(bed, SliceMenu()) }
        compose.waitUntil(timeoutMillis = 5_000) { bed.isWorkspaceMessageDialogAttached() }
        compose.onNodeWithTag("slice-info-title").assertIsDisplayed()
        compose.onNodeWithTag("slice-info-scheme").assertIsDisplayed()
        compose.onNodeWithTag("slice-info-seams").assertIsDisplayed()
        compose.onNodeWithTag("slice-info-close").performClick()
        compose.waitUntil(timeoutMillis = 5_000) { !bed.isWorkspaceMessageDialogAttached() }

        activity.runOnUiThread { showSliceLayersWorkspace(bed) }
        compose.waitUntil(timeoutMillis = 5_000) { bed.isWorkspaceMessageDialogAttached() }
        compose.onNodeWithTag("slice-layers-title").assertIsDisplayed()
        compose.onNodeWithTag("slice-layers-range-title").assertIsDisplayed()
        activity.runOnUiThread { activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitUntil(timeoutMillis = 5_000) { !bed.isWorkspaceMessageDialogAttached() }

        assertFalse("Closing result tools must not finish the app", activity.isFinishing)
    }

    private fun createInitializedActivityRule() = run {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val application = context.applicationContext as Application
        SliceBeam.INSTANCE = application as SliceBeam
        Prefs.init(application)
        Prefs.getPrefs()?.edit()?.remove("crash")?.putBoolean("converter_storage_first_prompt", true)?.commit()
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
