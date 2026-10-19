package ru.ytkab0bp.slicebeam

import android.app.Application
import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.view.InputDevice
import android.view.MotionEvent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.FileProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.geometry.Offset
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.assertArrayEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import android.os.SystemClock
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import ru.ytkab0bp.slicebeam.components.bed_menu.FileMenu
import ru.ytkab0bp.slicebeam.components.bed_menu.ListBedMenu
import ru.ytkab0bp.slicebeam.components.bed_menu.CameraPreset
import ru.ytkab0bp.slicebeam.components.bed_menu.CameraPresetGeometry
import ru.ytkab0bp.slicebeam.components.bed_menu.CameraVector
import ru.ytkab0bp.slicebeam.ui.workspace.WorkspaceViewCubeGeometry
import ru.ytkab0bp.slicebeam.ui.workspace.WorkspaceViewFace
import ru.ytkab0bp.slicebeam.components.PreSliceFilamentDialog
import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.slic3r.Model
import ru.ytkab0bp.slicebeam.slic3r.GLModel
import ru.ytkab0bp.slicebeam.slic3r.PaintSession
import ru.ytkab0bp.slicebeam.render.GLRenderer
import ru.ytkab0bp.slicebeam.utils.FilamentSlot
import ru.ytkab0bp.slicebeam.slic3r.Slic3rConfigWrapper
import ru.ytkab0bp.slicebeam.ui.navigation.WorkspaceDestination
import ru.ytkab0bp.slicebeam.utils.Prefs
import ru.ytkab0bp.slicebeam.utils.Vec3d
import java.util.concurrent.atomic.AtomicReference

/** Reproduces the first Project tap after a cold workspace launch. */
@RunWith(AndroidJUnit4::class)
class InitialProjectMenuDeviceTest {
    @get:Rule
    val compose = createInitializedActivityRule()

    @Test
    fun compactViewCubeIsVisibleAccessibleAndAtLeast48Dp() {
        val cube = compose.onNodeWithTag("workspace-view-cube").assertIsDisplayed()
        val node = cube.fetchSemanticsNode()
        val printerActionDescription = listOf(
            R.string.PrinterOpenWebDescription,
            R.string.PrinterWebConfigureDescription,
        ).map(compose.activity::getString).firstOrNull { description ->
            compose.onAllNodesWithContentDescription(description).fetchSemanticsNodes().isNotEmpty()
        } ?: error("Printer header action should expose an accessibility description")
        val printerAction = compose.onNodeWithContentDescription(printerActionDescription)
            .assertIsDisplayed().fetchSemanticsNode()
        val screenRight = compose.activity.window.decorView.width.toFloat()
        assertTrue("Printer header action must remain on screen", printerAction.boundsInRoot.right <= screenRight)
        val density = compose.activity.resources.displayMetrics.density
        assertTrue("View cube must have a 48 dp touch target", node.size.width / density >= 48f)
        assertTrue("View cube must have a 48 dp touch target", node.size.height / density >= 48f)
        assertEquals(6, node.config[SemanticsActions.CustomActions].size)
    }

    @Test
    fun modelMenuSeparatesSelectedObjectAndWholePlateActions() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "model-menu-scope-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            loadSingleTestModel(bed, modelFile)
            requireNotNull(bed.glView).selectModel(0, Runnable {})
            instrumentationWaitForGlIdle()

