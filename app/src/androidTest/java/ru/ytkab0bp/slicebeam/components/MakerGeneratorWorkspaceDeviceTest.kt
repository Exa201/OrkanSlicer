package ru.ytkab0bp.slicebeam.components

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
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
    fun savedModelFollowupUsesWorkspaceAndKeepDeletesOnlyTemporaryOutput() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val output = java.io.File(activity.cacheDir, "workspace-saved-followup-test.stl").apply {
            writeBytes(byteArrayOf(5, 6, 7, 8))
        }

        activity.runOnUiThread {
            ImageToStlConverter.showSavedOutputQuestion(activity, output, "stl", bed)
        }
        compose.waitUntil(timeoutMillis = 5_000) { bed.isWorkspaceMessageDialogAttached() }
        compose.onNodeWithText(activity.getString(ru.ytkab0bp.slicebeam.R.string.ConverterLoadQuestion)).assertIsDisplayed()
        compose.onNodeWithText(activity.getString(ru.ytkab0bp.slicebeam.R.string.ConverterOnlySave)).performClick()
        compose.waitUntil(timeoutMillis = 5_000) { !bed.isWorkspaceMessageDialogAttached() }
        assertFalse("Keep-saved action should remove only the temporary cache output", output.exists())
    }

    @Test
    fun converterErrorUsesWorkspaceComposeAndDismissesWithOk() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment

        activity.runOnUiThread {
            ImageToStlConverter.showError(activity, java.io.IOException("Converter test error"))
        }
        compose.waitUntil(timeoutMillis = 5_000) { bed.isWorkspaceMessageDialogAttached() }
        compose.onNodeWithText(activity.getString(ru.ytkab0bp.slicebeam.R.string.ConverterFailed)).assertIsDisplayed()
        compose.onNodeWithText("Converter test error").assertIsDisplayed()
        compose.onNodeWithText(activity.getString(android.R.string.ok)).performClick()
        compose.waitUntil(timeoutMillis = 5_000) { !bed.isWorkspaceMessageDialogAttached() }
    }

    @Test
    fun kromacutGeneratedStlUsesValidatedWorkspaceOutputAndCanBeDiscarded() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val menu = KromacutEditorMenu()
        activity.runOnUiThread { bed.showWorkspaceComposeTool(menu) }
        compose.waitUntil(timeoutMillis = 5_000) { menu.isAttached() }

        val bridgeClass = KromacutEditorMenu::class.java.declaredClasses
            .single { it.simpleName == "DownloadBridge" }
        val bridge = bridgeClass.getDeclaredConstructor(KromacutEditorMenu::class.java)
            .apply { isAccessible = true }
            .newInstance(menu)
        val before = activity.cacheDir.listFiles().orEmpty().mapTo(hashSetOf()) { it.name }
        val encodedStl = Base64.encodeToString(validTetrahedronStl(), Base64.NO_WRAP)
        activity.runOnUiThread {
            bridgeClass.getDeclaredMethod("beginDownload", String::class.java)
                .apply { isAccessible = true }.invoke(bridge, "../../kromacut-roundtrip.stl")
            bridgeClass.getDeclaredMethod("writeData", String::class.java)
                .apply { isAccessible = true }.invoke(bridge, encodedStl)
            bridgeClass.getDeclaredMethod("finishDownload")
                .apply { isAccessible = true }.invoke(bridge)
        }

        compose.waitUntil(timeoutMillis = 10_000) { bed.isWorkspaceMessageDialogAttached() }
        val generated = activity.cacheDir.listFiles().orEmpty()
            .filter { it.name !in before && it.name.startsWith("kromacut-") && it.name.endsWith("-kromacut-roundtrip.stl") }
            .singleOrNull()
        assertTrue("KromaCut must hand the generated file to the shared output dialog", generated?.isFile == true)
        assertEquals("The bridge must decode the complete binary STL payload", validTetrahedronStl().size.toLong(), generated!!.length())
        compose.onNodeWithText(generated.name, substring = true).assertIsDisplayed()
        compose.onNodeWithText(activity.getString(ru.ytkab0bp.slicebeam.R.string.GeneratedOutputCancel))
            .performClick()
        compose.waitUntil(timeoutMillis = 5_000) { !bed.isWorkspaceMessageDialogAttached() }
        assertFalse("Discard must delete only KromaCut's temporary output", generated.exists())

        activity.runOnUiThread { bed.onBackPressed() }
        compose.waitUntil(timeoutMillis = 5_000) { !menu.isAttached() }
    }

    @Test
    fun kromacutSaveFallsBackToCreateDocumentWhenFolderPickerIsCancelled() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val menu = KromacutEditorMenu()
        val preferences = requireNotNull(Prefs.getPrefs())
        val previousRoot = preferences.getString("converter_storage_tree_uri", null)
        preferences.edit().remove("converter_storage_tree_uri").commit()

        val launchersField = MainActivity::class.java.getDeclaredField("activityResultLaunchers")
            .apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val launchers = launchersField.get(activity) as MutableMap<Int, ActivityResultLauncher<Intent>>
        val rootRequestCode = MainActivity.REQUEST_CODE_CONVERTER_ROOT_SAVE
        val documentRequestCode = MainActivity.REQUEST_CODE_SAVE_CONVERTED_FILE
        val previousRootLauncher = requireNotNull(launchers[rootRequestCode])
        val previousDocumentLauncher = requireNotNull(launchers[documentRequestCode])
        val rootIntent = java.util.concurrent.atomic.AtomicReference<Intent?>()
        val documentIntent = java.util.concurrent.atomic.AtomicReference<Intent?>()
        fun capture(target: java.util.concurrent.atomic.AtomicReference<Intent?>) =
            object : ActivityResultLauncher<Intent>() {
                override val contract: androidx.activity.result.contract.ActivityResultContract<Intent, *> =
                    ActivityResultContracts.StartActivityForResult()

                override fun launch(input: Intent, options: ActivityOptionsCompat?) {
                    target.set(Intent(input))
                }

                override fun unregister() = Unit
            }
        launchers[rootRequestCode] = capture(rootIntent)
        launchers[documentRequestCode] = capture(documentIntent)

        var generated: java.io.File? = null
        try {
            activity.runOnUiThread { bed.showWorkspaceComposeTool(menu) }
            compose.waitUntil(timeoutMillis = 5_000) { menu.isAttached() }
            val bridgeClass = KromacutEditorMenu::class.java.declaredClasses
                .single { it.simpleName == "DownloadBridge" }
            val bridge = bridgeClass.getDeclaredConstructor(KromacutEditorMenu::class.java)
                .apply { isAccessible = true }
                .newInstance(menu)
            val encodedStl = Base64.encodeToString(validTetrahedronStl(), Base64.NO_WRAP)
            activity.runOnUiThread {
                bridgeClass.getDeclaredMethod("beginDownload", String::class.java)
                    .apply { isAccessible = true }.invoke(bridge, "kromacut-save-fallback.stl")
                bridgeClass.getDeclaredMethod("writeData", String::class.java)
                    .apply { isAccessible = true }.invoke(bridge, encodedStl)
                bridgeClass.getDeclaredMethod("finishDownload")
                    .apply { isAccessible = true }.invoke(bridge)
            }
            compose.waitUntil(timeoutMillis = 10_000) { bed.isWorkspaceMessageDialogAttached() }
            compose.onNodeWithText(activity.getString(ru.ytkab0bp.slicebeam.R.string.GeneratedOutputSave))
                .performClick()
            compose.waitUntil(timeoutMillis = 5_000) { rootIntent.get() != null }
            assertEquals(Intent.ACTION_OPEN_DOCUMENT_TREE, requireNotNull(rootIntent.get()).action)

            dispatchResult(activity, rootRequestCode, android.app.Activity.RESULT_CANCELED, Intent())
            compose.waitUntil(timeoutMillis = 5_000) { documentIntent.get() != null }
            val saveIntent = requireNotNull(documentIntent.get())
            assertEquals("Cancellation of the remembered-folder picker must fall back to SAF", Intent.ACTION_CREATE_DOCUMENT, saveIntent.action)
            assertEquals("model/stl", saveIntent.type)
            assertTrue(saveIntent.getStringExtra(Intent.EXTRA_TITLE).orEmpty().endsWith("kromacut-save-fallback.stl"))
            generated = activity.cacheDir.listFiles().orEmpty()
                .single { it.name.startsWith("kromacut-") && it.name.endsWith("-kromacut-save-fallback.stl") }

            dispatchResult(activity, documentRequestCode, android.app.Activity.RESULT_CANCELED, Intent())
            compose.waitUntil(timeoutMillis = 5_000) { !generated!!.exists() }
            assertFalse("Cancelling Android's document picker must remove the temporary KromaCut output", generated!!.exists())
        } finally {
            if (generated?.exists() == true) ImageToStlConverter.finishSave(activity, null)
            launchers[rootRequestCode] = previousRootLauncher
            launchers[documentRequestCode] = previousDocumentLauncher
            preferences.edit().also { editor ->
                if (previousRoot == null) editor.remove("converter_storage_tree_uri")
                else editor.putString("converter_storage_tree_uri", previousRoot)
            }.commit()
            if (menu.isAttached()) activity.runOnUiThread { bed.onBackPressed() }
        }
    }

    @Test
    fun kromacutWebGlCreationFailureShowsDismissibleWorkspaceError() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val menu = KromacutEditorMenu()
        activity.runOnUiThread { bed.showWorkspaceComposeTool(menu) }
        compose.waitUntil(timeoutMillis = 5_000) { menu.isAttached() }

        val bridgeClass = KromacutEditorMenu::class.java.declaredClasses
            .single { it.simpleName == "DownloadBridge" }
        val bridge = bridgeClass.getDeclaredConstructor(KromacutEditorMenu::class.java)
            .apply { isAccessible = true }
            .newInstance(menu)
        activity.runOnUiThread {
            bridgeClass.getDeclaredMethod("reportRendererIssue", String::class.java)
                .apply { isAccessible = true }
                .invoke(bridge, "webgl2-creation-failed:synthetic test failure")
        }

        val message = activity.getString(
            ru.ytkab0bp.slicebeam.R.string.KromacutWebGlCreationFailed,
            "synthetic test failure",
        )
        compose.onNodeWithTag("kromacut-error-message").assertIsDisplayed()
        compose.onNodeWithText(message).assertIsDisplayed()
        compose.onNodeWithText(activity.getString(android.R.string.ok)).performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithTag("kromacut-error-message").fetchSemanticsNodes().isEmpty()
        }
        assertTrue("Dismissing the renderer explanation must leave KromaCut open", menu.isAttached())
        activity.runOnUiThread { bed.onBackPressed() }
        compose.waitUntil(timeoutMillis = 5_000) { !menu.isAttached() }
    }

    @Test
    fun activityFileOperationErrorsUseWorkspaceComposeAndDismissWithOk() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment

        activity.runOnUiThread {
            activity.showFileOperationError("Workspace operation error", "The operation could not be completed")
        }
        compose.waitUntil(timeoutMillis = 5_000) { bed.isWorkspaceMessageDialogAttached() }
        compose.onNodeWithText("Workspace operation error").assertIsDisplayed()
        compose.onNodeWithText("The operation could not be completed").assertIsDisplayed()
        compose.onNodeWithText(activity.getString(android.R.string.ok)).performClick()
        compose.waitUntil(timeoutMillis = 5_000) { !bed.isWorkspaceMessageDialogAttached() }
    }

    @Test
    fun converterProgressUsesWorkspaceAndCompletesWithoutCancelingTheJob() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val title = "Workspace converter progress test"
        var completed = false
        lateinit var progress: ConverterProgressDialog.Progress

        activity.runOnUiThread {
            progress = ConverterProgressDialog.show(activity, title, bed)
        }
        compose.waitUntil(timeoutMillis = 5_000) { bed.isWorkspaceMessageDialogAttached() }
        compose.onNodeWithText(title).assertIsDisplayed()
        compose.onNodeWithTag("converter-progress-stage").assertIsDisplayed()

        progress.setStage(42, "Workspace test generation")
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText("Workspace test generation").fetchSemanticsNodes().isNotEmpty()
        }
        progress.complete { completed = true }
        compose.waitUntil(timeoutMillis = 5_000) {
            completed && !bed.isWorkspaceMessageDialogAttached()
        }
        assertTrue("Completion callback must run after the workspace progress panel closes", completed)
    }

    @Test
    fun imageConverterOptionsUseWorkspaceAndKeepModeAndEditorStateOnBack() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val imageFile = java.io.File(activity.cacheDir, "converter-options-compose-test.png")
        Bitmap.createBitmap(16, 12, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.LTGRAY)
            imageFile.outputStream().use { compress(Bitmap.CompressFormat.PNG, 100, it) }
            recycle()
        }

        activity.runOnUiThread {
            ImageToStlConverter.showOptions(activity, Uri.fromFile(imageFile), bed)
        }
        compose.waitUntil(timeoutMillis = 5_000) { bed.isWorkspaceMessageDialogAttached() }
        compose.onNodeWithText(activity.getString(ru.ytkab0bp.slicebeam.R.string.ConverterTitle)).assertIsDisplayed()
        compose.onNodeWithTag("image-to-stl-target").assertIsDisplayed()
        compose.onNodeWithTag("image-to-stl-mode").assertIsDisplayed()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithTag("image-to-stl-source-preview").fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNodeWithTag("image-to-stl-mode").performClick()
        val curvedMode = activity.resources.getStringArray(ru.ytkab0bp.slicebeam.R.array.ImageToStlModes)[5]
        compose.onNodeWithText(curvedMode).performClick()
        compose.onNodeWithText(activity.getString(ru.ytkab0bp.slicebeam.R.string.LithophaneCurveRadius))
            .performScrollTo().assertIsDisplayed()

        compose.onNodeWithTag("image-to-stl-edit-image").performScrollTo()
        compose.onNodeWithTag("image-to-stl-edit-image").performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText(activity.getString(ru.ytkab0bp.slicebeam.R.string.LithophaneImageEditor)).fetchSemanticsNodes().isNotEmpty()
        }
        activity.onBackPressedDispatcher.onBackPressed()
        compose.waitUntil(timeoutMillis = 5_000) { bed.isWorkspaceMessageDialogAttached() }
        compose.onNodeWithTag("image-to-stl-mode").assertIsDisplayed()
        compose.onNodeWithText(curvedMode).assertIsDisplayed()
        compose.onNodeWithText(activity.getString(ru.ytkab0bp.slicebeam.R.string.LithophaneCurveRadius))
            .performScrollTo().assertIsDisplayed()

        compose.onNodeWithTag("generator-dialog-cancel").performClick()
        compose.waitUntil(timeoutMillis = 5_000) { !bed.isWorkspaceMessageDialogAttached() }
        assertFalse("Cancel must not start generation or finish the workspace", activity.isFinishing)
        imageFile.delete()
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
    fun textTagGeneratorCreatesValidatedStlAndDiscardRemovesOnlyItsCacheOutput() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val cache = activity.cacheDir
        val existing = cache.listFiles().orEmpty().map { it.name }.toSet()

        activity.runOnUiThread { TextTagGenerator.show(activity, bed) }
        compose.waitUntil(timeoutMillis = 5_000) { bed.isWorkspaceMessageDialogAttached() }
        compose.onNodeWithTag("generator-dialog-title").assertIsDisplayed()
        compose.onNodeWithText(activity.getString(ru.ytkab0bp.slicebeam.R.string.ConverterConvertSave))
            .performClick()

        // Generation runs on IO_POOL. The output dialog is shown only after the binary STL
        // validator has accepted its triangle count, bounds, and exact file length.
        compose.waitUntil(timeoutMillis = 60_000) {
            bed.isWorkspaceMessageDialogAttached() &&
                compose.onAllNodesWithText(activity.getString(ru.ytkab0bp.slicebeam.R.string.GeneratedOutputTitle))
                    .fetchSemanticsNodes().isNotEmpty()
        }
        val generated = cache.listFiles().orEmpty().singleOrNull {
            it.name.startsWith("text-tag-") && it.name !in existing
        }
        assertTrue("Successful generation must produce one cache STL", generated != null)
        requireNotNull(generated)
        assertTrue("Validated binary STL must contain facets", generated.length() > 84L)
        compose.onNodeWithText(activity.getString(ru.ytkab0bp.slicebeam.R.string.GeneratedOutputCancel))
            .performClick()
        compose.waitUntil(timeoutMillis = 5_000) { !bed.isWorkspaceMessageDialogAttached() }
        assertFalse("Discard must delete the generated temporary STL", generated.exists())
        assertFalse("Generation and discard must keep the workspace active", activity.isFinishing)
    }

    @Test
    fun cableGrommetGeneratorCreatesValidatedStlAndDiscardRemovesOnlyItsCacheOutput() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val cache = activity.cacheDir
        val existing = cache.listFiles().orEmpty().map { it.name }.toSet()

        activity.runOnUiThread { CableGrommetGenerator.show(activity, bed) }
        compose.waitUntil(timeoutMillis = 5_000) { bed.isWorkspaceMessageDialogAttached() }
        compose.onNodeWithTag("generator-dialog-title").assertIsDisplayed()
        compose.onNodeWithText(activity.getString(ru.ytkab0bp.slicebeam.R.string.ConverterConvertSave))
            .performClick()
        compose.waitUntil(timeoutMillis = 60_000) {
            bed.isWorkspaceMessageDialogAttached() &&
                compose.onAllNodesWithText(activity.getString(ru.ytkab0bp.slicebeam.R.string.GeneratedOutputTitle))
                    .fetchSemanticsNodes().isNotEmpty()
        }

        val generated = cache.listFiles().orEmpty().singleOrNull {
            it.name.startsWith("cable-grommet-") && it.name !in existing
        }
        assertTrue("Successful generation must produce one cache STL", generated != null)
        requireNotNull(generated)
        assertTrue("Validated binary STL must contain facets", generated.length() > 84L)
        compose.onNodeWithText(activity.getString(ru.ytkab0bp.slicebeam.R.string.GeneratedOutputCancel))
            .performClick()
        compose.waitUntil(timeoutMillis = 5_000) { !bed.isWorkspaceMessageDialogAttached() }
        assertFalse("Discard must delete the generated temporary STL", generated.exists())
        assertFalse("Generation and discard must keep the workspace active", activity.isFinishing)
    }

    @Test
    fun phoneStandGeneratorCreatesValidatedStlAndDiscardRemovesOnlyItsCacheOutput() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val cache = activity.cacheDir
        val existing = cache.listFiles().orEmpty().map { it.name }.toSet()

        activity.runOnUiThread { PhoneStandGenerator.show(activity, bed) }
        compose.waitUntil(timeoutMillis = 5_000) { bed.isWorkspaceMessageDialogAttached() }
        compose.onNodeWithTag("generator-dialog-title").assertIsDisplayed()
        compose.onNodeWithText(activity.getString(ru.ytkab0bp.slicebeam.R.string.ConverterConvertSave))
            .performClick()
        compose.waitUntil(timeoutMillis = 60_000) {
            bed.isWorkspaceMessageDialogAttached() &&
                compose.onAllNodesWithText(activity.getString(ru.ytkab0bp.slicebeam.R.string.GeneratedOutputTitle))
                    .fetchSemanticsNodes().isNotEmpty()
        }

        val generated = cache.listFiles().orEmpty().singleOrNull {
            it.name.startsWith("phone-stand-") && it.name !in existing
        }
        assertTrue("Successful generation must produce one cache STL", generated != null)
        requireNotNull(generated)
        assertTrue("Validated binary STL must contain facets", generated.length() > 84L)
        compose.onNodeWithText(activity.getString(ru.ytkab0bp.slicebeam.R.string.GeneratedOutputCancel))
            .performClick()
        compose.waitUntil(timeoutMillis = 5_000) { !bed.isWorkspaceMessageDialogAttached() }
        assertFalse("Discard must delete the generated temporary STL", generated.exists())
        assertFalse("Generation and discard must keep the workspace active", activity.isFinishing)
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

    private fun validTetrahedronStl(): ByteArray {
        val triangles = listOf(
            floatArrayOf(0f, 0f, 0f, 10f, 0f, 0f, 0f, 10f, 0f),
            floatArrayOf(0f, 0f, 0f, 0f, 10f, 0f, 0f, 0f, 10f),
            floatArrayOf(0f, 0f, 0f, 0f, 0f, 10f, 10f, 0f, 0f),
            floatArrayOf(10f, 0f, 0f, 0f, 0f, 10f, 0f, 10f, 0f),
        )
        val bytes = ByteArrayOutputStream(84 + triangles.size * 50)
        bytes.write(ByteArray(80))
        bytes.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(triangles.size).array())
        triangles.forEach { triangle ->
            val facet = ByteBuffer.allocate(50).order(ByteOrder.LITTLE_ENDIAN)
            repeat(3) { facet.putFloat(0f) }
            triangle.forEach(facet::putFloat)
            facet.putShort(0)
            bytes.write(facet.array())
        }
        return bytes.toByteArray()
    }

    private fun dispatchResult(activity: MainActivity, requestCode: Int, resultCode: Int, data: Intent) {
        activity.runOnUiThread {
            MainActivity::class.java.getDeclaredMethod(
                "dispatchActivityResult",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Intent::class.java,
            ).apply { isAccessible = true }.invoke(activity, requestCode, resultCode, data)
        }
    }
}
