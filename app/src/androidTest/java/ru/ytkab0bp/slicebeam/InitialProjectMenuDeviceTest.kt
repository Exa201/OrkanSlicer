package ru.ytkab0bp.slicebeam

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.FileProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import ru.ytkab0bp.slicebeam.components.bed_menu.FileMenu
import ru.ytkab0bp.slicebeam.components.bed_menu.FileMenuObjectActions
import ru.ytkab0bp.slicebeam.components.bed_menu.ListBedMenu
import ru.ytkab0bp.slicebeam.components.bed_menu.CameraPreset
import ru.ytkab0bp.slicebeam.components.bed_menu.CameraPresetGeometry
import ru.ytkab0bp.slicebeam.components.bed_menu.CameraVector
import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.slic3r.Model
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
    fun multiPlateProjectExportWarnsAndCancelDoesNotOpenDocumentPicker() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val modelFile = File(activity.cacheDir, "menu-multiplate-export-warning-test.stl")
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
        } finally {
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
        val previousProjection = Prefs.isOrthoProjectionEnabled()
        val previousControlMode = Prefs.getCameraControlMode()
        try {
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuCamera))
                .assertIsDisplayed()
                .performClick()
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithText(activity.getString(R.string.MenuCameraTop))
                    .fetchSemanticsNodes().isNotEmpty()
            }

            val projectionAction = compose.onNodeWithContentDescription(
                activity.getString(R.string.MenuCameraOrtho),
            ).assertIsDisplayed()
            projectionAction.performClick()
            compose.waitForIdle()
            assertEquals("camera projection tile should update the persisted preference",
                !previousProjection, Prefs.isOrthoProjectionEnabled())

            compose.onNode(hasScrollToIndexAction()).performScrollToIndex(8)
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
        }
    }

    @Test
    fun cameraPresetTilesUpdateTheRendererPoseForEveryPreset() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        compose.onNodeWithContentDescription(activity.getString(R.string.MenuCamera))
            .assertIsDisplayed()
            .performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText(activity.getString(R.string.MenuCameraTop))
                .fetchSemanticsNodes().isNotEmpty()
        }

        val camera = requireNotNull(bed.glView).renderer.camera
        val modelBed = requireNotNull(bed.glView).renderer.bed!!
        val min = requireNotNull(modelBed.getVolumeMin())
        val max = requireNotNull(modelBed.getVolumeMax())
        val minVector = CameraVector(min.x, min.y, min.z)
        val maxVector = CameraVector(max.x, max.y, max.z)
        val presets = listOf(
            Triple(0, R.string.MenuCameraIsometric, CameraPreset.ISOMETRIC),
            Triple(2, R.string.MenuCameraTop, CameraPreset.TOP),
            Triple(3, R.string.MenuCameraBottom, CameraPreset.BOTTOM),
            Triple(4, R.string.MenuCameraFront, CameraPreset.FRONT),
            Triple(5, R.string.MenuCameraBack, CameraPreset.BACK),
            Triple(6, R.string.MenuCameraLeft, CameraPreset.LEFT),
            Triple(7, R.string.MenuCameraRight, CameraPreset.RIGHT),
        )
        presets.forEach { (index, presetLabel, preset) ->
            val target = CameraPresetGeometry.resolve(preset, minVector, maxVector)
            compose.onNode(hasScrollToIndexAction()).performScrollToIndex(index)
            compose.onNodeWithContentDescription(activity.getString(presetLabel))
                .assertIsDisplayed()
                .performClick()
            compose.waitUntil(timeoutMillis = 5_000) {
                kotlin.math.abs(camera.position.x - target.position.x) < 0.1 &&
                    kotlin.math.abs(camera.position.y - target.position.y) < 0.1 &&
                    kotlin.math.abs(camera.position.z - target.position.z) < 0.1 &&
                    kotlin.math.abs(camera.origin.x - target.origin.x) < 0.1 &&
                    kotlin.math.abs(camera.origin.y - target.origin.y) < 0.1 &&
                    kotlin.math.abs(camera.origin.z - target.origin.z) < 0.1
            }
            // Let the renderer's spring animation settle before requesting the next preset.
            SystemClock.sleep(900L)
            compose.waitForIdle()
            assertTrue(
                "${activity.getString(presetLabel)} must apply its configured renderer camera pose",
                kotlin.math.abs(camera.position.x - target.position.x) < 0.1 &&
                    kotlin.math.abs(camera.position.y - target.position.y) < 0.1 &&
                    kotlin.math.abs(camera.position.z - target.position.z) < 0.1,
            )
        }
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
        FileMenuObjectActions(bed).showDuplicateDialog(viewport)
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
        modelFile.delete()
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

            compose.onNodeWithContentDescription(activity.getString(R.string.MenuToolbarNavigation))
                .assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodesWithText(activity.getString(R.string.MenuTransformScale))
                    .fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNode(hasScrollToIndexAction()).performScrollToIndex(6)
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
            assertEquals("closing the scale tool should preserve the Edit destination",
                WorkspaceDestination.EDIT.ordinal,
                workspaceController.javaClass.getMethod("getSelectedDestinationSlot").invoke(workspaceController))
            assertEquals("closing the tool must keep the committed scale", original.x * 1.25,
                readScaleOnGl(glView, 0).x, 0.002)

            if (!(workspaceController.javaClass.getMethod("getToolPanelExpanded").invoke(workspaceController) as Boolean)) {
                compose.onNodeWithContentDescription(activity.getString(R.string.MenuToolbarNavigation))
                    .assertIsDisplayed().performClick()
            }
            compose.waitForIdle()
            compose.onNode(hasScrollToIndexAction()).performScrollToIndex(1)
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

            openEditMenu(activity)
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

            openEditMenu(activity)
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
            if (!(workspaceController.javaClass.getMethod("getToolPanelExpanded").invoke(workspaceController) as Boolean)) {
                compose.onNodeWithContentDescription(activity.getString(R.string.MenuToolbarNavigation))
                    .assertIsDisplayed().performClick()
            }
            compose.waitForIdle()
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

            compose.runOnUiThread { bed.undoModelEdit() }
            assertEquals("Undo should restore the original X rotation", 0.0,
                readRotationOnGl(glView, 0).x, 0.002)
            compose.runOnUiThread { bed.redoModelEdit() }
            assertEquals("Redo should restore the confirmed X rotation", Math.PI / 2,
                readRotationOnGl(glView, 0).x, 0.002)

            glView.selectModel(0, Runnable {})
            instrumentationWaitForGlIdle()
            assertEquals("test object should remain selected before reset", 0, readSelectedObjectOnGl(glView))
            assertTrue("Back should close the rotation tool before returning to Edit actions", bed.onBackPressed())
            compose.waitUntil(timeoutMillis = 5_000) {
                workspaceController.javaClass.getMethod("getActiveToolId").invoke(workspaceController) == null
            }
            if (!(workspaceController.javaClass.getMethod("getToolPanelExpanded").invoke(workspaceController) as Boolean)) {
                compose.onNodeWithContentDescription(activity.getString(R.string.MenuToolbarNavigation))
                    .assertIsDisplayed().performClick()
            }
            compose.waitForIdle()
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
            compose.runOnUiThread { bed.undoModelEdit() }
            assertEquals("Undo should restore rotation after Reset Rotation", Math.PI / 2,
                readRotationOnGl(glView, 0).x, 0.002)
            compose.runOnUiThread { bed.redoModelEdit() }
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

            openEditMenu(activity)
            compose.onNode(hasScrollToIndexAction()).performScrollToIndex(8)
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

            openEditMenu(activity)
            compose.onNode(hasScrollToIndexAction()).performScrollToIndex(10)
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuToolbarVariableLayerHeight))
                .performScrollTo().assertIsDisplayed().performClick()
            compose.onNodeWithText(activity.getString(R.string.VariableLayerTitle)).assertIsDisplayed()
            assertTrue("Variable Layer action should activate its workspace mode", bed.isVariableLayerHeightMode)
            compose.onNodeWithText(activity.getString(R.string.ActionDone)).assertIsDisplayed().performClick()
            compose.waitForIdle()
            assertFalse("Done should close Variable Layer mode", bed.isVariableLayerHeightMode)

            glView.selectModel(0, Runnable {})
            instrumentationWaitForGlIdle()
            openEditMenu(activity)
            compose.onNode(hasScrollToIndexAction()).performScrollToIndex(9)
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

            openEditMenu(activity)
            compose.onNode(hasScrollToIndexAction()).performScrollToIndex(7)
            compose.onNodeWithContentDescription(activity.getString(R.string.ModelSimplify))
                .performScrollTo().assertIsDisplayed().performClick()
            compose.onNodeWithText(activity.getString(android.R.string.cancel)).assertIsDisplayed().performClick()
            assertEquals("cancelling simplify must preserve the model", originalTriangles,
                readTriangleCountOnGl(glView, 0))

            openEditMenu(activity)
            compose.onNode(hasScrollToIndexAction()).performScrollToIndex(7)
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

    private fun openEditMenu(activity: MainActivity) {
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val controller = BedFragment::class.java.getDeclaredField("workspaceController")
            .apply { isAccessible = true }.get(bed)
        val editSlot = WorkspaceDestination.EDIT.ordinal
        val shouldNavigate = controller.javaClass.getMethod("getSelectedDestinationSlot")
            .invoke(controller) != editSlot
        val shouldExpand = !(controller.javaClass.getMethod("getToolPanelExpanded").invoke(controller) as Boolean)
        if (shouldNavigate || shouldExpand) {
            compose.onNodeWithContentDescription(activity.getString(R.string.MenuToolbarNavigation))
                .assertIsDisplayed().performClick()
            compose.waitUntil(timeoutMillis = 5_000) {
                controller.javaClass.getMethod("getSelectedDestinationSlot").invoke(controller) == editSlot &&
                    controller.javaClass.getMethod("getToolPanelExpanded").invoke(controller) == true
            }
        }
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(1)
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
