package ru.ytkab0bp.slicebeam.components

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.MainActivity
import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.components.bed_menu.showAIGeneratorWorkspace
import ru.ytkab0bp.slicebeam.slic3r.Slic3rConfigWrapper
import ru.ytkab0bp.slicebeam.utils.Prefs

/** Verifies the active Maker generator forms use the exclusive workspace dialog/back route. */
@RunWith(AndroidJUnit4::class)
class MakerGeneratorWorkspaceDeviceTest {
    @get:Rule
    val compose = createInitializedActivityRule()

    @Test
    fun generatedModelDiscardUsesWorkspaceAndDeletesOnlyThatOutput() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val output = java.io.File(activity.cacheDir, "workspace-output-test.stl").apply {
            writeBytes(byteArrayOf(1, 2, 3, 4))
        }

        activity.runOnUiThread { ImageToStlConverter.showGeneratedOutput(activity, output, "stl", bed) }
        compose.waitUntil(timeoutMillis = 5_000) { bed.isWorkspaceMessageDialogAttached() }
        compose.onNodeWithText(output.name, substring = true).assertIsDisplayed()
        compose.onNodeWithText(activity.getString(ru.ytkab0bp.slicebeam.R.string.GeneratedOutputCancel)).performClick()
        compose.waitUntil(timeoutMillis = 5_000) { !bed.isWorkspaceMessageDialogAttached() }
        assertFalse("Discard should remove the generated cache output", output.exists())
        assertFalse("The output dialog must leave the workspace active", activity.isFinishing)
    }

    @Test
    fun lithophaneToneEditorAppliesChangesThroughWorkspaceCompose() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val imageFile = java.io.File(activity.cacheDir, "tone-editor-compose-test.png")
        Bitmap.createBitmap(12, 10, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.LTGRAY)
            imageFile.outputStream().use { compress(Bitmap.CompressFormat.PNG, 100, it) }
            recycle()
        }
        val settings = intArrayOf(0, 100, 100)
        val invert = booleanArrayOf(false)
        var closeCount = 0

        activity.runOnUiThread {
            ImageToStlConverter.showLithophaneImageEditor(
                activity,
                Uri.fromFile(imageFile),
                bed,
                settings,
                invert,
            ) { closeCount++ }
        }
        compose.waitUntil(timeoutMillis = 5_000) { bed.isWorkspaceMessageDialogAttached() }
        compose.onNodeWithText(activity.getString(ru.ytkab0bp.slicebeam.R.string.LithophaneImageEditor)).assertIsDisplayed()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithTag("tone-editor-preview-image").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("tone-editor-invert").performClick()
        compose.onNodeWithTag("tone-editor-mode").performClick()
        val redMode = activity.resources.getStringArray(ru.ytkab0bp.slicebeam.R.array.LithophaneGrayscaleModes)[3]
        compose.onNodeWithText(redMode).performClick()
        compose.onNodeWithTag("tone-editor-apply").performClick()
        compose.waitUntil(timeoutMillis = 5_000) { !bed.isWorkspaceMessageDialogAttached() }

        assertEquals("Apply should persist the selected grayscale channel", 3, settings[0])
        assertTrue("Apply should persist the invert setting", invert[0])
        assertEquals("The editor should return to its parent converter form", 1, closeCount)
        imageFile.delete()
    }

    @Test
    fun lithophaneToneEditorBackDiscardsUnappliedChanges() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val imageFile = java.io.File(activity.cacheDir, "tone-editor-back-test.png")
        Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.WHITE)
            imageFile.outputStream().use { compress(Bitmap.CompressFormat.PNG, 100, it) }
            recycle()
        }
        val settings = intArrayOf(1, 80, 130)
        val invert = booleanArrayOf(true)
        var closeCount = 0

        activity.runOnUiThread {
            ImageToStlConverter.showLithophaneImageEditor(
                activity,
                Uri.fromFile(imageFile),
                bed,
                settings,
                invert,
            ) { closeCount++ }
        }
        compose.waitUntil(timeoutMillis = 5_000) { bed.isWorkspaceMessageDialogAttached() }
        compose.onNodeWithTag("tone-editor-invert").performClick()
        activity.onBackPressedDispatcher.onBackPressed()
        compose.waitUntil(timeoutMillis = 5_000) { !bed.isWorkspaceMessageDialogAttached() }

        assertEquals("Back must not commit tone mode", 1, settings[0])
        assertEquals("Back must not commit brightness", 80, settings[1])
        assertEquals("Back must not commit contrast", 130, settings[2])
        assertTrue("Back must not commit invert changes", invert[0])
        assertEquals("Back should return to the parent converter form", 1, closeCount)
        imageFile.delete()
    }


    @Test
    fun aiGeneratorSelectionUsesWorkspaceCancelAndBackWithoutLaunchingExternalActivities() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment

        activity.runOnUiThread { showAIGeneratorWorkspace(bed) }
        compose.waitUntil(timeoutMillis = 5_000) { bed.isWorkspaceMessageDialogAttached() }
        compose.onNodeWithTag("ai-generator-camera").assertIsDisplayed()
        compose.onNodeWithTag("ai-generator-gallery").assertIsDisplayed()
        compose.onNodeWithTag("generator-dialog-cancel").performClick()
        compose.waitUntil(timeoutMillis = 5_000) { !bed.isWorkspaceMessageDialogAttached() }

        activity.runOnUiThread { showAIGeneratorWorkspace(bed) }
        compose.waitUntil(timeoutMillis = 5_000) { bed.isWorkspaceMessageDialogAttached() }
        compose.onNodeWithTag("ai-generator-camera").assertIsDisplayed()
        activity.onBackPressedDispatcher.onBackPressed()
        compose.waitUntil(timeoutMillis = 5_000) { !bed.isWorkspaceMessageDialogAttached() }
        assertFalse("Back should dismiss the cloud generator choice", activity.isFinishing)
    }

    @Test
    fun generatorFormsCancelAndBackThroughWorkspaceWithoutStartingGeneration() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val generators: List<Pair<Int, () -> Unit>> = listOf(
            ru.ytkab0bp.slicebeam.R.string.FastenerTitle to { FastenerGenerator.show(activity, bed) },
            ru.ytkab0bp.slicebeam.R.string.CableClipTitle to { CableClipGenerator.show(activity, bed) },
            ru.ytkab0bp.slicebeam.R.string.CableGrommetTitle to { CableGrommetGenerator.show(activity, bed) },
            ru.ytkab0bp.slicebeam.R.string.UtilityBoxTitle to { UtilityBoxGenerator.show(activity, bed) },
            ru.ytkab0bp.slicebeam.R.string.TextTagTitle to { TextTagGenerator.show(activity, bed) },
            ru.ytkab0bp.slicebeam.R.string.PhoneStandTitle to { PhoneStandGenerator.show(activity, bed) },
            ru.ytkab0bp.slicebeam.R.string.PlanterTitle to { PlanterPotGenerator.show(activity, bed) },
            ru.ytkab0bp.slicebeam.R.string.PlantTagsTitle to { PlantLabelSetGenerator.show(activity, bed) },
            ru.ytkab0bp.slicebeam.R.string.PowerStripTitle to { PowerStripTrayGenerator.show(activity, bed) },
            ru.ytkab0bp.slicebeam.R.string.WallToolHolderTitle to { WallToolHolderGenerator.show(activity, bed) },
            ru.ytkab0bp.slicebeam.R.string.LitoGenTitle to {
                LitoGenOptionsDialog.show(
                    activity,
                    List(4) { index -> Uri.parse("content://maker-test/image-$index") },
                    bed,
                ) { _, _ -> "Test options only" }
            },
        )

        generators.forEach { (title, show) ->
            activity.runOnUiThread { show() }
            compose.waitUntil(timeoutMillis = 5_000) { bed.isWorkspaceMessageDialogAttached() }
            compose.onNodeWithText(activity.getString(title)).assertIsDisplayed()
            compose.onNodeWithTag("generator-dialog-title").assertIsDisplayed()
            compose.onNodeWithTag("generator-dialog-cancel").assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 5_000) { !bed.isWorkspaceMessageDialogAttached() }
            assertFalse("Cancel must leave the Maker activity open", activity.isFinishing)

            activity.runOnUiThread { show() }
            compose.waitUntil(timeoutMillis = 5_000) { bed.isWorkspaceMessageDialogAttached() }
            compose.onNodeWithText(activity.getString(title)).assertIsDisplayed()
            compose.onNodeWithTag("generator-dialog-title").assertIsDisplayed()
            activity.onBackPressedDispatcher.onBackPressed()
            compose.waitUntil(timeoutMillis = 5_000) { !bed.isWorkspaceMessageDialogAttached() }
            assertFalse("Back must dismiss the generator form without leaving the activity", activity.isFinishing)
        }
    }

    @Test
    fun litogenValidationStaysInWorkspaceFormWithoutDismissal() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        var attempts = 0
        activity.runOnUiThread {
            LitoGenOptionsDialog.show(
                activity,
                List(4) { index -> Uri.parse("content://maker-test/validation-$index") },
                bed,
            ) { _, _ ->
                attempts++
                "Test validation message"
            }
        }
        compose.waitUntil(timeoutMillis = 5_000) { bed.isWorkspaceMessageDialogAttached() }
        compose.onNodeWithText(activity.getString(ru.ytkab0bp.slicebeam.R.string.LithophaneGenerate)).performClick()
        compose.onNodeWithText("Test validation message").assertIsDisplayed()
        assertEquals("The explicit generate action should reach the injected validation gate", 1, attempts)
        assertTrue("Validation must keep the options dialog open", bed.isWorkspaceMessageDialogAttached())
        activity.onBackPressedDispatcher.onBackPressed()
        compose.waitUntil(timeoutMillis = 5_000) { !bed.isWorkspaceMessageDialogAttached() }
        assertEquals("Back after validation must not retry generation", 1, attempts)
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