            openSelectedObjectMenu(activity)
            compose.onNode(hasScrollToIndexAction()).performScrollToIndex(10)
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuToolbarMove))
                .performScrollTo().assertIsDisplayed()
            compose.onNode(hasScrollToIndexAction()).performScrollToIndex(1)
            compose.onNodeWithContentDescription(activity.getString(R.string.ModelMenuDuplicate))
                .performScrollTo().assertIsDisplayed()
            compose.onNodeWithContentDescription(activity.getString(R.string.ModelMenuArrange))
                .assertDoesNotExist()

            selectAllObjectsScope()
            compose.onNode(hasScrollToIndexAction()).performScrollToIndex(0)
            compose.onNodeWithContentDescription(activity.getString(R.string.ModelMenuArrange))
                .performScrollTo().assertIsDisplayed()
            compose.onNode(hasScrollToIndexAction()).performScrollToIndex(2)
            compose.onNodeWithContentDescription(activity.getString(R.string.ModelMenuSelectAll))
                .performScrollTo().assertIsDisplayed()
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuToolbarMove))
                .assertDoesNotExist()

            openEditMenu(activity)
            compose.onNodeWithContentDescription(activity.getString(R.string.ModelUndo))
                .performScrollTo().assertIsDisplayed()
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuToolbarMove))
                .assertDoesNotExist()
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun initialProjectRouteShowsFileActionsOnItsFirstOpen() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val controller = BedFragment::class.java.getDeclaredField("workspaceController")
            .apply { isAccessible = true }.get(bed)
        assertEquals(WorkspaceDestination.PROJECT.ordinal,
            controller.javaClass.getMethod("getSelectedDestinationSlot").invoke(controller))
        assertFalse(controller.javaClass.getMethod("getToolPanelExpanded").invoke(controller) as Boolean)

        val menuMap = BedFragment::class.java.getDeclaredField("menuMap")
            .apply { isAccessible = true }.get(bed) as android.util.SparseArray<*>
        val projectMenu = menuMap.get(WorkspaceDestination.PROJECT.ordinal)
        assertTrue(projectMenu is FileMenu)
        val boundFragment = ListBedMenu::class.java.getDeclaredField("fragment")
            .apply { isAccessible = true }.get(projectMenu)
        assertSame("the initially selected Project route must receive its BedFragment", bed, boundFragment)

        compose.onNodeWithContentDescription(activity.getString(R.string.MenuProject))
            .assertIsDisplayed()
            .performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText(activity.getString(R.string.WorkspaceAddModelAction))
                .fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNodeWithContentDescription(activity.getString(R.string.MenuProject)).assertIsDisplayed()
        compose.onNodeWithText(activity.getString(R.string.WorkspaceAddModelAction)).assertIsDisplayed()
        compose.onNodeWithText(activity.getString(R.string.MenuFileExport3mf)).assertIsDisplayed()
        assertTrue("the first Project tap should open its action panel",
            controller.javaClass.getMethod("getToolPanelExpanded").invoke(controller) as Boolean)
    }

    @Test
    fun projectMenuAddModelBuildsMultiSelectOpenDocumentRequest() {
        val activity = compose.activity
        val requestCode = MainActivity.REQUEST_CODE_OPEN_FILE
        val launchersField = MainActivity::class.java.getDeclaredField("activityResultLaunchers")
            .apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val launchers = launchersField.get(activity) as MutableMap<Int, ActivityResultLauncher<Intent>>
        val previousLauncher = requireNotNull(launchers[requestCode])
        val capturedIntent = java.util.concurrent.atomic.AtomicReference<Intent?>()
        launchers[requestCode] = object : ActivityResultLauncher<Intent>() {
            override val contract: ActivityResultContract<Intent, *> =
                ActivityResultContracts.StartActivityForResult()

            override fun launch(input: Intent, options: ActivityOptionsCompat?) {
                capturedIntent.set(Intent(input))
            }

            override fun unregister() = Unit
        }

        try {
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuProject))
                .assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithText(activity.getString(R.string.WorkspaceAddModelAction))
                    .fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText(activity.getString(R.string.WorkspaceAddModelAction))
                .assertIsDisplayed().performClick()

            val request = requireNotNull(capturedIntent.get())
            assertEquals(Intent.ACTION_OPEN_DOCUMENT, request.action)
            assertEquals("*/*", request.type)
            assertTrue(request.categories?.contains(Intent.CATEGORY_OPENABLE) == true)
            assertTrue("the project route must allow selecting multiple models",
                request.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false))
        } finally {
            launchers[requestCode] = previousLauncher
        }
    }

    @Test
    fun projectMenuImportsSelectedStlThroughTheActualResultHandler() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val inputFile = File(activity.cacheDir, "menu-import-${System.nanoTime()}.stl")
        val cachedImport = File(SliceBeam.getModelCacheDir(), inputFile.name)
        val requestCode = MainActivity.REQUEST_CODE_OPEN_FILE
        val launchersField = MainActivity::class.java.getDeclaredField("activityResultLaunchers")
            .apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val launchers = launchersField.get(activity) as MutableMap<Int, ActivityResultLauncher<Intent>>
        val previousLauncher = requireNotNull(launchers[requestCode])
        val capturedIntent = java.util.concurrent.atomic.AtomicReference<Intent?>()
        launchers[requestCode] = object : ActivityResultLauncher<Intent>() {
            override val contract: ActivityResultContract<Intent, *> =
                ActivityResultContracts.StartActivityForResult()

            override fun launch(input: Intent, options: ActivityOptionsCompat?) {
                capturedIntent.set(Intent(input))
            }

            override fun unregister() = Unit
        }
        @Suppress("DEPRECATION")
        val provider = activity.packageManager.getPackageInfo(
            activity.packageName,
            PackageManager.GET_PROVIDERS,
        ).providers.orEmpty().first { it.name == FileProvider::class.java.name }
        val authority = requireNotNull(provider.authority).substringBefore(';')
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                inputFile.outputStream().use(source::copyTo)
            }
            assertEquals(0, bed.currentModel?.objectsCount ?: 0)

            compose.onNodeWithContentDescription(activity.getString(R.string.MenuProject))
                .assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithText(activity.getString(R.string.WorkspaceAddModelAction))
                    .fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText(activity.getString(R.string.WorkspaceAddModelAction))
                .assertIsDisplayed().performClick()
            assertEquals(Intent.ACTION_OPEN_DOCUMENT, requireNotNull(capturedIntent.get()).action)

            val inputUri = FileProvider.getUriForFile(activity, authority, inputFile)
            activity.runOnUiThread {
                MainActivity::class.java.getDeclaredMethod(
                    "dispatchActivityResult", Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType, Intent::class.java,
                ).apply { isAccessible = true }
                    .invoke(activity, requestCode, android.app.Activity.RESULT_OK, Intent().setData(inputUri))
            }
            compose.waitUntil(timeoutMillis = 60_000) {
                bed.currentModel?.objectsCount == 1
            }
            assertEquals("the selected STL should become the active native model", 1,
                requireNotNull(bed.currentModel).objectsCount)
            assertTrue("the actual result handler should copy the selected file into the model cache",
                cachedImport.isFile && cachedImport.length() > 0L)
        } finally {
            launchers[requestCode] = previousLauncher
            inputFile.delete()
            cachedImport.delete()
        }
    }

    @Test
    fun projectMenuImportsMultipleSelectedStlsSequentially() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val firstFile = File(activity.cacheDir, "menu-multi-import-${System.nanoTime()}-a.stl")
        val secondFile = File(activity.cacheDir, "menu-multi-import-${System.nanoTime()}-b.stl")
        val cachedFiles = listOf(
            File(SliceBeam.getModelCacheDir(), firstFile.name),
            File(SliceBeam.getModelCacheDir(), secondFile.name),
        )
        val requestCode = MainActivity.REQUEST_CODE_OPEN_FILE
        val launchersField = MainActivity::class.java.getDeclaredField("activityResultLaunchers")
            .apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val launchers = launchersField.get(activity) as MutableMap<Int, ActivityResultLauncher<Intent>>
        val previousLauncher = requireNotNull(launchers[requestCode])
        val capturedIntent = java.util.concurrent.atomic.AtomicReference<Intent?>()
        launchers[requestCode] = object : ActivityResultLauncher<Intent>() {
            override val contract: ActivityResultContract<Intent, *> =
                ActivityResultContracts.StartActivityForResult()

            override fun launch(input: Intent, options: ActivityOptionsCompat?) {
                capturedIntent.set(Intent(input))
            }

            override fun unregister() = Unit
        }
        @Suppress("DEPRECATION")
        val provider = activity.packageManager.getPackageInfo(
            activity.packageName,
            PackageManager.GET_PROVIDERS,
        ).providers.orEmpty().first { it.name == FileProvider::class.java.name }
        val authority = requireNotNull(provider.authority).substringBefore(';')
        try {
            listOf(firstFile, secondFile).forEach { file ->
                activity.assets.open("models/xyz_cube.stl").use { source ->
                    file.outputStream().use(source::copyTo)
                }
            }
            assertEquals(0, bed.currentModel?.objectsCount ?: 0)

            compose.onNodeWithContentDescription(activity.getString(R.string.MenuProject))
                .assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithText(activity.getString(R.string.WorkspaceAddModelAction))
                    .fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText(activity.getString(R.string.WorkspaceAddModelAction))
                .assertIsDisplayed().performClick()
            assertTrue(requireNotNull(capturedIntent.get()).getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false))

            val firstUri = FileProvider.getUriForFile(activity, authority, firstFile)
            val secondUri = FileProvider.getUriForFile(activity, authority, secondFile)
            val clipData = android.content.ClipData.newUri(
                activity.contentResolver,
                "selected STL models",
                firstUri,
            ).apply { addItem(android.content.ClipData.Item(secondUri)) }
            val resultIntent = Intent().apply { setClipData(clipData) }
            activity.runOnUiThread {
                MainActivity::class.java.getDeclaredMethod(
                    "dispatchActivityResult", Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType, Intent::class.java,
                ).apply { isAccessible = true }
                    .invoke(activity, requestCode, android.app.Activity.RESULT_OK, resultIntent)
            }
            compose.waitUntil(timeoutMillis = 60_000) {
                bed.currentModel?.objectsCount == 2
            }
            assertEquals("both selected STLs should be imported in order", 2,
                requireNotNull(bed.currentModel).objectsCount)
            compose.waitUntil(timeoutMillis = 10_000) { cachedFiles.none(File::exists) }
            assertTrue("the sequential importer should remove each temporary cached STL after loading",
                cachedFiles.none(File::exists))
        } finally {
            launchers[requestCode] = previousLauncher
            firstFile.delete()
            secondFile.delete()
            cachedFiles.forEach(File::delete)
        }
    }

    @Test
    fun singlePlateProjectExportWritesReloadableThreeMfForTheActiveModel() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "single-plate-export-model-${System.nanoTime()}.stl")
        val output = File(activity.cacheDir, "single-plate-export-${System.nanoTime()}.3mf")
        val currentConfigFile = SliceBeam.getCurrentConfigFile()
        val previousCurrentConfig = currentConfigFile.takeIf(File::isFile)?.readBytes()
        val requestCode = MainActivity.REQUEST_CODE_EXPORT_3MF
        val launchersField = MainActivity::class.java.getDeclaredField("activityResultLaunchers")
            .apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val launchers = launchersField.get(activity) as MutableMap<Int, ActivityResultLauncher<Intent>>
        val previousLauncher = requireNotNull(launchers[requestCode])
        val capturedIntent = java.util.concurrent.atomic.AtomicReference<Intent?>()
        launchers[requestCode] = object : ActivityResultLauncher<Intent>() {
            override val contract: ActivityResultContract<Intent, *> =
                ActivityResultContracts.StartActivityForResult()

            override fun launch(input: Intent, options: ActivityOptionsCompat?) {
                capturedIntent.set(Intent(input))
            }

            override fun unregister() = Unit
        }
        @Suppress("DEPRECATION")
        val provider = activity.packageManager.getPackageInfo(
            activity.packageName,
            PackageManager.GET_PROVIDERS,
        ).providers.orEmpty().first { it.name == FileProvider::class.java.name }
        val authority = requireNotNull(provider.authority).substringBefore(';')
        val outputUri = FileProvider.getUriForFile(activity, authority, output)
        var model: Model? = null

        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            model = loadSingleTestModel(bed, modelFile)
            assertEquals(1, requireNotNull(model).objectsCount)

            compose.onNodeWithContentDescription(activity.getString(R.string.MenuProject))
                .assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithText(activity.getString(R.string.MenuFileExport3mf))
                    .fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText(activity.getString(R.string.MenuFileExport3mf))
                .assertIsEnabled().performClick()

            val cancelledRequest = requireNotNull(capturedIntent.get())
            assertEquals(Intent.ACTION_CREATE_DOCUMENT, cancelledRequest.action)
            assertEquals("application/3mf", cancelledRequest.type)
            assertEquals("OrkanSlicer_project.3mf", cancelledRequest.getStringExtra(Intent.EXTRA_TITLE))
            activity.runOnUiThread {
                MainActivity::class.java.getDeclaredMethod(
                    "dispatchActivityResult", Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType, Intent::class.java,
                ).apply { isAccessible = true }
                    .invoke(activity, requestCode, android.app.Activity.RESULT_CANCELED, Intent())
            }
            assertFalse("cancelling the save picker must not create an export file", output.exists())
            capturedIntent.set(null)

            compose.onNodeWithText(activity.getString(R.string.MenuFileExport3mf)).performClick()
            compose.waitUntil(timeoutMillis = 5_000) { capturedIntent.get() != null }
            activity.runOnUiThread {
                MainActivity::class.java.getDeclaredMethod(
                    "dispatchActivityResult", Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType, Intent::class.java,
                ).apply { isAccessible = true }
                    .invoke(activity, requestCode, android.app.Activity.RESULT_OK, Intent().setData(outputUri))
            }
            compose.waitUntil(timeoutMillis = 15_000) { output.isFile && output.length() > 0 }
            java.util.zip.ZipFile(output).use { archive ->
                assertTrue("the output must contain a 3MF model entry", archive.getEntry("3D/3dmodel.model") != null)
            }
            val reloaded = Model(output, 1)
            try {
                assertEquals("exported 3MF must reload the active plate's object", 1, reloaded.objectsCount)
            } finally {
                reloaded.release()
            }
        } finally {
            launchers[requestCode] = previousLauncher
            output.delete()
            modelFile.delete()
            model?.release()
            if (previousCurrentConfig == null) currentConfigFile.delete()
            else currentConfigFile.writeBytes(previousCurrentConfig)
        }
    }

    @Test
    fun sliceMenuManualActionOpensFilamentAssignmentAndCancelOrBackPreservesModel() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "slice-menu-filament-cancel-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val model = loadSingleTestModel(bed, modelFile)
            val originalExtruder = model.getExtruder(0)
            assertFalse("test fixture must start without a slice result", bed.hasSliceResult())

            compose.onNodeWithContentDescription(activity.getString(R.string.MenuSlice))
                .assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithText(activity.getString(R.string.MenuSliceCurrentSettings))
                    .fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText(activity.getString(R.string.QuickSliceTitle))
                .assertIsDisplayed().assertIsEnabled()
            compose.onNodeWithText(activity.getString(R.string.SmartSliceTitle))
                .assertIsDisplayed().assertIsEnabled()
            compose.onNodeWithText(activity.getString(R.string.MenuSliceCurrentSettings))
                .assertIsDisplayed().assertIsEnabled().performClick()

            compose.onNodeWithTag("pre-slice-title").assertIsDisplayed()
            compose.onNodeWithTag("pre-slice-cancel").performClick()
            compose.waitForIdle()
            compose.onNodeWithTag("pre-slice-title").assertDoesNotExist()
            assertEquals("Cancel must not alter the model's filament tool", originalExtruder, model.getExtruder(0))
            assertFalse("Cancel must not start native slicing", bed.hasSliceResult())

            compose.onNodeWithText(activity.getString(R.string.MenuSliceCurrentSettings))
                .assertIsDisplayed().performClick()
            compose.onNodeWithTag("pre-slice-title").assertIsDisplayed()
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
            compose.waitForIdle()
            compose.onNodeWithTag("pre-slice-title").assertDoesNotExist()
            assertEquals("Back must not alter the model's filament tool", originalExtruder, model.getExtruder(0))
            assertFalse("Back must not start native slicing", bed.hasSliceResult())
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun sliceMenuProducesResultAndExposesSafeResultActions() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "slice-menu-result-actions-test.stl")
        val prefs = requireNotNull(Prefs.getPrefs())
        val oldPerformanceMode = Prefs.isPerformanceModeEnabled()
        val oldGatePrinter = prefs.getString("mmu_choice_printer", null)
        val oldGateChoices = prefs.getString("mmu_choice_gates", null)
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val model = loadSingleTestModel(bed, modelFile)
            assertFalse("the fixture must start unsliced", bed.hasSliceResult())

            compose.onNodeWithContentDescription(activity.getString(R.string.MenuSlice))
                .assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithText(activity.getString(R.string.MenuSliceCurrentSettings))
                    .fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText(activity.getString(R.string.MenuSliceCurrentSettings))
                .assertIsDisplayed().assertIsEnabled().performClick()
            compose.onNodeWithTag("pre-slice-title").assertIsDisplayed()
            compose.onNodeWithTag("pre-slice-confirm").performClick()

            // The configured test printer may expose a cached physical-slot/material mismatch.
            val mismatchContinue = compose.onAllNodesWithText(
                activity.getString(R.string.PreSliceMaterialMismatchContinue),
            )
            if (mismatchContinue.fetchSemanticsNodes().isNotEmpty()) {
                mismatchContinue.get(0).assertIsDisplayed().performClick()
            }

            compose.waitUntil(timeoutMillis = 120_000) { bed.hasSliceResult() }
            assertTrue("native slicing must produce a non-empty G-code artifact",
                BedFragment.tempGCodePath.isFile && BedFragment.tempGCodePath.length() > 0L)
            val glView = requireNotNull(bed.glView)
            compose.waitUntil(timeoutMillis = 10_000) { glView.renderer.gcodeResult != null }
            val result = requireNotNull(glView.renderer.gcodeResult)
            compose.waitUntil(timeoutMillis = 10_000) { glView.renderer.viewer != null }
            assertTrue("sliced model should retain at least one assigned filament", model.getUsedFilaments(0).isNotEmpty())
            val viewer = requireNotNull(glView.renderer.viewer)
            val originalViewType = viewer.viewType
            val originalSeamsVisible = readGCodeOptionOnGl(
                glView,
                ru.ytkab0bp.slicebeam.slic3r.GCodeViewer.OPTION_TYPE_SEAMS,
            )

            // Slicing refreshes the same Slice menu from pre-slice actions to result actions.
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuSliceInfo))
                .assertIsDisplayed().performClick()
            compose.onNodeWithTag("slice-info-title").assertIsDisplayed()
            compose.onNodeWithTag("slice-info-scheme").assertIsDisplayed()
            compose.onNodeWithTag("slice-info-seams").assertIsDisplayed()
            val alternateScheme = activity.resources.getStringArray(R.array.GCodeColorSchemes).getOrNull(1)
            assertTrue("the result should offer multiple G-code color schemes", !alternateScheme.isNullOrBlank())
            compose.onNodeWithTag("slice-info-scheme").performClick()
            compose.onNodeWithText(requireNotNull(alternateScheme)).assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 10_000) { viewer.viewType != originalViewType }
            assertEquals("choosing a color scheme must update the rendered G-code view", 1, viewer.viewType)
            compose.onNodeWithTag("slice-info-seams").assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 5_000) {
                readGCodeOptionOnGl(glView, ru.ytkab0bp.slicebeam.slic3r.GCodeViewer.OPTION_TYPE_SEAMS) != originalSeamsVisible
            }
            compose.onNodeWithTag("slice-info-close").performClick()
            compose.waitUntil(timeoutMillis = 5_000) { !bed.isWorkspaceMessageDialogAttached() }

            compose.onNodeWithContentDescription(activity.getString(R.string.MenuSliceLayers))
                .assertIsDisplayed().performClick()
            compose.onNodeWithTag("slice-layers-title").assertIsDisplayed()
            compose.onNodeWithTag("slice-layers-range-title").assertIsDisplayed()
            compose.onNodeWithText("25").assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 5_000) { result.previewSelection?.blockSize == 25 }
            assertEquals("layer preview control must update the active G-code range policy", 25,
                result.previewSelection?.blockSize)
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
            compose.waitUntil(timeoutMillis = 5_000) { !bed.isWorkspaceMessageDialogAttached() }

            compose.onNodeWithContentDescription(activity.getString(R.string.MenuSlicePerformance))
                .assertIsDisplayed().performClick()
            assertEquals("performance result action must toggle the preference", !oldPerformanceMode,
                Prefs.isPerformanceModeEnabled())
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuSlicePerformance))
                .assertIsDisplayed().performClick()
            assertEquals("second toggle must restore the original preference", oldPerformanceMode,
                Prefs.isPerformanceModeEnabled())
            assertTrue("result actions must not replace or release the active G-code result",
                glView.renderer.gcodeResult === result)

            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val chooserMonitor = instrumentation.addMonitor(
                IntentFilter(Intent.ACTION_CHOOSER),
                ActivityResult(Activity.RESULT_CANCELED, Intent()),
                true,
            )
            try {
                compose.onNodeWithContentDescription(activity.getString(R.string.MenuSliceShare))
                    .assertIsDisplayed().assertIsEnabled().performClick()
                compose.waitUntil(timeoutMillis = 5_000) { chooserMonitor.hits > 0 }
                assertTrue("Share should route through the Android system chooser", chooserMonitor.hits > 0)
                assertTrue("opening Share must keep the generated G-code available", bed.hasSliceResult() &&
                    BedFragment.tempGCodePath.isFile && BedFragment.tempGCodePath.length() > 0L)
                assertTrue("opening Share must preserve the active rendered result",
                    glView.renderer.gcodeResult === result)
            } finally {
                instrumentation.removeMonitor(chooserMonitor)
            }
        } finally {
            Prefs.setPerformanceModeEnabled(oldPerformanceMode)
            prefs.edit().apply {
                if (oldGatePrinter == null) remove("mmu_choice_printer") else putString("mmu_choice_printer", oldGatePrinter)
                if (oldGateChoices == null) remove("mmu_choice_gates") else putString("mmu_choice_gates", oldGateChoices)
            }.commit()
            modelFile.delete()
        }
    }

    @Test
    fun sliceResultExportUsesDocumentPickerAndWritesTheGeneratedGcode() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "slice-result-export-model-${System.nanoTime()}.stl")
        val output = File(activity.cacheDir, "slice-result-export-${System.nanoTime()}.gcode")
        val requestCode = MainActivity.REQUEST_CODE_EXPORT_GCODE
        val launchersField = MainActivity::class.java.getDeclaredField("activityResultLaunchers")
            .apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val launchers = launchersField.get(activity) as MutableMap<Int, ActivityResultLauncher<Intent>>
        val previousLauncher = requireNotNull(launchers[requestCode])
        val capturedIntent = java.util.concurrent.atomic.AtomicReference<Intent?>()
        launchers[requestCode] = object : ActivityResultLauncher<Intent>() {
            override val contract: ActivityResultContract<Intent, *> =
                ActivityResultContracts.StartActivityForResult()

            override fun launch(input: Intent, options: ActivityOptionsCompat?) {
                capturedIntent.set(Intent(input))
            }

            override fun unregister() = Unit
        }
        @Suppress("DEPRECATION")
        val provider = activity.packageManager.getPackageInfo(
            activity.packageName,
            PackageManager.GET_PROVIDERS,
        ).providers.orEmpty().first { it.name == FileProvider::class.java.name }
        val authority = requireNotNull(provider.authority).substringBefore(';')
        val outputUri = FileProvider.getUriForFile(activity, authority, output)
        val prefs = requireNotNull(Prefs.getPrefs())
        val oldPerformanceMode = Prefs.isPerformanceModeEnabled()
        val oldGatePrinter = prefs.getString("mmu_choice_printer", null)
        val oldGateChoices = prefs.getString("mmu_choice_gates", null)

        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            loadSingleTestModel(bed, modelFile)
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuSlice))
                .assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithText(activity.getString(R.string.MenuSliceCurrentSettings))
                    .fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText(activity.getString(R.string.MenuSliceCurrentSettings))
                .assertIsDisplayed().performClick()
            compose.onNodeWithTag("pre-slice-confirm").performClick()

            val mismatchContinue = compose.onAllNodesWithText(
                activity.getString(R.string.PreSliceMaterialMismatchContinue),
            )
            if (mismatchContinue.fetchSemanticsNodes().isNotEmpty()) {
                mismatchContinue.get(0).assertIsDisplayed().performClick()
            }
            compose.waitUntil(timeoutMillis = 120_000) {
                bed.hasSliceResult() && BedFragment.tempGCodePath.isFile && BedFragment.tempGCodePath.length() > 0
            }

            compose.onNodeWithText(activity.getString(R.string.GCodeExportShort))
                .assertIsDisplayed().assertIsEnabled().performClick()
            val request = requireNotNull(capturedIntent.get())
            assertEquals(Intent.ACTION_CREATE_DOCUMENT, request.action)
            assertEquals("application/x-gcode", request.type)
            assertEquals(requireNotNull(bed.glView).renderer.gcodeResult!!.getRecommendedName(),
                request.getStringExtra(Intent.EXTRA_TITLE))

            activity.runOnUiThread {
                MainActivity::class.java.getDeclaredMethod(
                    "dispatchActivityResult", Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType, Intent::class.java,
                ).apply { isAccessible = true }
                    .invoke(activity, requestCode, android.app.Activity.RESULT_CANCELED, Intent())
            }
            assertFalse("canceling the G-code save must not create an output file", output.exists())

            capturedIntent.set(null)
            compose.onNodeWithText(activity.getString(R.string.GCodeExportShort))
                .assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 5_000) { capturedIntent.get() != null }
            activity.runOnUiThread {
                MainActivity::class.java.getDeclaredMethod(
                    "dispatchActivityResult", Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType, Intent::class.java,
                ).apply { isAccessible = true }
                    .invoke(activity, requestCode, android.app.Activity.RESULT_OK, Intent().setData(outputUri))
            }
            compose.waitUntil(timeoutMillis = 10_000) { output.isFile && output.length() > 0L }
            assertTrue("export must preserve the generated G-code bytes",
                output.readBytes().contentEquals(BedFragment.tempGCodePath.readBytes()))
        } finally {
            launchers[requestCode] = previousLauncher
            Prefs.setPerformanceModeEnabled(oldPerformanceMode)
            prefs.edit().apply {
                if (oldGatePrinter == null) remove("mmu_choice_printer") else putString("mmu_choice_printer", oldGatePrinter)
                if (oldGateChoices == null) remove("mmu_choice_gates") else putString("mmu_choice_gates", oldGateChoices)
            }.commit()
            output.delete()
            modelFile.delete()
        }
    }

    @Test
    fun multiPlateProjectExportWarnsAndCancelDoesNotOpenDocumentPicker() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "menu-multiplate-export-warning-test.stl")
        val output = File(activity.cacheDir, "menu-multiplate-export-warning-${System.nanoTime()}.3mf")
        val requestCode = MainActivity.REQUEST_CODE_EXPORT_3MF
        val launchersField = MainActivity::class.java.getDeclaredField("activityResultLaunchers")
            .apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val launchers = launchersField.get(activity) as MutableMap<Int, ActivityResultLauncher<Intent>>
        val previousLauncher = requireNotNull(launchers[requestCode])
        val capturedIntent = java.util.concurrent.atomic.AtomicReference<Intent?>()
        launchers[requestCode] = object : ActivityResultLauncher<Intent>() {
            override val contract: ActivityResultContract<Intent, *> =
                ActivityResultContracts.StartActivityForResult()

            override fun launch(input: Intent, options: ActivityOptionsCompat?) {
                capturedIntent.set(Intent(input))
            }

            override fun unregister() = Unit
        }
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            loadSingleTestModel(bed, modelFile)
            assertEquals(1, bed.plateCount)

            compose.onNodeWithContentDescription(activity.getString(R.string.PlateNumber, "1"))
                .assertIsDisplayed().performClick()
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuAddPlate))
                .assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 10_000) { bed.plateCount == 2 && bed.currentPlateIndex == 1 }
            compose.onNodeWithContentDescription(activity.getString(R.string.PlateNumber, "2"))
                .assertIsDisplayed().performClick()
            compose.onNodeWithContentDescription(
                activity.getString(R.string.PlatePickerSelectDescription, "1"),
            ).assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 10_000) { bed.currentPlateIndex == 0 }

            compose.onNodeWithContentDescription(activity.getString(R.string.MenuProject))
                .assertIsDisplayed().performClick()
            compose.onNodeWithText(activity.getString(R.string.MenuFileExport3mf))
                .assertIsDisplayed().assertIsEnabled().performClick()

            compose.onNodeWithText(activity.getString(R.string.Project3mfExportActivePlateTitle))
                .assertIsDisplayed()
            compose.onNodeWithText(
                activity.getString(R.string.Project3mfExportActivePlateMessage, 1, 2),
                substring = true,
            ).assertIsDisplayed()
            compose.onNodeWithText(activity.getString(R.string.Project3mfExportActivePlateConfirm))
                .assertIsDisplayed()
            compose.onNodeWithText(activity.getString(android.R.string.cancel))
                .assertIsDisplayed().performClick()
            compose.waitForIdle()

            compose.onNodeWithText(activity.getString(R.string.Project3mfExportActivePlateTitle))
                .assertDoesNotExist()
            assertEquals("cancel must preserve the two-plate workspace", 2, bed.plateCount)
            assertEquals("cancel must preserve the active plate", 0, bed.currentPlateIndex)
            assertEquals("cancel must not mutate the model", 1, bed.currentModel?.objectsCount)
            assertEquals("dismissing the warning must not launch the document picker", null, capturedIntent.get())

            compose.onNodeWithText(activity.getString(R.string.MenuFileExport3mf))
                .assertIsDisplayed().performClick()
            compose.onNodeWithText(activity.getString(R.string.Project3mfExportActivePlateConfirm))
                .assertIsDisplayed().performClick()

            val request = requireNotNull(capturedIntent.get())
            assertEquals(Intent.ACTION_CREATE_DOCUMENT, request.action)
            assertEquals("application/3mf", request.type)
            assertEquals("OrkanSlicer_plate_1.3mf", request.getStringExtra(Intent.EXTRA_TITLE))

            activity.runOnUiThread {
                MainActivity::class.java.getDeclaredMethod(
                    "dispatchActivityResult", Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType, Intent::class.java,
                ).apply { isAccessible = true }
                    .invoke(activity, requestCode, android.app.Activity.RESULT_CANCELED, Intent())
            }
            assertFalse("cancelling the document picker must not create an export file", output.exists())
            assertEquals("the confirmed export is limited to the selected plate", 2, bed.plateCount)
            assertEquals("the confirmed export must not switch the selected plate", 0, bed.currentPlateIndex)
            compose.onNodeWithContentDescription(activity.getString(R.string.PlateNumber, "1"))
                .assertIsDisplayed().performClick()
            compose.onNodeWithContentDescription(
                activity.getString(R.string.PlateCloseDescription,
                    activity.getString(R.string.PlateNumber, "2")),
            ).assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 10_000) {
                bed.plateCount == 1 && bed.currentPlateIndex == 0
            }
            assertEquals("closing the empty second plate must retain the model on the first plate",
                1, bed.currentModel?.objectsCount)
            compose.onNodeWithContentDescription(activity.getString(R.string.PlateNumber, "1"))
                .assertIsDisplayed()
        } finally {
            launchers[requestCode] = previousLauncher
            output.delete()
            modelFile.delete()
        }
    }

    @Test
    fun toolsMenuProfileExportActionOpensComposeFlowAndAndroidBackDismissesIt() {
        val activity = compose.activity
        compose.onNodeWithContentDescription(activity.getString(R.string.MenuOptions))
            .assertIsDisplayed()
            .performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText(activity.getString(R.string.MenuFileExportProfiles))
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(activity.getString(R.string.MenuFileExportProfiles))
            .assertIsDisplayed()
            .performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText(activity.getString(R.string.MenuFileExportProfilesPrints))
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(activity.getString(R.string.MenuFileExportProfilesPrints)).assertIsDisplayed()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        compose.onNodeWithText(activity.getString(R.string.MenuFileExportProfilesPrints)).assertDoesNotExist()
    }

    @Test
    fun toolsMenuProfileImportBuildsCorrectDocumentPickerRequest() {
        val activity = compose.activity
        val requestCode = MainActivity.REQUEST_CODE_IMPORT_PROFILES
        val launchersField = MainActivity::class.java.getDeclaredField("activityResultLaunchers")
            .apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val launchers = launchersField.get(activity) as MutableMap<Int, ActivityResultLauncher<Intent>>
        val previousLauncher = requireNotNull(launchers[requestCode])
        val capturedIntent = java.util.concurrent.atomic.AtomicReference<Intent?>()
        launchers[requestCode] = object : ActivityResultLauncher<Intent>() {
            override val contract: ActivityResultContract<Intent, *> =
                ActivityResultContracts.StartActivityForResult()

            override fun launch(input: Intent, options: ActivityOptionsCompat?) {
                capturedIntent.set(Intent(input))
            }

            override fun unregister() = Unit
        }
        try {
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuOptions))
                .assertIsDisplayed()
                .performClick()
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithText(activity.getString(R.string.MenuFileImportProfiles))
                    .fetchSemanticsNodes().isNotEmpty()
            }

            compose.onNodeWithText(activity.getString(R.string.MenuFileImportProfiles))
                .assertIsDisplayed()
                .performClick()
            val request = requireNotNull(capturedIntent.get())
            assertEquals(Intent.ACTION_OPEN_DOCUMENT, request.action)
            assertEquals("*/*", request.type)
            assertTrue(request.categories?.contains(Intent.CATEGORY_OPENABLE) == true)
            assertFalse(request.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false))
            assertTrue(request.getStringArrayExtra(Intent.EXTRA_MIME_TYPES)?.contains("text/plain") == true)
            compose.onNodeWithText(activity.getString(R.string.MenuFileImportProfiles)).assertIsDisplayed()
            assertFalse("opening the picker must not show an import dialog before a result is selected",
                (activity.navigationDelegate.getCurrentFragment() as BedFragment).isWorkspaceMessageDialogAttached())
        } finally {
            launchers[requestCode] = previousLauncher
        }
    }

    @Test
    fun toolsMenuProfileImportAppliesSelectedProfileAndRestoresFixtureState() {
        val activity = compose.activity
        val requestCode = MainActivity.REQUEST_CODE_IMPORT_PROFILES
        val launchersField = MainActivity::class.java.getDeclaredField("activityResultLaunchers")
            .apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val launchers = launchersField.get(activity) as MutableMap<Int, ActivityResultLauncher<Intent>>
        val previousLauncher = requireNotNull(launchers[requestCode])
        val capturedIntent = AtomicReference<Intent?>()
        val importedTitle = "Codex UI Import ${System.nanoTime()}"
        val input = File(activity.cacheDir, "codex-profile-import.ini")
        input.writeText("[print:$importedTitle]\nlayer_height = 0.17\n", StandardCharsets.UTF_8)

        val originalConfig = SliceBeam.requireConfig()
        val configSnapshot = Slic3rConfigWrapper(originalConfig.serialize().byteInputStream(StandardCharsets.UTF_8))
        val configFile = SliceBeam.getConfigFile()
        val persistedConfig = configFile.takeIf { it.isFile }?.readBytes()
        @Suppress("DEPRECATION")
        val provider = activity.packageManager.getPackageInfo(
            activity.packageName,
            PackageManager.GET_PROVIDERS,
        ).providers.orEmpty().first { it.name == FileProvider::class.java.name }
        val authority = requireNotNull(provider.authority).substringBefore(';')
        val inputUri = FileProvider.getUriForFile(activity, authority, input)

        launchers[requestCode] = object : ActivityResultLauncher<Intent>() {
            override val contract: ActivityResultContract<Intent, *> =
                ActivityResultContracts.StartActivityForResult()

            override fun launch(input: Intent, options: ActivityOptionsCompat?) {
                capturedIntent.set(Intent(input))
            }

            override fun unregister() = Unit
        }
        try {
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuOptions))
                .assertIsDisplayed().performClick()
            fun chooseImportFile() {
                capturedIntent.set(null)
                compose.onNodeWithText(activity.getString(R.string.MenuFileImportProfiles))
                    .assertIsDisplayed().performClick()
                val pickerIntent = requireNotNull(capturedIntent.get())
                assertEquals(Intent.ACTION_OPEN_DOCUMENT, pickerIntent.action)
                val result = Intent().setData(inputUri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                activity.runOnUiThread {
                    MainActivity::class.java.getDeclaredMethod(
                        "dispatchActivityResult", Int::class.javaPrimitiveType,
                        Int::class.javaPrimitiveType, Intent::class.java,
                    ).apply { isAccessible = true }
                        .invoke(activity, requestCode, Activity.RESULT_OK, result)
                }
            }

            assertTrue("profile must not exist before selecting the import result",
                SliceBeam.requireConfig().findPrint(importedTitle) == null)
            chooseImportFile()
            compose.waitUntil(timeoutMillis = 15_000) {
                compose.onAllNodesWithText(importedTitle).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText(activity.getString(R.string.MenuFileExportProfilesPrints))
                .assertIsDisplayed()
            compose.onNodeWithText(importedTitle).assertIsDisplayed()

            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithText(importedTitle).fetchSemanticsNodes().isEmpty()
            }
            assertTrue("Android Back must cancel profile import without mutation",
                SliceBeam.requireConfig().findPrint(importedTitle) == null)

            chooseImportFile()
            compose.waitUntil(timeoutMillis = 15_000) {
                compose.onAllNodesWithText(importedTitle).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText(activity.getString(android.R.string.cancel))
                .assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithText(importedTitle).fetchSemanticsNodes().isEmpty()
            }
            assertTrue("Cancel must discard the selected profile without mutation",
                SliceBeam.requireConfig().findPrint(importedTitle) == null)

            chooseImportFile()
            compose.waitUntil(timeoutMillis = 15_000) {
                compose.onAllNodesWithText(importedTitle).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText(activity.getString(android.R.string.ok)).assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 10_000) {
                SliceBeam.requireConfig().findPrint(importedTitle) != null
            }
            assertEquals("selected profile settings must be imported", "0.17",
                SliceBeam.requireConfig().findPrint(importedTitle)?.get("layer_height"))

            val existingProfileName = "0.16mm Standard"
            val existingProfile = requireNotNull(SliceBeam.requireConfig().findPrint(existingProfileName))
            val existingLayerHeight = existingProfile["layer_height"]
            assertTrue("the selected printer fixture should include the conflict target", existingLayerHeight != null)
            input.writeText("[print:$existingProfileName]\nlayer_height = 0.19\n", StandardCharsets.UTF_8)
            capturedIntent.set(null)
            compose.onNodeWithText(activity.getString(R.string.MenuFileImportProfiles))
                .assertIsDisplayed().performClick()
            assertEquals(Intent.ACTION_OPEN_DOCUMENT, requireNotNull(capturedIntent.get()).action)

            val conflictResult = Intent().setData(inputUri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            activity.runOnUiThread {
                MainActivity::class.java.getDeclaredMethod(
                    "dispatchActivityResult", Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType, Intent::class.java,
                ).apply { isAccessible = true }
                    .invoke(activity, requestCode, Activity.RESULT_OK, conflictResult)
            }
            val existingLabel = activity.getString(R.string.MenuFileImportProfilesExisting, existingProfileName)
            compose.waitUntil(timeoutMillis = 15_000) {
                compose.onAllNodesWithText(existingLabel).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText(existingLabel).assertIsDisplayed()
            compose.onNodeWithText(activity.getString(android.R.string.ok)).assertIsDisplayed().performClick()
            compose.onNodeWithText(activity.getString(R.string.MenuFileImportProfilesConflictTitle))
                .assertIsDisplayed()
            compose.onNodeWithText(activity.getString(R.string.MenuFileImportProfilesSkipExisting))
                .assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 10_000) {
                compose.onAllNodesWithText(activity.getString(R.string.MenuFileImportProfilesConflictTitle))
                    .fetchSemanticsNodes().isEmpty()
            }
            assertEquals("skip existing must keep the stored process value", existingLayerHeight,
                SliceBeam.requireConfig().findPrint(existingProfileName)?.get("layer_height"))

            input.writeText("[print:$existingProfileName]\nlayer_height = 0.19\n", StandardCharsets.UTF_8)
            capturedIntent.set(null)
            compose.onNodeWithText(activity.getString(R.string.MenuFileImportProfiles))
                .assertIsDisplayed().performClick()
            assertEquals(Intent.ACTION_OPEN_DOCUMENT, requireNotNull(capturedIntent.get()).action)
            val overwriteResult = Intent().setData(inputUri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            activity.runOnUiThread {
                MainActivity::class.java.getDeclaredMethod(
                    "dispatchActivityResult", Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType, Intent::class.java,
                ).apply { isAccessible = true }
                    .invoke(activity, requestCode, Activity.RESULT_OK, overwriteResult)
            }
            compose.waitUntil(timeoutMillis = 15_000) {
                compose.onAllNodesWithText(existingLabel).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText(activity.getString(android.R.string.ok)).assertIsDisplayed().performClick()
            compose.onNodeWithText(activity.getString(R.string.MenuFileImportProfilesConflictTitle))
                .assertIsDisplayed()
            compose.onNodeWithText(activity.getString(R.string.MenuFileImportProfilesOverwrite))
                .assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 10_000) {
                SliceBeam.requireConfig().findPrint(existingProfileName)?.get("layer_height") == "0.19"
            }
            assertEquals("overwrite must apply only after the explicit overwrite choice", "0.19",
                SliceBeam.requireConfig().findPrint(existingProfileName)?.get("layer_height"))
        } finally {
            launchers[requestCode] = previousLauncher
            SliceBeam.CONFIG = configSnapshot
            if (persistedConfig == null) configFile.delete() else configFile.writeBytes(persistedConfig)
            input.delete()
        }
    }

    @Test
    fun toolsMenuProfileExportWritesSelectedProfilesToTheRequestedDocument() {
        val activity = compose.activity
        val requestCode = MainActivity.REQUEST_CODE_EXPORT_PROFILES
        val launchersField = MainActivity::class.java.getDeclaredField("activityResultLaunchers")
            .apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val launchers = launchersField.get(activity) as MutableMap<Int, ActivityResultLauncher<Intent>>
        val previousLauncher = requireNotNull(launchers[requestCode])
        val capturedIntent = java.util.concurrent.atomic.AtomicReference<Intent?>()
        launchers[requestCode] = object : ActivityResultLauncher<Intent>() {
            override val contract: ActivityResultContract<Intent, *> =
                ActivityResultContracts.StartActivityForResult()

            override fun launch(input: Intent, options: ActivityOptionsCompat?) {
                capturedIntent.set(Intent(input))
            }

            override fun unregister() = Unit
        }

        val previousPrints = MainActivity.EXPORTING_PRINTS
        val previousFilaments = MainActivity.EXPORTING_FILAMENTS
        val previousPrinters = MainActivity.EXPORTING_PRINTERS
        val output = File(activity.cacheDir, "codex-profile-export-${System.nanoTime()}.ini")
        @Suppress("DEPRECATION")
        val provider = activity.packageManager.getPackageInfo(
            activity.packageName,
            PackageManager.GET_PROVIDERS,
        ).providers.orEmpty().first { it.name == FileProvider::class.java.name }
        val authority = requireNotNull(provider.authority).substringBefore(';')
        val outputUri = FileProvider.getUriForFile(activity, authority, output)

        try {
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuOptions))
                .assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithText(activity.getString(R.string.MenuFileExportProfiles))
                    .fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText(activity.getString(R.string.MenuFileExportProfiles)).performClick()
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithText(activity.getString(R.string.MenuFileExportProfilesPrints))
                    .fetchSemanticsNodes().isNotEmpty()
            }

            repeat(2) {
                compose.onNodeWithText(activity.getString(R.string.IntroNext)).performClick()
                compose.waitForIdle()
            }
            val exportLabels = compose.onAllNodesWithText(activity.getString(R.string.MenuFileExportProfiles))
            assertEquals("the open export dialog and workspace action should both have semantics", 2,
                exportLabels.fetchSemanticsNodes().size)
            exportLabels[1].performClick()
            compose.waitUntil(timeoutMillis = 5_000) {
                capturedIntent.get() != null
            }

            val request = requireNotNull(capturedIntent.get())
            assertEquals(Intent.ACTION_CREATE_DOCUMENT, request.action)
            assertEquals("application/ini", request.type)
            assertEquals("OrcaSlicerMobile_config_bundle.ini", request.getStringExtra(Intent.EXTRA_TITLE))
            assertTrue(MainActivity.EXPORTING_PRINTS.orEmpty().isNotEmpty())
            assertTrue(MainActivity.EXPORTING_FILAMENTS.orEmpty().isNotEmpty())
            assertTrue(MainActivity.EXPORTING_PRINTERS.orEmpty().isNotEmpty())
            val selectedPrintCount = MainActivity.EXPORTING_PRINTS.orEmpty().size
            val selectedFilamentCount = MainActivity.EXPORTING_FILAMENTS.orEmpty().size
            val selectedPrinterCount = MainActivity.EXPORTING_PRINTERS.orEmpty().size

            activity.runOnUiThread {
                MainActivity::class.java.getDeclaredMethod(
                    "dispatchActivityResult", Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType, Intent::class.java,
                ).apply { isAccessible = true }
                    .invoke(activity, requestCode, android.app.Activity.RESULT_OK, Intent().setData(outputUri))
            }
            compose.waitUntil(timeoutMillis = 10_000) { output.isFile && output.length() > 0 }
            val exported = Slic3rConfigWrapper(output)
            assertEquals("export must contain all selected print profiles",
                selectedPrintCount, exported.printConfigs.size)
            assertEquals("export must contain all selected filament profiles",
                selectedFilamentCount, exported.filamentConfigs.size)
            assertEquals("export must contain all selected printer profiles",
                selectedPrinterCount, exported.printerConfigs.size)
        } finally {
            launchers[requestCode] = previousLauncher
            MainActivity.EXPORTING_PRINTS = previousPrints
            MainActivity.EXPORTING_FILAMENTS = previousFilaments
            MainActivity.EXPORTING_PRINTERS = previousPrinters
            output.delete()
        }
    }

    @Test
    fun cameraMenuActionsUpdateProjectionAndCameraControlPreference() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val glView = requireNotNull(bed.glView)
        val previousProjection = Prefs.isOrthoProjectionEnabled()
        val previousControlMode = Prefs.getCameraControlMode()
        try {
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuCamera))
                .assertIsDisplayed()
                .performClick()
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithContentDescription(activity.getString(R.string.MenuCameraOrtho))
                    .fetchSemanticsNodes().isNotEmpty()
            }

            val projectionBefore = readProjectionMatrixOnGl(glView)
            val projectionAction = compose.onNodeWithContentDescription(
                activity.getString(R.string.MenuCameraOrtho),
            ).assertIsDisplayed()
            projectionAction.performClick()
            compose.waitUntil(timeoutMillis = 5_000) {
                val projectionAfter = readProjectionMatrixOnGl(glView)
                projectionAfter.indices.any { index ->
                    kotlin.math.abs(projectionAfter[index] - projectionBefore[index]) > 1e-9
                }
            }
            assertEquals("camera projection tile should update the persisted preference",
                !previousProjection, Prefs.isOrthoProjectionEnabled())

            compose.onNodeWithContentDescription(activity.getString(R.string.MenuCameraControlMode))
                .assertIsDisplayed()
                .performClick()
            val targetMode = (previousControlMode + 1) % 3
            val targetLabel = activity.getString(
                when (targetMode) {
                    0 -> R.string.MenuCameraControlModeOne
                    1 -> R.string.MenuCameraControlModeTwo
                    else -> R.string.MenuCameraControlModeThree
                },
            )
            compose.onNodeWithText(targetLabel, substring = true).assertIsDisplayed().performClick()
            compose.waitForIdle()
            assertEquals("camera control dialog selection should update its preference",
                targetMode, Prefs.getCameraControlMode())
        } finally {
            Prefs.setOrthoProjectionEnabled(previousProjection)
            Prefs.setCameraControlMode(previousControlMode)
            glView.queueEvent {
                glView.renderer.updateProjection()
                glView.requestRender()
            }
        }
    }

    @Test
    fun cameraPresetTilesUpdateTheRendererPoseForEveryPreset() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        compose.onNodeWithTag("workspace-view-cube").assertIsDisplayed()

        val glView = requireNotNull(bed.glView)
        val modelBed = glView.renderer.bed!!
        val min = requireNotNull(modelBed.getVolumeMin())
        val max = requireNotNull(modelBed.getVolumeMax())
        val minVector = CameraVector(min.x, min.y, min.z)
        val maxVector = CameraVector(max.x, max.y, max.z)
        val presets = listOf(
            R.string.ViewCubeTop to CameraPreset.TOP,
            R.string.ViewCubeBottom to CameraPreset.BOTTOM,
            R.string.ViewCubeFront to CameraPreset.FRONT,
            R.string.ViewCubeBack to CameraPreset.BACK,
            R.string.ViewCubeLeft to CameraPreset.LEFT,
            R.string.ViewCubeRight to CameraPreset.RIGHT,
        )
        presets.forEach { (faceLabel, preset) ->
            val target = CameraPresetGeometry.resolve(preset, minVector, maxVector)
            performViewCubeFace(faceLabel)
            compose.waitUntil(timeoutMillis = 5_000) {
                val pose = readCameraPoseOnGl(glView)
                kotlin.math.abs(pose[0] - target.origin.x) < 0.1 &&
                    kotlin.math.abs(pose[1] - target.origin.y) < 0.1 &&
                    kotlin.math.abs(pose[2] - target.origin.z) < 0.1 &&
                    kotlin.math.abs(pose[3] - target.position.x) < 0.1 &&
                    kotlin.math.abs(pose[4] - target.position.y) < 0.1 &&
                    kotlin.math.abs(pose[5] - target.position.z) < 0.1
            }
            // Let the renderer's spring animation settle before requesting the next preset.
            SystemClock.sleep(900L)
            compose.waitForIdle()
            val pose = readCameraPoseOnGl(glView)
            assertTrue(
                "${activity.getString(faceLabel)} must apply its configured renderer camera pose",
                kotlin.math.abs(pose[0] - target.origin.x) < 0.1 &&
                    kotlin.math.abs(pose[1] - target.origin.y) < 0.1 &&
                    kotlin.math.abs(pose[2] - target.origin.z) < 0.1 &&
                    kotlin.math.abs(pose[3] - target.position.x) < 0.1 &&
                    kotlin.math.abs(pose[4] - target.position.y) < 0.1 &&
                    kotlin.math.abs(pose[5] - target.position.z) < 0.1,
            )
        }
    }

    @Test
    fun rapidCameraPresetChangesKeepTheLatestSelection() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val glView = requireNotNull(bed.glView)
        val modelBed = requireNotNull(glView.renderer.bed)
        val min = requireNotNull(modelBed.getVolumeMin())
        val max = requireNotNull(modelBed.getVolumeMax())
        val target = CameraPresetGeometry.resolve(
            CameraPreset.RIGHT,
            CameraVector(min.x, min.y, min.z),
            CameraVector(max.x, max.y, max.z),
        )

        compose.onNodeWithTag("workspace-view-cube").assertIsDisplayed()
        performViewCubeFace(R.string.ViewCubeTop)
        performViewCubeFace(R.string.ViewCubeRight)

        compose.waitUntil(timeoutMillis = 5_000) {
            val pose = readCameraPoseOnGl(glView)
            kotlin.math.abs(pose[0] - target.origin.x) < 0.1 &&
                kotlin.math.abs(pose[1] - target.origin.y) < 0.1 &&
                kotlin.math.abs(pose[2] - target.origin.z) < 0.1 &&
                kotlin.math.abs(pose[3] - target.position.x) < 0.1 &&
                kotlin.math.abs(pose[4] - target.position.y) < 0.1 &&
                kotlin.math.abs(pose[5] - target.position.z) < 0.1
        }
        SystemClock.sleep(900L)
        val finalPose = readCameraPoseOnGl(glView)
        assertTrue(
            "a queued earlier preset must not replace the latest camera selection",
            kotlin.math.abs(finalPose[3] - target.position.x) < 0.1 &&
                kotlin.math.abs(finalPose[4] - target.position.y) < 0.1 &&
                kotlin.math.abs(finalPose[5] - target.position.z) < 0.1,
        )
    }

    @Test
    fun viewCubeFaceTapSelectsThatViewAndDragRotatesTheRendererCamera() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val glView = requireNotNull(bed.glView)
        val modelBed = requireNotNull(glView.renderer.bed)
        val min = requireNotNull(modelBed.getVolumeMin())
        val max = requireNotNull(modelBed.getVolumeMax())
        val target = CameraPresetGeometry.resolve(
            CameraPreset.FRONT,
            CameraVector(min.x, min.y, min.z),
            CameraVector(max.x, max.y, max.z),
        )
        val cube = compose.onNodeWithTag("workspace-view-cube").assertIsDisplayed()
        val size = cube.fetchSemanticsNode().size
        val polygons = WorkspaceViewCubeGeometry.project(
            size.width.toFloat(), size.height.toFloat(),
            Math.toRadians(-35.0).toFloat(), Math.toRadians(-25.0).toFloat(),
        )
        val front = requireNotNull(polygons.firstOrNull { it.face == WorkspaceViewFace.FRONT })
        val localX = front.points.map { it.x }.average().toFloat()
        val localY = front.points.map { it.y }.average().toFloat()
        assertEquals(WorkspaceViewFace.FRONT,
            WorkspaceViewCubeGeometry.hitTest(polygons, localX, localY))

        cube.performTouchInput {
            down(Offset(localX, localY))
            up()
        }
        compose.waitUntil(timeoutMillis = 5_000) {
            val pose = readCameraPoseOnGl(glView)
            kotlin.math.abs(pose[3] - target.position.x) < 0.1 &&
                kotlin.math.abs(pose[4] - target.position.y) < 0.1 &&
                kotlin.math.abs(pose[5] - target.position.z) < 0.1
        }
        SystemClock.sleep(900L)
        val beforeDrag = readCameraPoseOnGl(glView)
        cube.performTouchInput {
            val start = Offset(size.width * 0.5f, size.height * 0.5f)
            down(start)
            moveTo(Offset(size.width * 0.82f, size.height * 0.68f), delayMillis = 250)
            up()
        }
        compose.waitUntil(timeoutMillis = 5_000) {
            val pose = readCameraPoseOnGl(glView)
            (3..5).any { kotlin.math.abs(pose[it] - beforeDrag[it]) > 0.1 }
        }
    }

    private fun performViewCubeFace(faceLabelRes: Int) {
        val activity = compose.activity
        val label = activity.getString(
            R.string.ViewCubeShowFace,
            activity.getString(faceLabelRes),
        )
        val action = requireNotNull(
            compose.onNodeWithTag("workspace-view-cube")
                .fetchSemanticsNode().config[SemanticsActions.CustomActions]
                .firstOrNull { it.label == label },
        ) {
                "View cube should expose accessibility action '$label'"
        }
        compose.runOnIdle { action.action() }
    }

    @Test
    fun modelMenuDuplicateActionAppliesRequestedCopyCountToNativeModel() {
        val activity = compose.activity
        val context = activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(context.cacheDir, "menu-duplicate-action-test.stl")
        context.assets.open("models/xyz_cube.stl").use { source ->
            modelFile.outputStream().use(source::copyTo)
        }
        val loadDone = CountDownLatch(1)
        var loaded: Model? = null
        var loadError: Exception? = null
        bed.loadModel(modelFile, false, 1, object : BedFragment.ModelLoadCallback {
            override fun onLoaded(loadedModel: Model, firstNewObject: Int, addedObjects: Int) {
                loaded = loadedModel
                loadDone.countDown()
            }

            override fun onFailed(error: Exception) {
                loadError = error
                loadDone.countDown()
            }
        })
        assertTrue("temporary STL should load for the menu action", loadDone.await(60, TimeUnit.SECONDS))
        assertEquals("temporary STL should load without error: $loadError", 1, requireNotNull(loaded).objectsCount)

        val viewport = requireNotNull(bed.glView)
        viewport.selectModel(0, Runnable {})
        instrumentationWaitForGlIdle()
        compose.onNodeWithContentDescription(activity.getString(R.string.MenuModel))
            .assertIsDisplayed().performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText(activity.getString(R.string.ModelMenuDuplicateShort))
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(activity.getString(R.string.ModelMenuDuplicateShort))
            .assertIsDisplayed().assertIsEnabled().performClick()
        val duplicateCount = compose.onNodeWithTag("duplicate-model-count").assertIsDisplayed()
        duplicateCount.performTextClearance()
        duplicateCount.performTextInput("2")
        compose.onNodeWithTag("duplicate-model-confirm").performClick()
        compose.waitUntil(timeoutMillis = 15_000) {
            requireNotNull(bed.currentModel).objectsCount == 3
        }
        assertEquals("two requested copies should be added to the original", 3,
            requireNotNull(bed.currentModel).objectsCount)
        assertFalse("successful duplication should close its dialog", bed.isWorkspaceMessageDialogAttached())
        selectAllObjectsScope()
        compose.onNodeWithContentDescription(activity.getString(R.string.ModelMenuSelectAll))
            .assertIsDisplayed().assertIsEnabled().performClick()
        val selectedCount = CountDownLatch(1)
        val selectedObjects = AtomicReference<Int>()
        viewport.queueEvent {
            selectedObjects.set(viewport.renderer.selectedObjectsCount)
            selectedCount.countDown()
        }
        assertTrue("GL thread should report the select-all result", selectedCount.await(10, TimeUnit.SECONDS))
        assertEquals("Select All should select all three native models", 3, requireNotNull(selectedObjects.get()))
        modelFile.delete()
    }

    @Test
    fun modelMenuCenterOnBedRunsNativeCenteringForSelectedObject() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "menu-center-on-bed-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val model = loadSingleTestModel(bed, modelFile)
            val glView = requireNotNull(bed.glView)
            glView.selectModel(0, Runnable {})

            // Move only this disposable fixture away from the bed center before using the real menu action.
            glView.queueEvent {
                model.translate(0, 37.0, -23.0, 0.0)
                glView.requestRender()
            }
            instrumentationWaitForGlIdle()
            val translationBefore = readTranslationOnGl(glView, 0)

            openModelMenu(activity)
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithText(activity.getString(R.string.ModelMenuCenterOnBedShort))
                    .fetchSemanticsNodes().isNotEmpty()
            }
            val centerAction = compose.onNodeWithText(activity.getString(R.string.ModelMenuCenterOnBedShort))
                .performScrollTo().assertIsDisplayed().assertIsEnabled()
            centerAction.performClick()

            compose.waitUntil(timeoutMillis = 10_000) {
                val after = readTranslationOnGl(glView, 0)
                kotlin.math.abs(after.x - translationBefore.x) > 1.0 ||
                    kotlin.math.abs(after.y - translationBefore.y) > 1.0
            }
            val translationAfter = readTranslationOnGl(glView, 0)
            assertTrue(
                "Center on Bed must alter the selected disposable model's native placement",
                kotlin.math.abs(translationAfter.x - translationBefore.x) > 1.0 ||
                    kotlin.math.abs(translationAfter.y - translationBefore.y) > 1.0,
            )
            assertEquals("centering must not add or remove models", 1, readObjectCountOnGl(glView))
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun modelMenuFlattenToggleEntersAndLeavesRendererModeWithoutEditingModel() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "menu-flatten-toggle-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            loadSingleTestModel(bed, modelFile)
            val glView = requireNotNull(bed.glView)
            glView.selectModel(0, Runnable {})
            instrumentationWaitForGlIdle()
            val triangleCount = readTriangleCountOnGl(glView, 0)

            openSelectedObjectMenu(activity)
            compose.onNode(hasScrollToIndexAction()).performScrollToIndex(6)
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithText(activity.getString(R.string.ModelMenuFlattenShort))
                    .fetchSemanticsNodes().isNotEmpty()
            }
            val flatten = compose.onNodeWithText(activity.getString(R.string.ModelMenuFlattenShort))
                .performScrollTo().assertIsDisplayed().assertIsEnabled()
            flatten.performClick()
            compose.waitUntil(timeoutMillis = 5_000) {
                readRendererBooleanOnGl(glView, "isInFlattenMode")
            }
            assertTrue("Flatten action should activate the renderer's flatten interaction mode",
                readRendererBooleanOnGl(glView, "isInFlattenMode"))
            assertEquals("entering Flatten mode must not edit the mesh", triangleCount,
                readTriangleCountOnGl(glView, 0))

            compose.onNodeWithText(activity.getString(R.string.ModelMenuFlattenShort))
                .performScrollTo().performClick()
            compose.waitUntil(timeoutMillis = 5_000) {
                !readRendererBooleanOnGl(glView, "isInFlattenMode")
            }
            assertFalse("second Flatten action should leave the renderer interaction mode",
                readRendererBooleanOnGl(glView, "isInFlattenMode"))
            assertEquals("leaving Flatten mode must preserve the mesh", triangleCount,
                readTriangleCountOnGl(glView, 0))
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun modelMenuAutoOrientActionsRunForAllObjectsAndSelectedObject() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "menu-auto-orient-test.stl")
        try {
            activity.assets.open("models/pyramid.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val model = loadSingleTestModel(bed, modelFile)
            val glView = requireNotNull(bed.glView)
            glView.selectModel(0, Runnable {})
            val duplicateDone = CountDownLatch(1)
            glView.duplicateSelectedModel(1, Runnable { duplicateDone.countDown() })
            assertTrue("second temporary object should finish duplicating", duplicateDone.await(15, TimeUnit.SECONDS))
            instrumentationWaitForGlIdle()
            assertEquals(2, readObjectCountOnGl(glView))

            openModelMenu(activity)
            selectAllObjectsScope()
            val beforeOrientAll = model.geometryRevision
            compose.onNodeWithText(activity.getString(R.string.ModelMenuOrientAllShort))
                .performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()
            compose.waitUntil(timeoutMillis = 10_000) { model.geometryRevision >= beforeOrientAll + 2 }
            assertEquals("Orient All should process both workspace objects", beforeOrientAll + 2,
                model.geometryRevision)

            selectSingleObjectScope()
            glView.selectModel(0, Runnable {})
            instrumentationWaitForGlIdle()
            val beforeOrientSelected = model.geometryRevision
            compose.onNodeWithText(activity.getString(R.string.ModelMenuAutoOrientSelectedShort))
                .performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()
            compose.waitUntil(timeoutMillis = 10_000) { model.geometryRevision > beforeOrientSelected }
            assertEquals("Auto Orient Selected should process only the selected object", beforeOrientSelected + 1,
                model.geometryRevision)
            assertEquals("orientation actions must preserve the object count", 2, readObjectCountOnGl(glView))
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun modelMenuArrangeBringsOffBedObjectBackIntoWorkspace() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "menu-arrange-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val model = loadSingleTestModel(bed, modelFile)
            val glView = requireNotNull(bed.glView)
            glView.selectModel(0, Runnable {})
            glView.queueEvent {
                model.translate(0, 2_000.0, 2_000.0, 0.0)
                glView.requestRender()
            }
            instrumentationWaitForGlIdle()
            val before = readTranslationOnGl(glView, 0)
            assertTrue("test object should begin clearly outside a typical build volume",
                before.x > 1_000.0 && before.y > 1_000.0)

            openModelMenu(activity)
            selectAllObjectsScope()
            compose.onNodeWithText(activity.getString(R.string.ModelMenuArrangeShort))
                .performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()
            compose.waitUntil(timeoutMillis = 15_000) {
                val after = readTranslationOnGl(glView, 0)
                kotlin.math.abs(after.x - before.x) > 10.0 || kotlin.math.abs(after.y - before.y) > 10.0
            }
            val after = readTranslationOnGl(glView, 0)
            assertTrue("Arrange should change the off-bed XY placement",
                kotlin.math.abs(after.x - before.x) > 10.0 || kotlin.math.abs(after.y - before.y) > 10.0)
            assertEquals("Arrange must preserve the single temporary object", 1, readObjectCountOnGl(glView))
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun modelMenuFillBedCreatesCopiesWithinRendererObjectLimit() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "menu-fill-bed-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            loadSingleTestModel(bed, modelFile)
            val glView = requireNotNull(bed.glView)
            glView.selectModel(0, Runnable {})
            instrumentationWaitForGlIdle()

            openModelMenu(activity)
            compose.onNode(hasScrollToIndexAction()).performScrollToIndex(4)
            compose.onNodeWithText(activity.getString(R.string.ModelMenuFillBedShort))
                .performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()

            compose.waitUntil(timeoutMillis = 30_000) { readObjectCountOnGl(glView) > 1 }
            val resultingCount = readObjectCountOnGl(glView)
            assertTrue("Fill Bed should add at least one copy of the disposable test model", resultingCount > 1)
            assertTrue("Fill Bed must respect the renderer's hard object cap", resultingCount <= 256)
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun modelMenuMoveToPlateTransfersSelectedObjectBetweenWorkspacePlates() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "menu-move-to-plate-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            loadSingleTestModel(bed, modelFile)
            val glView = requireNotNull(bed.glView)
            glView.selectModel(0, Runnable {})
            assertEquals(1, bed.plateCount)

            compose.onNodeWithContentDescription(activity.getString(R.string.PlateNumber, "1"))
                .assertIsDisplayed().performClick()
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuAddPlate))
                .assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 10_000) {
                bed.plateCount == 2 && bed.currentPlateIndex == 1
            }
            compose.onNodeWithContentDescription(activity.getString(R.string.PlateNumber, "2"))
                .assertIsDisplayed().performClick()
            compose.onNodeWithContentDescription(
                activity.getString(R.string.PlatePickerSelectDescription, "1"),
            ).assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 10_000) {
                bed.currentPlateIndex == 0 && readObjectCountOnGl(glView) == 1
            }
            glView.selectModel(0, Runnable {})
            instrumentationWaitForGlIdle()

            openModelMenu(activity)
            compose.onNodeWithText(activity.getString(R.string.ModelMenuMoveToPlateShort))
                .performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()
            compose.onNodeWithText(activity.getString(R.string.PlateNumber, "2"))
                .assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 15_000) {
                bed.currentPlateIndex == 1 && readObjectCountOnGl(glView) == 1
            }
            assertEquals("moving the selected object should keep the workspace's two plates", 2, bed.plateCount)

            compose.onNodeWithContentDescription(activity.getString(R.string.PlateNumber, "2"))
                .assertIsDisplayed().performClick()
            compose.onNodeWithContentDescription(
                activity.getString(R.string.PlatePickerSelectDescription, "1"),
            ).assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 10_000) {
                bed.currentPlateIndex == 0 && readObjectCountOnGl(glView) == 0
            }
            assertEquals("the moved object must no longer exist on the source plate", 0,
                readObjectCountOnGl(glView))
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun modelMenuDeleteAllCancelAndBackPreserveTheLoadedModel() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "menu-delete-all-cancel-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            loadSingleTestModel(bed, modelFile)
            val glView = requireNotNull(bed.glView)
            assertEquals(1, readObjectCountOnGl(glView))
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuModel))
                .assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithText(activity.getString(R.string.ModelMenuObjectsShort))
                    .fetchSemanticsNodes().isNotEmpty()
            }
            selectAllObjectsScope()

            val deleteAll = compose.onNodeWithContentDescription(activity.getString(R.string.ModelMenuDeleteAll))
                .assertIsDisplayed().assertIsEnabled()
            deleteAll.performClick()
            compose.onNodeWithText(activity.getString(R.string.ModelDeleteAllConfirm)).assertIsDisplayed()
            compose.onNodeWithText(activity.getString(android.R.string.cancel)).assertIsDisplayed().performClick()
            compose.waitForIdle()
            assertFalse("cancel must dismiss the workspace confirmation", bed.isWorkspaceMessageDialogAttached())
            assertEquals("cancel must not delete the loaded model", 1, readObjectCountOnGl(glView))

            compose.onNodeWithContentDescription(activity.getString(R.string.ModelMenuDeleteAll)).performClick()
            compose.onNodeWithText(activity.getString(R.string.ModelDeleteAllConfirm)).assertIsDisplayed()
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
            compose.waitForIdle()
            assertFalse("Back must dismiss the workspace confirmation", bed.isWorkspaceMessageDialogAttached())
            assertEquals("Back must not delete the loaded model", 1, readObjectCountOnGl(glView))

            compose.onNodeWithContentDescription(activity.getString(R.string.ModelMenuDeleteAll)).performClick()
            compose.onNodeWithText(activity.getString(R.string.ModelDeleteAllConfirm)).assertIsDisplayed()
            compose.onNode(
                hasText(activity.getString(R.string.ModelMenuDeleteAll)) and hasClickAction(),
            ).assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 5_000) { !bed.isWorkspaceMessageDialogAttached() }
            val modelRemoved = CountDownLatch(1)
            val rendererIsEmpty = AtomicReference<Boolean>()
            glView.queueEvent {
                rendererIsEmpty.set(glView.renderer.model == null)
                modelRemoved.countDown()
            }
            assertTrue("GL thread should confirm delete-all completion", modelRemoved.await(10, TimeUnit.SECONDS))
            assertTrue("confirm should remove the model from the native renderer", requireNotNull(rendererIsEmpty.get()))
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun modelMenuDeleteSelectedCancelBackAndConfirmUseTheNativeSelection() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "menu-delete-selected-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            loadSingleTestModel(bed, modelFile)
            val glView = requireNotNull(bed.glView)
            glView.selectModel(0, Runnable {})
            instrumentationWaitForGlIdle()
            assertEquals("the disposable selected model should be loaded", 1, readObjectCountOnGl(glView))
            val controller = BedFragment::class.java.getDeclaredField("workspaceController")
                .apply { isAccessible = true }.get(bed)
            val modelSlot = WorkspaceDestination.MODEL.ordinal
            val shouldNavigate = controller.javaClass.getMethod("getSelectedDestinationSlot")
                .invoke(controller) != modelSlot
            val shouldExpand = !(controller.javaClass.getMethod("getToolPanelExpanded")
                .invoke(controller) as Boolean)
            if (shouldNavigate || shouldExpand) {
                compose.onNodeWithContentDescription(activity.getString(R.string.MenuModel))
                    .assertIsDisplayed().performClick()
            }
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithText(activity.getString(R.string.ModelMenuObjectsShort))
                    .fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNode(hasScrollToIndexAction()).performScrollToIndex(7)
            val deleteSelected = compose.onNodeWithContentDescription(
                activity.getString(R.string.ModelMenuDeleteSelected),
            ).assertIsDisplayed().assertIsEnabled()

            deleteSelected.performClick()
            compose.waitUntil(timeoutMillis = 5_000) { bed.isWorkspaceMessageDialogAttached() }
            compose.onNodeWithText(activity.getString(R.string.ModelDeleteConfirm)).assertIsDisplayed()
            compose.onNodeWithText(activity.getString(android.R.string.cancel)).performClick()
            compose.waitForIdle()
            assertFalse("cancel should dismiss the selected-model confirmation", bed.isWorkspaceMessageDialogAttached())
            assertEquals("cancel should preserve the selected model", 1, readObjectCountOnGl(glView))

            compose.onNodeWithContentDescription(activity.getString(R.string.ModelMenuDeleteSelected)).performClick()
            compose.onNodeWithText(activity.getString(R.string.ModelDeleteConfirm)).assertIsDisplayed()
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
            compose.waitForIdle()
            assertFalse("Back should dismiss the selected-model confirmation", bed.isWorkspaceMessageDialogAttached())
            assertEquals("Back should preserve the selected model", 1, readObjectCountOnGl(glView))

            compose.onNodeWithContentDescription(activity.getString(R.string.ModelMenuDeleteSelected)).performClick()
            compose.onNodeWithText(activity.getString(R.string.ModelDeleteConfirm)).assertIsDisplayed()
            compose.onNode(
                hasText(activity.getString(R.string.ModelMenuDeleteSelected)) and hasClickAction(),
            ).performClick()
            compose.waitUntil(timeoutMillis = 5_000) { !bed.isWorkspaceMessageDialogAttached() }
            val removed = CountDownLatch(1)
            val rendererIsEmpty = AtomicReference<Boolean>()
            glView.queueEvent {
                rendererIsEmpty.set(glView.renderer.model == null)
                removed.countDown()
            }
            assertTrue("GL thread should confirm selected-model deletion", removed.await(10, TimeUnit.SECONDS))
            assertTrue("confirm should delete only the selected model", requireNotNull(rendererIsEmpty.get()))
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun editScaleActionCancelsWithoutMutationAndAppliesOnlyAfterConfirmation() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "menu-scale-action-test.stl")
        val previousLinked = Prefs.isScaleLinked()
        val previousMillimeters = Prefs.isScaleInputInMM()
        Prefs.setScaleLinked(false)
        Prefs.setScaleInputInMM(false)
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val loadDone = CountDownLatch(1)
            val loaded = AtomicReference<Model?>()
            val loadError = AtomicReference<Exception?>()
            bed.loadModel(modelFile, false, 1, object : BedFragment.ModelLoadCallback {
                override fun onLoaded(loadedModel: Model, firstNewObject: Int, addedObjects: Int) {
                    loaded.set(loadedModel)
                    loadDone.countDown()
                }

                override fun onFailed(error: Exception) {
                    loadError.set(error)
                    loadDone.countDown()
                }
            })
            assertTrue("temporary scale model should finish loading", loadDone.await(60, TimeUnit.SECONDS))
            val model = requireNotNull(loaded.get()) { "temporary scale model failed: ${loadError.get()}" }
            assertEquals(1, model.objectsCount)
            val glView = requireNotNull(bed.glView)
            glView.selectModel(0, Runnable {})
            instrumentationWaitForGlIdle()

            openSelectedObjectMenu(activity)
            compose.onNode(hasScrollToIndexAction()).performScrollToIndex(12)
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuTransformScale))
                .assertIsDisplayed().performClick()
            compose.onNodeWithContentDescription("X, X =", substring = true).assertIsDisplayed()
            val original = readScaleOnGl(glView, 0)

            compose.onNodeWithContentDescription("X, X =", substring = true).performClick()
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithText(activity.getString(R.string.ScaleManualPercent))
                    .fetchSemanticsNodes().isNotEmpty()
            }
            val cancelledScaleField = compose.onNodeWithText(activity.getString(R.string.ScaleManualPercent))
            cancelledScaleField.performTextClearance()
            cancelledScaleField.performTextInput("150")
            compose.onNodeWithText(activity.getString(android.R.string.cancel)).performClick()
            compose.waitForIdle()
            val afterCancel = readScaleOnGl(glView, 0)
            assertEquals("cancelling the manual editor must preserve X", original.x, afterCancel.x, 0.0001)
            assertEquals("cancelling the manual editor must preserve Y", original.y, afterCancel.y, 0.0001)
            assertEquals("cancelling the manual editor must preserve Z", original.z, afterCancel.z, 0.0001)

            compose.onNodeWithContentDescription("X, X =", substring = true).performClick()
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithText(activity.getString(R.string.ScaleManualPercent))
                    .fetchSemanticsNodes().isNotEmpty()
            }
            val confirmedScaleField = compose.onNodeWithText(activity.getString(R.string.ScaleManualPercent))
            confirmedScaleField.performTextClearance()
            confirmedScaleField.performTextInput("125")
            compose.onNodeWithText(activity.getString(android.R.string.ok)).performClick()
            val afterApply = readScaleOnGl(glView, 0)
            assertEquals("confirming X=125% should scale only X", original.x * 1.25, afterApply.x, 0.002)
            assertEquals("unlinked X scaling should preserve Y", original.y, afterApply.y, 0.002)
            assertEquals("unlinked X scaling should preserve Z", original.z, afterApply.z, 0.002)

            compose.runOnUiThread { bed.undoModelEdit() }
            val afterUndo = readScaleOnGl(glView, 0)
            assertEquals("Undo should restore the pre-scale model", original.x, afterUndo.x, 0.002)
            compose.runOnUiThread { bed.redoModelEdit() }
            val afterRedo = readScaleOnGl(glView, 0)
            assertEquals("Redo should restore the confirmed scale", original.x * 1.25, afterRedo.x, 0.002)

            val workspaceController = BedFragment::class.java.getDeclaredField("workspaceController")
                .apply { isAccessible = true }.get(bed)
            assertTrue("workspace host should consume Back while the scale tool is active", bed.onBackPressed())
            compose.waitUntil(timeoutMillis = 5_000) {
                workspaceController.javaClass.getMethod("getActiveToolId").invoke(workspaceController) == null
            }
            assertEquals("Android Back should clear the active scale tool", null,
                workspaceController.javaClass.getMethod("getActiveToolId").invoke(workspaceController))
            assertEquals("closing the scale tool should preserve the Model destination",
                WorkspaceDestination.MODEL.ordinal,
                workspaceController.javaClass.getMethod("getSelectedDestinationSlot").invoke(workspaceController))
            assertEquals("closing the tool must keep the committed scale", original.x * 1.25,
                readScaleOnGl(glView, 0).x, 0.002)

            openSelectedObjectMenu(activity)
            compose.onNode(hasScrollToIndexAction()).performScrollToIndex(9)
            compose.onNodeWithContentDescription(activity.getString(R.string.ModelContextResetScale))
                .assertIsDisplayed().performClick()
            compose.waitForIdle()
            val afterReset = readScaleOnGl(glView, 0)
            assertEquals("Reset Scale should restore the original native X scale", original.x,
                afterReset.x, 0.002)
            compose.runOnUiThread { bed.undoModelEdit() }
            assertEquals("Undo should restore the explicitly confirmed scale after reset", original.x * 1.25,
                readScaleOnGl(glView, 0).x, 0.002)
            compose.runOnUiThread { bed.redoModelEdit() }
            assertEquals("Redo should restore the reset scale", original.x,
                readScaleOnGl(glView, 0).x, 0.002)
        } finally {
            Prefs.setScaleLinked(previousLinked)
            Prefs.setScaleInputInMM(previousMillimeters)
            modelFile.delete()
        }
    }

    @Test
    fun editPositionActionIsUndoableAndRedoableAfterConfirmation() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "menu-position-action-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val loaded = loadSingleTestModel(bed, modelFile)
            assertEquals(1, loaded.objectsCount)
            val glView = requireNotNull(bed.glView)
            glView.selectModel(0, Runnable {})
            instrumentationWaitForGlIdle()
            val original = readTranslationOnGl(glView, 0)

            openSelectedObjectMenu(activity)
            compose.onNode(hasScrollToIndexAction()).performScrollToIndex(10)
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuToolbarMove))
                .assertIsDisplayed().performClick()
            compose.onNodeWithText("X =", substring = true).assertIsDisplayed().performClick()
            val positionField = compose.onNodeWithText(activity.getString(R.string.OrientationManualValue))
            positionField.assertIsDisplayed()
            positionField.performTextClearance()
            positionField.performTextInput("12.5")
            compose.onNodeWithText(activity.getString(android.R.string.ok)).performClick()
            val moved = readTranslationOnGl(glView, 0)
            assertEquals("confirming position X should move the native object", 12.5, moved.x, 0.01)
            assertEquals("position editing should preserve Y", original.y, moved.y, 0.01)

            compose.runOnUiThread { bed.undoModelEdit() }
            val afterUndo = readTranslationOnGl(glView, 0)
            assertEquals("Undo should restore the original X position", original.x, afterUndo.x, 0.01)
            assertEquals("Undo should restore the original Y position", original.y, afterUndo.y, 0.01)
            compose.runOnUiThread { bed.redoModelEdit() }
            assertEquals("Redo should restore the confirmed X position", 12.5,
                readTranslationOnGl(glView, 0).x, 0.01)
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun editMeasureMenuActionOpensComposeModeAndAndroidBackClosesIt() {
        val activity = compose.activity
        dismissStartupChangelogIfVisible(activity)
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val glView = requireNotNull(bed.glView)
        val workspaceController = BedFragment::class.java.getDeclaredField("workspaceController")
            .apply { isAccessible = true }.get(bed)

        assertFalse("measurement mode should start disabled", glView.renderer.isInMeasureMode)
        openEditMenu(activity)
        compose.onNodeWithContentDescription(activity.getString(R.string.MenuToolbarMeasure))
            .assertIsDisplayed()
            .performClick()

        compose.waitUntil(timeoutMillis = 5_000) {
            workspaceController.javaClass.getMethod("getActiveToolId").invoke(workspaceController) == "measure-mode" &&
                compose.onAllNodesWithText(activity.getString(R.string.MeasureTitle))
                    .fetchSemanticsNodes().isNotEmpty() && glView.renderer.isInMeasureMode
        }
        compose.onNodeWithText(activity.getString(R.string.MeasureDistance)).assertIsDisplayed()
        assertSame("opening a GL tool must retain the workspace renderer view", glView, bed.glView)

        activity.onBackPressedDispatcher.onBackPressed()
        compose.waitUntil(timeoutMillis = 5_000) {
            workspaceController.javaClass.getMethod("getActiveToolId").invoke(workspaceController) == null &&
                !glView.renderer.isInMeasureMode
        }
            assertEquals("Back should leave the user in the Edit destination", WorkspaceDestination.EDIT.ordinal,
            workspaceController.javaClass.getMethod("getSelectedDestinationSlot").invoke(workspaceController))
        assertSame("closing the tool must retain GLView", glView, bed.glView)
        compose.onNodeWithText(activity.getString(R.string.MeasureTitle)).assertDoesNotExist()
    }

    @Test
    fun editRotationActionIsUndoableAndResetChangesNativeModel() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "menu-rotation-action-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val loaded = loadSingleTestModel(bed, modelFile)
            assertEquals(1, loaded.objectsCount)
            val glView = requireNotNull(bed.glView)
            glView.selectModel(0, Runnable {})
            instrumentationWaitForGlIdle()
            assertEquals("test object should be selected before rotation", 0, readSelectedObjectOnGl(glView))

            openSelectedObjectMenu(activity)
            compose.onNode(hasScrollToIndexAction()).performScrollToIndex(11)
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuToolbarRotate))
                .assertIsDisplayed().performClick()
            compose.onNodeWithText("X =", substring = true).assertIsDisplayed().performClick()
            val previewField = compose.onNodeWithText(activity.getString(R.string.OrientationManualValue))
            previewField.performTextClearance()
            previewField.performTextInput("45")
            compose.onNodeWithText(activity.getString(android.R.string.ok)).performClick()
            assertEquals("rotation editing should remain a preview until Apply", 0.0,
                readRotationOnGl(glView, 0).x, 0.002)
            val workspaceController = BedFragment::class.java.getDeclaredField("workspaceController")
                .apply { isAccessible = true }.get(bed)
            assertTrue("Workspace host should consume Back while the rotation tool is active", bed.onBackPressed())
            compose.waitUntil(timeoutMillis = 5_000) {
                workspaceController.javaClass.getMethod("getActiveToolId").invoke(workspaceController) == null
            }
            assertEquals("Back should discard the uncommitted rotation preview", 0.0,
                readRotationOnGl(glView, 0).x, 0.002)
            openSelectedObjectMenu(activity)
            compose.onNode(hasScrollToIndexAction()).performScrollToIndex(11)
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuToolbarRotate))
                .assertIsDisplayed().performClick()
            compose.onNodeWithText("X =", substring = true).assertIsDisplayed().performClick()
            val rotationField = compose.onNodeWithText(activity.getString(R.string.OrientationManualValue))
            rotationField.assertIsDisplayed()
            rotationField.performTextClearance()
            rotationField.performTextInput("90")
            compose.onNodeWithText(activity.getString(android.R.string.ok)).performClick()
            compose.onNodeWithText(activity.getString(R.string.MenuOrientationRotationApply))
                .assertIsDisplayed().performClick()
            val rotated = readRotationOnGl(glView, 0)
            assertEquals("confirmed X rotation should reach the native model", Math.PI / 2, rotated.x, 0.002)

            assertTrue("Back should close the rotation tool before history actions", bed.onBackPressed())
            compose.waitUntil(timeoutMillis = 5_000) {
                workspaceController.javaClass.getMethod("getActiveToolId").invoke(workspaceController) == null
            }
            openEditMenu(activity)
            compose.onNodeWithContentDescription(activity.getString(R.string.ModelUndo))
                .performScrollTo().assertIsDisplayed().performClick()
            assertEquals("Undo should restore the original X rotation", 0.0,
                readRotationOnGl(glView, 0).x, 0.002)
            compose.onNodeWithContentDescription(activity.getString(R.string.ModelRedo))
                .performScrollTo().assertIsDisplayed().performClick()
            assertEquals("Redo should restore the confirmed X rotation", Math.PI / 2,
                readRotationOnGl(glView, 0).x, 0.002)

            glView.selectModel(0, Runnable {})
            instrumentationWaitForGlIdle()
            assertEquals("test object should remain selected before reset", 0, readSelectedObjectOnGl(glView))
            assertEquals("history actions should leave no active tool", null,
                workspaceController.javaClass.getMethod("getActiveToolId").invoke(workspaceController))
            openSelectedObjectMenu(activity)
            compose.onNode(hasScrollToIndexAction()).performScrollToIndex(8)
            val snackbarControllerField = BedFragment::class.java.getDeclaredField("snackbarController")
                .apply { isAccessible = true }
            val snackbarController = snackbarControllerField.get(bed)
            snackbarController.javaClass.getMethod("clear").invoke(snackbarController)
            compose.waitForIdle()
            compose.onNodeWithContentDescription(activity.getString(R.string.ModelContextResetRotation))
                .assertIsDisplayed().assertIsEnabled().performClick()
            compose.waitForIdle()
            val snackbarEntries = snackbarController.javaClass.getMethod("getEntries").invoke(snackbarController) as List<*>
            val resetSnackbarShown = snackbarEntries.any { entry ->
                val snackbar = entry?.javaClass?.getMethod("getSnackbar")?.invoke(entry)
                snackbar?.javaClass?.getField("title")?.get(snackbar) ==
                    activity.getString(R.string.ModelContextResetRotationDone)
            }
            assertTrue("Reset Rotation action should execute its completion callback", resetSnackbarShown)
            val afterReset = readRotationOnGl(glView, 0)
            assertEquals("Reset Rotation should clear the native model rotation", 0.0, afterReset.x, 0.002)
            openEditMenu(activity)
            compose.onNodeWithContentDescription(activity.getString(R.string.ModelUndo))
                .performScrollTo().assertIsDisplayed().performClick()
            assertEquals("Undo should restore rotation after Reset Rotation", Math.PI / 2,
                readRotationOnGl(glView, 0).x, 0.002)
            compose.onNodeWithContentDescription(activity.getString(R.string.ModelRedo))
                .performScrollTo().assertIsDisplayed().performClick()
            assertEquals("Redo should reapply Reset Rotation", 0.0, readRotationOnGl(glView, 0).x, 0.002)
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun editCutAndVariableLayerActionsOpenAndCancelWithoutChangingModel() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "menu-cut-action-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val loaded = loadSingleTestModel(bed, modelFile)
            assertEquals(1, loaded.objectsCount)
            val glView = requireNotNull(bed.glView)
            glView.selectModel(0, Runnable {})
            instrumentationWaitForGlIdle()

            openSelectedObjectMenu(activity)
            compose.onNode(hasScrollToIndexAction()).performScrollToIndex(14)
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuTransformCut))
                .performScrollTo().assertIsDisplayed().performClick()
            compose.onNodeWithText(activity.getString(R.string.CutApply))
                .performScrollTo().assertIsDisplayed().assertIsEnabled()
            val workspaceController = BedFragment::class.java.getDeclaredField("workspaceController")
                .apply { isAccessible = true }.get(bed)
            assertEquals("Cut should be the active workspace tool",
                "ru.ytkab0bp.slicebeam.components.bed_menu.TransformCutMenu",
                workspaceController.javaClass.getMethod("getActiveToolId").invoke(workspaceController))
            assertEquals("opening Cut must not mutate the model", 1, readObjectCountOnGl(glView))
            assertTrue("Cut preview should be visible while the tool is open", readRendererBooleanOnGl(glView, "cutPlaneVisible"))

            assertTrue("Back should cancel and close the Cut tool", bed.onBackPressed())
            compose.waitUntil(timeoutMillis = 5_000) {
                workspaceController.javaClass.getMethod("getActiveToolId").invoke(workspaceController) == null
            }
            assertFalse("Back should hide the uncommitted cut preview", readRendererBooleanOnGl(glView, "cutPlaneVisible"))
            assertEquals("cancelling Cut must preserve the object", 1, readObjectCountOnGl(glView))

            openSelectedObjectMenu(activity)
            compose.onNode(hasScrollToIndexAction()).performScrollToIndex(16)
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuToolbarVariableLayerHeight))
                .performScrollTo().assertIsDisplayed().performClick()
            compose.onNodeWithText(activity.getString(R.string.VariableLayerTitle)).assertIsDisplayed()
            assertTrue("Variable Layer action should activate its workspace mode", bed.isVariableLayerHeightMode)
            compose.onNodeWithText(activity.getString(R.string.ActionDone)).assertIsDisplayed().performClick()
            compose.waitForIdle()
            assertFalse("Done should close Variable Layer mode", bed.isVariableLayerHeightMode)

            glView.selectModel(0, Runnable {})
            instrumentationWaitForGlIdle()
            openSelectedObjectMenu(activity)
            compose.onNode(hasScrollToIndexAction()).performScrollToIndex(15)
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuToolbarSplit))
                .performScrollTo().assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 5_000) {
                hasSnackbarTitle(bed, R.string.MenuToolbarSplitNothing)
            }
            assertEquals("Split on a single connected test mesh should preserve its object", 1,
                readObjectCountOnGl(glView))
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun applyingCutRunsOnGlThreadAndSplitsTheSelectedModel() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "menu-cut-apply-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val model = loadSingleTestModel(bed, modelFile)
            assertEquals(1, model.objectsCount)
            val glView = requireNotNull(bed.glView)
            glView.selectModel(0, Runnable {})
            instrumentationWaitForGlIdle()

            openSelectedObjectMenu(activity)
            compose.onNode(hasScrollToIndexAction()).performScrollToIndex(14)
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuTransformCut))
                .performScrollTo().assertIsDisplayed().performClick()
            compose.onNodeWithText(activity.getString(R.string.CutApply))
                .performScrollTo().assertIsDisplayed().performClick()

            compose.waitUntil(timeoutMillis = 30_000) {
                readObjectCountOnGl(glView) == 2 &&
                    !readRendererBooleanOnGl(glView, "cutPlaneVisible")
            }
            assertEquals("applying a cut should split the model into two native objects", 2,
                readObjectCountOnGl(glView))
            assertFalse("the cut preview should close after applying",
                readRendererBooleanOnGl(glView, "cutPlaneVisible"))
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun cutConnectorTapCreatesDiscardablePreviewWithoutMutatingModel() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "menu-cut-connector-cancel-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val loaded = loadSingleTestModel(bed, modelFile)
            assertEquals(1, loaded.objectsCount)
            val glView = requireNotNull(bed.glView)
            glView.selectModel(0, Runnable {})
            instrumentationWaitForGlIdle()

            openSelectedObjectMenu(activity)
            compose.onNode(hasScrollToIndexAction()).performScrollToIndex(14)
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuTransformCut))
                .performScrollTo().assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 10_000) {
                runCatching {
                    compose.onNodeWithText(activity.getString(R.string.CutApply)).assertIsEnabled()
                }.isSuccess
            }
            compose.onNodeWithText(activity.getString(R.string.CutAddConnector))
                .performScrollTo().assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 10_000) {
                readRendererBooleanOnGl(glView, "cutConnectorPlacement")
            }

            tapGlViewCenter(glView)
            compose.waitUntil(timeoutMillis = 10_000) {
                readCutConnectorCountOnGl(glView, 0) == 1
            }
            assertEquals("placing a cut connector should only create a preview", 1,
                readObjectCountOnGl(glView))

            assertTrue("Back should close the cut tool", bed.onBackPressed())
            compose.waitUntil(timeoutMillis = 10_000) {
                !readRendererBooleanOnGl(glView, "cutPlaneVisible") &&
                    readCutConnectorCountOnGl(glView, 0) == 0
            }
            assertEquals("cancelling the connector preview must preserve the model", 1,
                readObjectCountOnGl(glView))
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun editSplitActionSeparatesDisconnectedMeshComponentsIntoNativeObjects() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "menu-split-disconnected-mesh-${System.nanoTime()}.stl")
        try {
            writeTwoDisconnectedCubesStl(activity, modelFile)
            val loaded = loadSingleTestModel(bed, modelFile)
            assertEquals("the importer should keep the compound STL as one selectable model", 1,
                loaded.objectsCount)
            val importedTriangleCount = loaded.getTriangleCount(0)
            assertTrue("both components should contain valid mesh triangles",
                importedTriangleCount > 0 && importedTriangleCount % 2 == 0)
            val trianglesPerComponent = importedTriangleCount / 2

            val glView = requireNotNull(bed.glView)
            glView.selectModel(0, Runnable {})
            instrumentationWaitForGlIdle()
            openSelectedObjectMenu(activity)
            compose.onNode(hasScrollToIndexAction()).performScrollToIndex(15)
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuToolbarSplit))
                .assertIsDisplayed().assertIsEnabled().performClick()

            compose.waitUntil(timeoutMillis = 30_000) { readObjectCountOnGl(glView) == 2 }
            assertEquals("native split should create exactly two independent model objects", 2,
                readObjectCountOnGl(glView))
            assertEquals("first split object should preserve its triangles", trianglesPerComponent,
                readTriangleCountOnGl(glView, 0))
            assertEquals("second split object should preserve its triangles", trianglesPerComponent,
                readTriangleCountOnGl(glView, 1))
        } finally {
            modelFile.delete()
        }
    }

    @Test
    fun editEmbossActionEntersComposeGlModeAndBackExitsWithoutMeshMutation() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "menu-emboss-action-test.stl")
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val loaded = loadSingleTestModel(bed, modelFile)
            assertEquals(1, loaded.objectsCount)
            val glView = requireNotNull(bed.glView)
            glView.selectModel(0, Runnable {})
            instrumentationWaitForGlIdle()
            val trianglesBefore = readTriangleCountOnGl(glView, 0)

            openSelectedObjectMenu(activity)
            compose.onNode(hasScrollToIndexAction()).performScrollToIndex(21)
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuToolbarEmboss))
                .performScrollTo().assertIsDisplayed().performClick()
            compose.onNodeWithText(activity.getString(R.string.EmbossTitle)).assertIsDisplayed()
            compose.onNodeWithText(activity.getString(R.string.EmbossInstruction)).assertIsDisplayed()

            val workspaceController = BedFragment::class.java.getDeclaredField("workspaceController")
                .apply { isAccessible = true }.get(bed)
            assertEquals("Emboss should be the active workspace tool", "emboss-mode",
                workspaceController.javaClass.getMethod("getActiveToolId").invoke(workspaceController))
            assertTrue("Opening the menu action should activate renderer emboss mode",
                readRendererBooleanOnGl(glView, "isInEmbossMode"))
            assertEquals("Entering emboss mode must not mutate the mesh", trianglesBefore,
                readTriangleCountOnGl(glView, 0))

            assertTrue("Android Back should close the active emboss tool", bed.onBackPressed())
            compose.waitUntil(timeoutMillis = 5_000) {
                workspaceController.javaClass.getMethod("getActiveToolId").invoke(workspaceController) == null
            }
            assertFalse("Back must leave renderer emboss mode", readRendererBooleanOnGl(glView, "isInEmbossMode"))
            assertFalse("Back must clear the active emboss tool", bed.isEmbossMode)
            assertEquals("Cancelling the emboss panel must preserve the mesh", trianglesBefore,
                readTriangleCountOnGl(glView, 0))
            assertTrue("Closing the tool must retain the existing GLView", bed.glView === glView)
        } finally {
            if (bed.isEmbossMode) bed.onBackPressed()
            modelFile.delete()
        }
    }

    @Test
    fun editPaintActionsOpenTheirGlModesAndBackCancelsWithoutMeshMutation() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "menu-paint-actions-test.stl")
        val previousSlots = Prefs.getStoredFilamentSlots().map { FilamentSlot(it.color, it.type) }
        Prefs.setStoredFilamentSlots(listOf(
            FilamentSlot(0xFFCC3333.toInt(), "UI Paint Base"),
            FilamentSlot(0xFF3366CC.toInt(), "UI Paint Accent"),
        ))
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val model = loadSingleTestModel(bed, modelFile)
            assertEquals(1, model.objectsCount)
            val glView = requireNotNull(bed.glView)
            glView.selectModel(0, Runnable {})
            instrumentationWaitForGlIdle()
            val trianglesBefore = readTriangleCountOnGl(glView, 0)
            assertFalse("The fixture starts without paint overrides", readModelHasPaintOnGl(glView, 0))

            val paintActions = listOf(
                Triple(R.string.MenuToolbarSupportPaint, GLRenderer.PAINT_MODE_SUPPORT, 17),
                Triple(R.string.MenuToolbarSeamPaint, GLRenderer.PAINT_MODE_SEAM, 18),
                Triple(R.string.MenuToolbarFuzzyPaint, GLRenderer.PAINT_MODE_FUZZY, 19),
                Triple(R.string.MenuToolbarColorPaint, GLRenderer.PAINT_MODE_COLOR, 20),
            )
            paintActions.forEach { (labelRes, expectedMode, index) ->
                openSelectedObjectMenu(activity)
                compose.onNode(hasScrollToIndexAction()).performScrollToIndex(index)
                compose.onNodeWithContentDescription(activity.getString(labelRes))
                    .performScrollTo().assertIsDisplayed().performClick()
                compose.onNodeWithText(activity.getString(R.string.PaintWith)).assertIsDisplayed()
                compose.waitUntil(timeoutMillis = 10_000) {
                    bed.isPaintMode && readRendererBooleanOnGl(glView, "isPaintMode")
                }
                assertEquals("The selected menu action must activate its matching GL paint mode",
                    expectedMode, readRendererIntOnGl(glView, "paintModeKind"))
                assertFalse("Opening or cancelling paint must not add model paint",
                    readModelHasPaintOnGl(glView, 0))

                assertTrue("Android Back should cancel the active paint tool", bed.onBackPressed())
                compose.waitUntil(timeoutMillis = 10_000) { !bed.isPaintMode }
                compose.waitUntil(timeoutMillis = 10_000) {
                    !readRendererBooleanOnGl(glView, "isPaintMode")
                }
                assertFalse("Cancellation should preserve the original unpainted mesh",
                    readModelHasPaintOnGl(glView, 0))
                assertEquals("Cancellation should preserve mesh topology", trianglesBefore,
                    readTriangleCountOnGl(glView, 0))
                assertTrue("Paint tool exit should retain the renderer view", bed.glView === glView)
            }

            openSelectedObjectMenu(activity)
            compose.onNode(hasScrollToIndexAction()).performScrollToIndex(20)
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuToolbarColorPaint))
                .performScrollTo().assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 10_000) {
                bed.isPaintMode && readRendererBooleanOnGl(glView, "isPaintMode")
            }
            compose.onNodeWithText(activity.getString(R.string.PaintBaseFilament)).assertIsDisplayed()
            compose.onAllNodesWithContentDescription(
                activity.getString(R.string.FilamentPaletteSlotDescription, 2, "UI Paint Accent")
            ).get(0).assertIsDisplayed().performClick()
            assertArrayEquals("a base-filament choice remains a draft until confirmed", intArrayOf(1),
                readUsedFilamentsOnGl(glView, 0))
            compose.onNodeWithTag("workspace-mode-panel").performTouchInput { swipeUp() }
            compose.onNodeWithText(activity.getString(android.R.string.cancel)).performScrollTo().assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 10_000) { !bed.isPaintMode }
            instrumentationWaitForGlIdle()
            assertArrayEquals(
                "cancelling a paint session must restore the model's original base filament",
                intArrayOf(1), readUsedFilamentsOnGl(glView, 0),
            )

            openSelectedObjectMenu(activity)
            compose.onNode(hasScrollToIndexAction()).performScrollToIndex(20)
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuToolbarColorPaint))
                .performScrollTo().assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 10_000) {
                bed.isPaintMode && readRendererBooleanOnGl(glView, "isPaintMode")
            }
            compose.onNodeWithText(activity.getString(R.string.PaintBaseFilament)).assertIsDisplayed()
            compose.onAllNodesWithContentDescription(
                activity.getString(R.string.FilamentPaletteSlotDescription, 2, "UI Paint Accent")
            ).get(0).assertIsDisplayed().performClick()
            assertArrayEquals("choosing a base filament must not mutate the model before Done", intArrayOf(1),
                readUsedFilamentsOnGl(glView, 0))
            compose.onNodeWithTag("workspace-mode-panel").performTouchInput { swipeUp() }
            compose.onNodeWithText(activity.getString(R.string.ActionDone))
                .performScrollTo().assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 10_000) { !bed.isPaintMode }
            instrumentationWaitForGlIdle()
            assertArrayEquals("the confirmed color-panel base-filament action should update the native model",
                intArrayOf(2), readUsedFilamentsOnGl(glView, 0))
            assertFalse("changing the base filament should not fabricate facet paint",
                readModelHasPaintOnGl(glView, 0))
            assertEquals("changing the base filament must preserve mesh topology", trianglesBefore,
                readTriangleCountOnGl(glView, 0))
        } finally {
            if (bed.isPaintMode) bed.onBackPressed()
            modelFile.delete()
            Prefs.setStoredFilamentSlots(previousSlots)
        }
    }

    @Test
    fun colorPaintPanelAcceptsRealCanvasTouchAndUndoRestoresFacetAssignments() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "menu-paint-touch-test.stl")
        val previousSlots = Prefs.getStoredFilamentSlots().map { FilamentSlot(it.color, it.type) }
        Prefs.setStoredFilamentSlots(listOf(
            FilamentSlot(0xFFCC3333.toInt(), "Touch Paint Base"),
            FilamentSlot(0xFF3366CC.toInt(), "Touch Paint Accent"),
        ))
        try {
            activity.assets.open("models/xyz_cube.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val model = loadSingleTestModel(bed, modelFile)
            val glView = requireNotNull(bed.glView)
            glView.selectModel(0, Runnable {})
            instrumentationWaitForGlIdle()
            assertFalse("paint touch fixture starts without facet assignments", readModelHasPaintOnGl(glView, 0))
            openSelectedObjectMenu(activity)
            compose.onNode(hasScrollToIndexAction()).performScrollToIndex(20)
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuToolbarColorPaint))
                .performScrollTo().assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 10_000) {
                bed.isPaintMode && readRendererBooleanOnGl(glView, "isPaintMode")
            }
            compose.onAllNodesWithContentDescription(
                activity.getString(R.string.FilamentPaletteSlotDescription, 2, "Touch Paint Accent")
            ).get(1).assertIsDisplayed().performClick()

            val point = findUncoveredPreciseSurfacePoint(glView, bed)
            injectCanvasStroke(point.first, point.second)
            val previewTriangles = readPaintPreviewTriangleCountOnGl(glView, 2)
            assertTrue("an injected touch on a precise GL surface hit must create a paint preview; point=$point",
                previewTriangles > 0)
            assertFalse("painting remains a preview until Done", readModelHasPaintOnGl(glView, 0))

            compose.onNodeWithTag("workspace-mode-panel").performTouchInput { swipeUp() }
            compose.onNodeWithText(activity.getString(R.string.ActionDone))
                .performScrollTo().assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 10_000) { !bed.isPaintMode }
            compose.waitUntil(timeoutMillis = 10_000) { readModelHasPaintOnGl(glView, 0) }
            val usedAfterPaint = readUsedFilamentsOnGl(glView, 0)
            assertTrue("the actual canvas stroke must assign the selected second filament: ${usedAfterPaint.contentToString()}",
                usedAfterPaint.contains(2))
            assertTrue("the committed color brush must produce painted facets",
                readPaintMaxFilamentOnGl(glView, 0) >= 2)

            openEditMenu(activity)
            compose.onNodeWithContentDescription(activity.getString(R.string.ModelUndo))
                .performScrollTo().assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 10_000) { !readModelHasPaintOnGl(glView, 0) }
            assertArrayEquals("Undo restores the original base filament", intArrayOf(1), readUsedFilamentsOnGl(glView, 0))
            compose.onNodeWithContentDescription(activity.getString(R.string.ModelRedo))
                .performScrollTo().assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 10_000) { readModelHasPaintOnGl(glView, 0) }
            assertTrue("Redo restores the committed painted filament", readUsedFilamentsOnGl(glView, 0).contains(2))
        } finally {
            if (bed.isPaintMode) bed.onBackPressed()
            modelFile.delete()
            Prefs.setStoredFilamentSlots(previousSlots)
        }
    }

    @Test
    fun editSimplifyRequiresConfirmationAndCanBeUndoneAndRedone() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "menu-simplify-action-test.stl")
        try {
            activity.assets.open("models/sphere.stl").use { source ->
                modelFile.outputStream().use(source::copyTo)
            }
            val loaded = loadSingleTestModel(bed, modelFile)
            assertEquals(1, loaded.objectsCount)
            val glView = requireNotNull(bed.glView)
            glView.selectModel(0, Runnable {})
            instrumentationWaitForGlIdle()
            val originalTriangles = readTriangleCountOnGl(glView, 0)
            assertTrue("sphere fixture must be large enough for simplification", originalTriangles > 4)

            openSelectedObjectMenu(activity)
            compose.onNode(hasScrollToIndexAction()).performScrollToIndex(13)
            compose.onNodeWithContentDescription(activity.getString(R.string.ModelSimplify))
                .performScrollTo().assertIsDisplayed().performClick()
            compose.onNodeWithText(activity.getString(android.R.string.cancel)).assertIsDisplayed().performClick()
            assertEquals("cancelling simplify must preserve the model", originalTriangles,
                readTriangleCountOnGl(glView, 0))

            openSelectedObjectMenu(activity)
            compose.onNode(hasScrollToIndexAction()).performScrollToIndex(13)
            compose.onNodeWithContentDescription(activity.getString(R.string.ModelSimplify))
                .performScrollTo().assertIsDisplayed().performClick()
            val startAction = compose.onAllNodesWithText(activity.getString(R.string.ModelSimplifyStart))
            val startNodeIndex = startAction.fetchSemanticsNodes().lastIndex
            assertTrue("simplify confirmation button should be present", startNodeIndex >= 0)
            startAction.get(startNodeIndex).assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 30_000) {
                readTriangleCountOnGl(glView, 0) < originalTriangles
            }
            val simplifiedTriangles = readTriangleCountOnGl(glView, 0)
            assertTrue("simplify should produce a smaller native mesh", simplifiedTriangles in 4 until originalTriangles)

            compose.runOnUiThread { bed.undoModelEdit() }
            assertEquals("Undo should restore the imported mesh", originalTriangles,
                readTriangleCountOnGl(requireNotNull(bed.glView), 0))
            compose.runOnUiThread { bed.redoModelEdit() }
            assertEquals("Redo should restore the simplified mesh", simplifiedTriangles,
                readTriangleCountOnGl(requireNotNull(bed.glView), 0))
        } finally {
            modelFile.delete()
        }
    }

    private fun loadSingleTestModel(bed: BedFragment, modelFile: File): Model {
        val done = CountDownLatch(1)
        val loaded = AtomicReference<Model?>()
        val error = AtomicReference<Exception?>()
        bed.loadModel(modelFile, false, 1, object : BedFragment.ModelLoadCallback {
            override fun onLoaded(loadedModel: Model, firstNewObject: Int, addedObjects: Int) {
                loaded.set(loadedModel)
                done.countDown()
            }

            override fun onFailed(failure: Exception) {
                error.set(failure)
                done.countDown()
            }
        })
        assertTrue("temporary test model should finish loading", done.await(60, TimeUnit.SECONDS))
        return requireNotNull(loaded.get()) { "temporary model failed: ${error.get()}" }
    }

    /** Creates a standard binary STL containing two closed copies separated by 30 mm on X. */
    private fun writeTwoDisconnectedCubesStl(activity: MainActivity, destination: File) {
        val source = activity.assets.open("models/xyz_cube.stl").use { it.readBytes() }
        assertTrue("bundled STL must have a binary STL header", source.size >= 84)
        val triangleCount = ByteBuffer.wrap(source, 80, 4).order(ByteOrder.LITTLE_ENDIAN).int
        assertTrue("bundled STL triangle count must match its binary payload", triangleCount > 0 &&
            source.size == 84 + triangleCount * 50)

        val output = ByteBuffer.allocate(84 + triangleCount * 2 * 50).order(ByteOrder.LITTLE_ENDIAN)
        output.put(source, 0, 80)
        output.putInt(triangleCount * 2)
        output.put(source, 84, triangleCount * 50)
        repeat(triangleCount) { triangleIndex ->
            val triangleOffset = 84 + triangleIndex * 50
            val translated = source.copyOfRange(triangleOffset, triangleOffset + 50)
            val record = ByteBuffer.wrap(translated).order(ByteOrder.LITTLE_ENDIAN)
            listOf(12, 24, 36).forEach { vertexXOffset ->
                record.putFloat(vertexXOffset, record.getFloat(vertexXOffset) + 30f)
            }
            output.put(translated)
        }
        destination.writeBytes(output.array())
    }

    private fun openEditMenu(activity: MainActivity) {
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val controller = BedFragment::class.java.getDeclaredField("workspaceController")
            .apply { isAccessible = true }.get(bed)
        compose.waitUntil(timeoutMillis = 5_000) {
            !(controller.javaClass.getMethod("isNavigationTransitionInProgress").invoke(controller) as Boolean)
        }
        val editSlot = WorkspaceDestination.EDIT.ordinal
        val shouldNavigate = controller.javaClass.getMethod("getSelectedDestinationSlot")
            .invoke(controller) != editSlot
        val shouldExpand = !(controller.javaClass.getMethod("getToolPanelExpanded").invoke(controller) as Boolean)
        if (shouldNavigate || shouldExpand) {
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuToolbarNavigation))
                .assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 5_000) {
                controller.javaClass.getMethod("getSelectedDestinationSlot").invoke(controller) == editSlot &&
                    controller.javaClass.getMethod("getToolPanelExpanded").invoke(controller) == true &&
                    !(controller.javaClass.getMethod("isNavigationTransitionInProgress").invoke(controller) as Boolean)
            }
        }
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(1)
    }

    private fun dismissStartupChangelogIfVisible(activity: MainActivity) {
        if (compose.onAllNodesWithText(activity.getString(R.string.Changelog))
                .fetchSemanticsNodes().isNotEmpty()
        ) {
            compose.onNodeWithText(activity.getString(R.string.ChangelogOK)).assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithText(activity.getString(R.string.Changelog))
                    .fetchSemanticsNodes().isEmpty()
            }
        }
    }

    private fun openModelMenu(activity: MainActivity) {
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val controller = BedFragment::class.java.getDeclaredField("workspaceController")
            .apply { isAccessible = true }.get(bed)
        compose.waitUntil(timeoutMillis = 5_000) {
            !(controller.javaClass.getMethod("isNavigationTransitionInProgress").invoke(controller) as Boolean)
        }
        val modelSlot = WorkspaceDestination.MODEL.ordinal
        val shouldNavigate = controller.javaClass.getMethod("getSelectedDestinationSlot")
            .invoke(controller) != modelSlot
        val shouldExpand = !(controller.javaClass.getMethod("getToolPanelExpanded").invoke(controller) as Boolean)
        if (shouldNavigate || shouldExpand) {
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuModel))
                .assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 5_000) {
                controller.javaClass.getMethod("getSelectedDestinationSlot").invoke(controller) == modelSlot &&
                    controller.javaClass.getMethod("getToolPanelExpanded").invoke(controller) == true &&
                    !(controller.javaClass.getMethod("isNavigationTransitionInProgress").invoke(controller) as Boolean)
            }
        }
    }

    private fun selectAllObjectsScope() {
        compose.onNodeWithTag("model-menu-scope-all").performClick()
        compose.waitForIdle()
    }

    private fun selectSingleObjectScope() {
        compose.onNodeWithTag("model-menu-scope-object").performClick()
        compose.waitForIdle()
    }

    private fun openSelectedObjectMenu(activity: MainActivity) {
        openModelMenu(activity)
        selectSingleObjectScope()
    }

    @Test
    fun modelObjectsMenuAssignmentOverridesImportedThreeMfMaterial() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val file = File(activity.cacheDir, "menu-filament-assignment-test.3mf")
        InstrumentationRegistry.getInstrumentation().context.assets.open("models/two-color-project.3mf").use { input ->
            file.outputStream().use { input.copyTo(it) }
        }
        val previousSlots = Prefs.getStoredFilamentSlots().map { FilamentSlot(it.color, it.type) }
        val testSlots = listOf(
            FilamentSlot(0xFFCC3333.toInt(), "UI Test PLA"),
            FilamentSlot(0xFF3366CC.toInt(), "UI Test PETG"),
            FilamentSlot(0xFF33AA66.toInt(), "UI Test ABS"),
        )
        Prefs.setStoredFilamentSlots(testSlots)
        try {
            val loadedLatch = CountDownLatch(1)
            var loaded: Model? = null
            var loadFailure: Exception? = null
            bed.loadModel(file, true, 1, object : BedFragment.ModelLoadCallback {
                override fun onLoaded(loadedModel: Model, firstNewObject: Int, addedObjects: Int) {
                    loaded = loadedModel
                    loadedLatch.countDown()
                }

                override fun onFailed(error: Exception) {
                    loadFailure = error
                    loadedLatch.countDown()
                }
            })
            assertTrue("color 3MF fixture should load", loadedLatch.await(60, TimeUnit.SECONDS))
            assertEquals("color fixture load failure: $loadFailure", 2, requireNotNull(loaded).objectsCount)
            assertTrue("fixture begins with imported tool 1", loaded!!.getUsedFilaments(0).contentEquals(intArrayOf(1)))

            compose.onNodeWithContentDescription(activity.getString(R.string.MenuModel)).performClick()
            compose.onNodeWithText(activity.getString(R.string.ModelMenuObjectsShort)).assertIsDisplayed().performClick()
            compose.onAllNodesWithText(activity.getString(R.string.ObjectActionAssignFilamentShort))[0]
                .assertIsDisplayed().performClick()
            val targetLabel = activity.getString(
                R.string.ModelContextFilamentSlot,
                2,
                "UI Test ABS",
                String.format(java.util.Locale.ROOT, "#%06X", testSlots[2].color and 0xFFFFFF),
            )
            compose.onNodeWithText(targetLabel).assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 10_000) {
                bed.currentModel?.getUsedFilaments(0)?.contentEquals(intArrayOf(3)) == true
            }
            assertArrayEquals("explicit Model-menu T2 assignment replaces imported 3MF T0",
                intArrayOf(3), requireNotNull(bed.currentModel).getUsedFilaments(0))
            assertArrayEquals("unselected second object keeps its imported T1 assignment",
                intArrayOf(2), requireNotNull(bed.currentModel).getUsedFilaments(1))
            assertFalse("uniform reassignment removes old per-volume paint overrides",
                requireNotNull(bed.currentModel).hasPaint(0))
        } finally {
            Prefs.setStoredFilamentSlots(previousSlots)
            file.delete()
        }
    }

    @Test
    fun preSliceCanChooseDifferentSlotsForEachObject() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val file = File(activity.cacheDir, "pre-slice-per-object-assignment.3mf")
        InstrumentationRegistry.getInstrumentation().context.assets.open("models/two-color-project.3mf").use { input ->
            file.outputStream().use { input.copyTo(it) }
        }
        val previousSlots = Prefs.getStoredFilamentSlots().map { FilamentSlot(it.color, it.type) }
        val testSlots = listOf(
            FilamentSlot(0xFFCC3333.toInt(), "Test PLA"),
            FilamentSlot(0xFF3366CC.toInt(), "Test PETG"),
            FilamentSlot(0xFF33AA66.toInt(), "Test ABS"),
            FilamentSlot(0xFFFFCC22.toInt(), "Test TPU"),
        )
        Prefs.setStoredFilamentSlots(testSlots)
        try {
            val loadedLatch = CountDownLatch(1)
            val loadFailure = AtomicReference<Exception?>()
            bed.loadModel(file, true, 1, object : BedFragment.ModelLoadCallback {
                override fun onLoaded(loadedModel: Model, firstNewObject: Int, addedObjects: Int) {
                    loadedLatch.countDown()
                }

                override fun onFailed(error: Exception) {
                    loadFailure.set(error)
                    loadedLatch.countDown()
                }
            })
            assertTrue("two-color project import should finish", loadedLatch.await(60, TimeUnit.SECONDS))
            assertEquals("two-color project import failure: ${loadFailure.get()}", 2,
                requireNotNull(bed.currentModel).objectsCount)

            val selectedSlots = AtomicReference<IntArray?>()
            PreSliceFilamentDialog.show(
                bed,
                requireNotNull(bed.currentModel),
                PreSliceFilamentDialog.Callback { selectedSlots.set(it) },
            )
            compose.onNodeWithTag("pre-slice-title").assertIsDisplayed()
            compose.onNodeWithText(activity.getString(R.string.PreSliceIndividualMode)).performClick()

            // Object 1 -> slot 4/T3, object 2 -> slot 2/T1.
            compose.onNodeWithTag("pre-slice-object-slot-0").performScrollTo().performClick()
            compose.onNodeWithTag("pre-slice-object-0-filament-3").performClick()
            compose.waitForIdle()
            compose.onNodeWithTag("pre-slice-object-slot-1").performScrollTo().performClick()
            compose.onNodeWithTag("pre-slice-object-1-filament-1").performClick()
            compose.onNodeWithTag("pre-slice-confirm").performClick()
            assertArrayEquals("first object maps to slot 4", intArrayOf(3, 1), selectedSlots.get())
            assertArrayEquals("choosing assignments is staged until the user confirms the slice",
                intArrayOf(1), requireNotNull(bed.currentModel).getUsedFilaments(0))
        } finally {
            Prefs.setStoredFilamentSlots(previousSlots)
            file.delete()
        }
    }

    private fun instrumentationWaitForGlIdle() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        SystemClock.sleep(250L)
    }

    private fun readGCodeOptionOnGl(glView: ru.ytkab0bp.slicebeam.view.GLView, option: Int): Boolean {
        val finished = CountDownLatch(1)
        val visible = AtomicReference<Boolean>()
        glView.queueEvent {
            visible.set(requireNotNull(glView.renderer.viewer).isOptionVisible(option))
            finished.countDown()
        }
        assertTrue("GL thread did not return G-code option visibility", finished.await(10, TimeUnit.SECONDS))
        return requireNotNull(visible.get())
    }

    private fun readScaleOnGl(glView: ru.ytkab0bp.slicebeam.view.GLView, index: Int): Vec3d {
        val finished = CountDownLatch(1)
        val scale = AtomicReference<Vec3d?>()
        val failure = AtomicReference<Throwable?>()
        glView.queueEvent {
            try {
                val model = requireNotNull(glView.renderer.model)
                check(model.pointer != 0L) { "Renderer returned a released native model" }
                check(index in 0 until model.objectsCount) {
                    "Renderer model has ${model.objectsCount} objects; requested scale for index $index"
                }
                scale.set(Vec3d().also { model.getScale(index, it) })
            } catch (error: Throwable) {
                failure.set(error)
            } finally {
                finished.countDown()
            }
        }
        assertTrue("GL thread did not return model scale", finished.await(10, TimeUnit.SECONDS))
        failure.get()?.let { throw AssertionError("Could not read model scale on GL thread", it) }
        return requireNotNull(scale.get())
    }

    private fun readTranslationOnGl(glView: ru.ytkab0bp.slicebeam.view.GLView, index: Int): Vec3d =
        readVectorOnGl(glView, index) { model, vector -> model.getTranslation(index, vector) }

    private fun readRotationOnGl(glView: ru.ytkab0bp.slicebeam.view.GLView, index: Int): Vec3d =
        readVectorOnGl(glView, index) { model, vector -> model.getRotation(index, vector) }

    private fun readSelectedObjectOnGl(glView: ru.ytkab0bp.slicebeam.view.GLView): Int {
        val finished = CountDownLatch(1)
        val selected = AtomicReference<Int>()
        glView.queueEvent {
            selected.set(glView.renderer.selectedObject)
            finished.countDown()
        }
        assertTrue("GL thread did not return selected object", finished.await(10, TimeUnit.SECONDS))
        return requireNotNull(selected.get())
    }

    private fun readObjectCountOnGl(glView: ru.ytkab0bp.slicebeam.view.GLView): Int {
        val finished = CountDownLatch(1)
        val count = AtomicReference<Int>()
        glView.queueEvent {
            count.set(requireNotNull(glView.renderer.model).objectsCount)
            finished.countDown()
        }
        assertTrue("GL thread did not return model object count", finished.await(10, TimeUnit.SECONDS))
        return requireNotNull(count.get())
    }

    private fun readCutConnectorCountOnGl(glView: ru.ytkab0bp.slicebeam.view.GLView, index: Int): Int {
        val finished = CountDownLatch(1)
        val count = AtomicReference<Int>()
        glView.queueEvent {
            count.set(glView.renderer.getCutConnectorCount(index))
            finished.countDown()
        }
        assertTrue("GL thread did not return cut connector count", finished.await(10, TimeUnit.SECONDS))
        return requireNotNull(count.get())
    }

    private fun tapGlViewCenter(glView: ru.ytkab0bp.slicebeam.view.GLView) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val downTime = SystemClock.uptimeMillis()
            val x = glView.width / 2f
            val y = glView.height / 2f
            val down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0)
            val up = MotionEvent.obtain(downTime, downTime + 80L, MotionEvent.ACTION_UP, x, y, 0)
            try {
                glView.dispatchTouchEvent(down)
                glView.dispatchTouchEvent(up)
            } finally {
                down.recycle()
                up.recycle()
            }
        }
    }

    private fun readTriangleCountOnGl(glView: ru.ytkab0bp.slicebeam.view.GLView, index: Int): Int {
        val finished = CountDownLatch(1)
        val count = AtomicReference<Int>()
        glView.queueEvent {
            count.set(requireNotNull(glView.renderer.model).getTriangleCount(index))
            finished.countDown()
        }
        assertTrue("GL thread did not return native triangle count", finished.await(10, TimeUnit.SECONDS))
        return requireNotNull(count.get())
    }

    private fun readRendererBooleanOnGl(glView: ru.ytkab0bp.slicebeam.view.GLView, fieldName: String): Boolean {
        val finished = CountDownLatch(1)
        val value = AtomicReference<Boolean>()
        glView.queueEvent {
            val field = glView.renderer.javaClass.getDeclaredField(fieldName).apply { isAccessible = true }
            value.set(field.getBoolean(glView.renderer))
            finished.countDown()
        }
        assertTrue("GL thread did not return renderer state $fieldName", finished.await(10, TimeUnit.SECONDS))
        return requireNotNull(value.get())
    }

    private fun readProjectionMatrixOnGl(glView: ru.ytkab0bp.slicebeam.view.GLView): DoubleArray {
        val finished = CountDownLatch(1)
        val matrix = AtomicReference<DoubleArray>()
        glView.queueEvent {
            matrix.set(glView.renderer.projectionMatrix.clone())
            finished.countDown()
        }
        assertTrue("GL thread did not return the projection matrix", finished.await(10, TimeUnit.SECONDS))
        return requireNotNull(matrix.get())
    }

    private fun readCameraPoseOnGl(glView: ru.ytkab0bp.slicebeam.view.GLView): DoubleArray {
        val finished = CountDownLatch(1)
        val pose = AtomicReference<DoubleArray>()
        glView.queueEvent {
            val camera = glView.renderer.camera
            pose.set(doubleArrayOf(
                camera.origin.x, camera.origin.y, camera.origin.z,
                camera.position.x, camera.position.y, camera.position.z,
            ))
            finished.countDown()
        }
        assertTrue("GL thread did not return the camera pose", finished.await(10, TimeUnit.SECONDS))
        return requireNotNull(pose.get())
    }

    private fun readRendererIntOnGl(glView: ru.ytkab0bp.slicebeam.view.GLView, fieldName: String): Int {
        val finished = CountDownLatch(1)
        val value = AtomicReference<Int>()
        glView.queueEvent {
            val field = glView.renderer.javaClass.getDeclaredField(fieldName).apply { isAccessible = true }
            value.set(field.getInt(glView.renderer))
            finished.countDown()
        }
        assertTrue("GL thread did not return renderer state $fieldName", finished.await(10, TimeUnit.SECONDS))
        return requireNotNull(value.get())
    }

    private fun readModelHasPaintOnGl(glView: ru.ytkab0bp.slicebeam.view.GLView, index: Int): Boolean {
        val finished = CountDownLatch(1)
        val value = AtomicReference<Boolean>()
        glView.queueEvent {
            value.set(requireNotNull(glView.renderer.model).hasPaint(index))
            finished.countDown()
        }
        assertTrue("GL thread did not return model paint state", finished.await(10, TimeUnit.SECONDS))
        return requireNotNull(value.get())
    }

    private fun readUsedFilamentsOnGl(glView: ru.ytkab0bp.slicebeam.view.GLView, index: Int): IntArray {
        val finished = CountDownLatch(1)
        val value = AtomicReference<IntArray>()
        glView.queueEvent {
            value.set(requireNotNull(glView.renderer.model).getUsedFilaments(index))
            finished.countDown()
        }
        assertTrue("GL thread did not return used filament assignments", finished.await(10, TimeUnit.SECONDS))
        return requireNotNull(value.get())
    }

    private fun readPaintMaxFilamentOnGl(glView: ru.ytkab0bp.slicebeam.view.GLView, index: Int): Int {
        val finished = CountDownLatch(1)
        val value = AtomicReference<Int>()
        glView.queueEvent {
            value.set(requireNotNull(glView.renderer.model).paintMaxFilament(index))
            finished.countDown()
        }
        assertTrue("GL thread did not return maximum painted filament", finished.await(10, TimeUnit.SECONDS))
        return requireNotNull(value.get())
    }

    private fun readPaintPreviewTriangleCountOnGl(
        glView: ru.ytkab0bp.slicebeam.view.GLView,
        filament: Int,
    ): Int {
        val finished = CountDownLatch(1)
        val value = AtomicReference<Int>()
        val failure = AtomicReference<Throwable?>()
        glView.queueEvent {
            var overlay: GLModel? = null
            try {
                val renderer = glView.renderer
                val session = GLRenderer::class.java.getDeclaredField("paintSession")
                    .apply { isAccessible = true }.get(renderer) as? PaintSession
                if (session == null) error("Paint session ended before preview could be inspected")
                overlay = GLModel()
                value.set(session.buildOverlay(overlay, filament))
            } catch (error: Throwable) {
                failure.set(error)
            } finally {
                overlay?.release()
                finished.countDown()
            }
        }
        assertTrue("GL thread did not return paint preview triangles", finished.await(10, TimeUnit.SECONDS))
        failure.get()?.let { throw AssertionError("Could not inspect active paint preview", it) }
        return requireNotNull(value.get())
    }

    /** Finds an exact mesh hit in the visible portion of the GL view, outside the paint sheet. */
    private fun findUncoveredPreciseSurfacePoint(
        glView: ru.ytkab0bp.slicebeam.view.GLView,
        bed: BedFragment,
    ): Pair<Float, Float> {
        val panelBounds = compose.onNodeWithTag("workspace-mode-panel").fetchSemanticsNode().boundsInRoot
        val glLocation = IntArray(2)
        val composeLocation = IntArray(2)
        compose.runOnUiThread {
            glView.getLocationOnScreen(glLocation)
            requireNotNull(bed.overlayLayout).getLocationOnScreen(composeLocation)
        }
        val result = AtomicReference<Pair<Float, Float>?>(null)
        val finished = CountDownLatch(1)
        val failure = AtomicReference<Throwable?>()
        glView.queueEvent {
            try {
                val renderer = glView.renderer
                @Suppress("UNCHECKED_CAST")
                val models = GLRenderer::class.java.getDeclaredField("glModels")
                    .apply { isAccessible = true }.get(renderer) as ArrayList<GLModel>
                val mesh = models.firstOrNull() ?: error("Paint mode did not initialize the selected GL mesh")
                val hits = ArrayList<GLModel.HitResult>()
                val renderScale = Prefs.getRenderScale()
                val centerX = renderer.viewportWidth / 2
                val centerY = renderer.viewportHeight / 2
                // Search around the projected center first, where the loaded cube is framed.
                for (radius in listOf(0, 16, 32, 48, 64, 96, 128, 176)) {
                    val offsets = if (radius == 0) listOf(0 to 0) else buildList {
                        for (offset in -radius..radius step 8) {
                            add(offset to -radius)
                            add(offset to radius)
                            add(-radius to offset)
                            add(radius to offset)
                        }
                    }
                    for ((dx, dy) in offsets) {
                        val x = centerX + dx
                        val y = centerY + dy
                        if (x !in 0 until renderer.viewportWidth || y !in 0 until renderer.viewportHeight) continue
                        val localX = x / renderScale
                        val localY = y / renderScale
                        val rootX = glLocation[0] + localX - composeLocation[0]
                        val rootY = glLocation[1] + localY - composeLocation[1]
                        if (panelBounds.contains(Offset(rootX, rootY))) continue
                        mesh.raycaster.raycast(renderer, hits, x.toFloat(), y.toFloat())
                        if (hits.any { !it.approximate }) {
                            result.set((glLocation[0] + localX) to (glLocation[1] + localY))
                            return@queueEvent
                        }
                    }
                }
            } catch (error: Throwable) {
                failure.set(error)
            } finally {
                finished.countDown()
            }
        }
        assertTrue("GL thread did not locate a visible exact surface hit", finished.await(15, TimeUnit.SECONDS))
        failure.get()?.let { throw AssertionError("Could not locate visible paint target", it) }
        return requireNotNull(result.get()) {
            "No precise model surface point was reachable outside the paint controls; panel bounds=$panelBounds"
        }
    }

    private fun injectCanvasStroke(screenX: Float, screenY: Float) {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val downTime = SystemClock.uptimeMillis()
        fun inject(action: Int, x: Float, y: Float, eventTime: Long) {
            val event = MotionEvent.obtain(downTime, eventTime, action, x, y, 0).apply {
                source = InputDevice.SOURCE_TOUCHSCREEN
            }
            try {
                assertTrue("Android rejected a synthetic touchscreen event", automation.injectInputEvent(event, true))
            } finally {
                event.recycle()
            }
        }
        inject(MotionEvent.ACTION_DOWN, screenX, screenY, downTime)
        repeat(5) { step ->
            SystemClock.sleep(24L)
            inject(MotionEvent.ACTION_MOVE, screenX + step * 2f, screenY + step * 2f, SystemClock.uptimeMillis())
        }
        SystemClock.sleep(24L)
        inject(MotionEvent.ACTION_UP, screenX + 10f, screenY + 10f, SystemClock.uptimeMillis())
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    private fun hasSnackbarTitle(bed: BedFragment, titleRes: Int): Boolean {
        val snackbarController = BedFragment::class.java.getDeclaredField("snackbarController")
            .apply { isAccessible = true }.get(bed)
        val entries = snackbarController.javaClass.getMethod("getEntries").invoke(snackbarController) as List<*>
        val expectedTitle = compose.activity.getString(titleRes)
        return entries.any { entry ->
            val snackbar = entry?.javaClass?.getMethod("getSnackbar")?.invoke(entry)
            snackbar?.javaClass?.getField("title")?.get(snackbar) == expectedTitle
        }
    }

    private fun readVectorOnGl(
        glView: ru.ytkab0bp.slicebeam.view.GLView,
        index: Int,
        read: (Model, Vec3d) -> Unit,
    ): Vec3d {
        val finished = CountDownLatch(1)
        val value = AtomicReference<Vec3d?>()
        val failure = AtomicReference<Throwable?>()
        glView.queueEvent {
            try {
                val model = requireNotNull(glView.renderer.model)
                check(model.pointer != 0L) { "Renderer returned a released native model" }
                check(index in 0 until model.objectsCount) {
                    "Renderer model has ${model.objectsCount} objects; requested vector for index $index"
                }
                value.set(Vec3d().also { read(model, it) })
            } catch (error: Throwable) {
                failure.set(error)
            } finally {
                finished.countDown()
            }
        }
        assertTrue("GL thread did not return model vector", finished.await(10, TimeUnit.SECONDS))
        failure.get()?.let { throw AssertionError("Could not read native model vector on GL thread", it) }
        return requireNotNull(value.get())
    }

    private fun createInitializedActivityRule() = run {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val application = context.applicationContext as Application
        SliceBeam.INSTANCE = application as SliceBeam
        Prefs.init(application)
        Prefs.getPrefs()?.edit()
            ?.remove("crash")
            ?.putBoolean("converter_storage_first_prompt", true)
            ?.commit()
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
