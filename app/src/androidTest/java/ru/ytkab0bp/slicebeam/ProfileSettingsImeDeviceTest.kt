package ru.ytkab0bp.slicebeam

import android.view.KeyEvent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.config.ProfileSettingsRepository
import ru.ytkab0bp.slicebeam.components.ProfileConfigImportDialog
import ru.ytkab0bp.slicebeam.components.ProfileImportCategory
import ru.ytkab0bp.slicebeam.components.ProfileImportKey
import ru.ytkab0bp.slicebeam.components.ProfileImportOption
import ru.ytkab0bp.slicebeam.config.TemporaryProjectSettings
import ru.ytkab0bp.slicebeam.config.ThreeMfProjectSettings
import ru.ytkab0bp.slicebeam.fragment.BedFragment
import ru.ytkab0bp.slicebeam.fragment.PrintConfigFragment
import ru.ytkab0bp.slicebeam.navigation.MobileNavigationDelegate
import ru.ytkab0bp.slicebeam.slic3r.Slic3rConfigWrapper
import ru.ytkab0bp.slicebeam.utils.Prefs
import java.util.concurrent.atomic.AtomicReference

/** Exercises the real process-profile filter inside its profile screen while the IME is open. */
@RunWith(AndroidJUnit4::class)
class ProfileSettingsImeDeviceTest {
    @get:Rule
    val compose = createInitializedActivityRule()

    @After
    fun clearTemporaryProjectSettings() {
        TemporaryProjectSettings.clear()
    }

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

    @Test
    fun workspaceDialogBackHidesImeBeforeDismissingTheDialog() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        activity.runOnUiThread {
            bed.showWorkspaceComposeDialog { dismiss -> WorkspaceImeBackProbe(dismiss) }
        }
        compose.waitUntil(5_000) { bed.isWorkspaceMessageDialogAttached() }
        compose.waitUntil(10_000) {
            ViewCompat.getRootWindowInsets(activity.window.decorView)
                ?.isVisible(WindowInsetsCompat.Type.ime()) == true
        }

        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitUntil(10_000) {
            ViewCompat.getRootWindowInsets(activity.window.decorView)
                ?.isVisible(WindowInsetsCompat.Type.ime()) != true
        }
        compose.runOnIdle {
            assertEquals("Back hides the keyboard first and leaves the workspace dialog open", true,
                bed.isWorkspaceMessageDialogAttached())
            assertEquals("The workspace stays active behind its dialog", false, activity.isFinishing)
        }

        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitUntil(5_000) { !bed.isWorkspaceMessageDialogAttached() }
        compose.runOnIdle { assertEquals("The dialog is dismissed by the second Back", false, activity.isFinishing) }
    }

    @Test
    fun unsupportedFileImportUsesWorkspaceComposeErrorDialog() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        activity.runOnUiThread {
            MainActivity::class.java.getDeclaredMethod("loadFile", Uri::class.java)
                .apply { isAccessible = true }
                .invoke(activity, Uri.parse("file:///tmp/unsupported-model.testtype"))
        }

        compose.waitUntil(timeoutMillis = 5_000) { bed.isWorkspaceMessageDialogAttached() }
        compose.onNodeWithText(
            activity.getString(R.string.UnsupportedFileType, "unsupported-model.testtype"),
        ).assertIsDisplayed()
        compose.onNodeWithText(activity.getString(android.R.string.ok)).assertIsDisplayed().performClick()
        compose.waitUntil(timeoutMillis = 5_000) { !bed.isWorkspaceMessageDialogAttached() }
    }

    @Test
    fun profileImportComposeFlowShowsWarningsSelectionAndSkipConflicts() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val printKind = ConfigObject.PROFILE_LIST_PRINT
        val existing = ProfileImportKey(printKind, 0)
        val newProfile = ProfileImportKey(printKind, 1)
        val imported = AtomicReference<Set<ProfileImportKey>?>()
        val replaced = AtomicReference<Boolean?>()
        activity.runOnUiThread {
            bed.showWorkspaceComposeDialog { dismiss ->
                ProfileConfigImportDialog(
                    title = "Import profiles",
                    warningTitle = "Import warnings",
                    warningMessage = "Unsupported option warning",
                    selectionTitle = "Select profiles",
                    conflictTitle = "Profiles already exist",
                    categories = listOf(
                        ProfileImportCategory("Print", listOf(
                            ProfileImportOption(existing, "Standard", "Standard (already exists)", true),
                            ProfileImportOption(newProfile, "Fast", "Fast", false),
                        )),
                    ),
                    formatConflictMessage = { names -> "Resolve conflicts: ${names.joinToString()}" },
                    continueLabel = "Continue",
                    importLabel = "Import selected",
                    overwriteLabel = "Overwrite",
                    skipExistingLabel = "Skip existing",
                    cancelLabel = "Cancel",
                    warnings = listOf("Unsupported option warning"),
                    onDismiss = dismiss,
                    onImport = { selected, overwrite ->
                        imported.set(selected)
                        replaced.set(overwrite)
                        dismiss()
                    },
                )
            }
        }

        compose.waitUntil(timeoutMillis = 5_000) { bed.isWorkspaceMessageDialogAttached() }
        compose.onNodeWithText("Unsupported option warning").assertIsDisplayed()
        compose.onNodeWithText("Continue").performClick()
        compose.onNodeWithText("Standard (already exists)").assertIsDisplayed()
        compose.onNodeWithText("Fast").assertIsDisplayed()
        compose.onNodeWithText("Fast").performClick()
        compose.onNodeWithText("Fast").performClick()
        compose.onNodeWithText("Import selected").performClick()
        compose.onNodeWithText("Resolve conflicts: Print: Standard").assertIsDisplayed()
        compose.onNodeWithText("Skip existing").performClick()
        compose.waitUntil(timeoutMillis = 5_000) { !bed.isWorkspaceMessageDialogAttached() }

        assertEquals(setOf(existing, newProfile), imported.get())
        assertEquals(false, replaced.get())
    }

    @Test
    fun profileImportComposeFlowCanOverwriteOrCancelConflicts() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val existing = ProfileImportKey(ConfigObject.PROFILE_LIST_FILAMENT, 2)
        val imported = AtomicReference<Boolean?>()
        activity.runOnUiThread {
            bed.showWorkspaceComposeDialog { dismiss ->
                ProfileConfigImportDialog(
                    title = "Import profiles",
                    warningTitle = "Import warnings",
                    warningMessage = "",
                    selectionTitle = "Select profiles",
                    conflictTitle = "Profiles already exist",
                    categories = listOf(
                        ProfileImportCategory("Filaments", listOf(
                            ProfileImportOption(existing, "PLA", "PLA (already exists)", true),
                        )),
                    ),
                    formatConflictMessage = { names -> "Resolve conflicts: ${names.joinToString()}" },
                    continueLabel = "Continue",
                    importLabel = "Import selected",
                    overwriteLabel = "Overwrite",
                    skipExistingLabel = "Skip existing",
                    cancelLabel = "Cancel",
                    warnings = emptyList(),
                    onDismiss = dismiss,
                    onImport = { _, overwrite ->
                        imported.set(overwrite)
                        dismiss()
                    },
                )
            }
        }
        compose.waitUntil(timeoutMillis = 5_000) { bed.isWorkspaceMessageDialogAttached() }
        compose.onNodeWithText("Import selected").performClick()
        compose.onNodeWithText("Resolve conflicts: Filaments: PLA").assertIsDisplayed()
        compose.onNodeWithText("Overwrite").performClick()
        compose.waitUntil(timeoutMillis = 5_000) { !bed.isWorkspaceMessageDialogAttached() }
        assertEquals(true, imported.get())

        imported.set(null)
        activity.runOnUiThread {
            bed.showWorkspaceComposeDialog { dismiss ->
                ProfileConfigImportDialog(
                    title = "Import profiles",
                    warningTitle = "Import warnings",
                    warningMessage = "",
                    selectionTitle = "Select profiles",
                    conflictTitle = "Profiles already exist",
                    categories = listOf(ProfileImportCategory("Filaments", listOf(
                        ProfileImportOption(existing, "PLA", "PLA (already exists)", true),
                    ))),
                    formatConflictMessage = { names -> "Resolve conflicts: ${names.joinToString()}" },
                    continueLabel = "Continue",
                    importLabel = "Import selected",
                    overwriteLabel = "Overwrite",
                    skipExistingLabel = "Skip existing",
                    cancelLabel = "Cancel",
                    warnings = emptyList(),
                    onDismiss = dismiss,
                    onImport = { _, overwrite ->
                        imported.set(overwrite)
                        dismiss()
                    },
                )
            }
        }
        compose.waitUntil(timeoutMillis = 5_000) { bed.isWorkspaceMessageDialogAttached() }
        compose.onNodeWithText("Cancel").assertIsDisplayed().performClick()
        compose.waitUntil(timeoutMillis = 5_000) { !bed.isWorkspaceMessageDialogAttached() }
        assertEquals("Cancel never triggers the import callback", null, imported.get())
    }

    @Test
    fun threeMfSettingsChoiceAppliesOnlySelectedTemporaryOverrides() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        val config = SliceBeam.requireConfig()
        val printer = requireNotNull(config.presets?.get("printer"))
        val alternatePrinter = requireNotNull(config.printerConfigs.mapNotNull { it.title }.firstOrNull { it != printer })
        val savedPrintValue = config.findPrint(config.presets?.get("print"))?.get("layer_height")
        val savedFilamentValue = config.findFilament(config.presets?.get("filament"))?.get("nozzle_temperature")

        activity.runOnUiThread {
            activity.showThreeMfChoiceForTest(
                bed,
                ThreeMfProjectSettings(
                    sourceName = "settings-choice.3mf",
                    process = mapOf("layer_height" to "0.24", "curr_bed_type" to "Textured PEI Plate"),
                    filaments = listOf(mapOf(
                        "filament_colour" to "#112233",
                        "filament_type" to "PETG",
                        "nozzle_temperature" to "240",
                    )),
                    filamentTypes = emptyMap(),
                    printerTitle = alternatePrinter,
                ),
                hasFileFilaments = true,
            )
        }

        compose.waitUntil(timeoutMillis = 5_000) { bed.isWorkspaceMessageDialogAttached() }
        compose.onNodeWithText(activity.getString(R.string.Project3mfImportTitle)).assertIsDisplayed()
        val useFilePrinter = activity.getString(R.string.Project3mfUseFilePrinter, alternatePrinter)
        compose.onNodeWithText(useFilePrinter).performClick()
        compose.onNodeWithText(useFilePrinter).performClick()
        compose.onNodeWithText(activity.getString(R.string.Project3mfUsePrintSettings)).performClick()
        compose.onNodeWithText(activity.getString(R.string.Project3mfUseFilamentSettings)).performClick()
        compose.onNodeWithText(activity.getString(R.string.Project3mfApplyChoice)).performClick()
        compose.waitUntil(timeoutMillis = 5_000) { !bed.isWorkspaceMessageDialogAttached() }

        assertTrue("The choice activates project-local settings", TemporaryProjectSettings.active)
        assertTrue("Selected print settings stay project-local", TemporaryProjectSettings.owns(ConfigObject.PROFILE_LIST_PRINT))
        assertTrue("Selected filament settings stay project-local", TemporaryProjectSettings.owns(ConfigObject.PROFILE_LIST_FILAMENT))
        val effective = ConfigObject().apply { TemporaryProjectSettings.applyTo(this) }
        assertEquals("0.24", effective["layer_height"])
        assertEquals("Unchecking the file printer reselects the current printer", printer, config.presets?.get("printer"))
        assertEquals("Unchecking the file printer excludes its plate type", null, effective["curr_bed_type"])
        assertEquals("240", effective["nozzle_temperature"])
        assertEquals("The dialog never writes the saved print profile", savedPrintValue,
            config.findPrint(config.presets?.get("print"))?.get("layer_height"))
        assertEquals("The dialog never writes the saved filament profile", savedFilamentValue,
            config.findFilament(config.presets?.get("filament"))?.get("nozzle_temperature"))
    }

    @Test
    fun cancellingThreeMfSettingsChoiceClearsPreviousProjectOverrides() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as BedFragment
        TemporaryProjectSettings.activate(ThreeMfProjectSettings(
            sourceName = "previous.3mf",
            process = mapOf("layer_height" to "0.3"),
            filaments = emptyList(),
            filamentTypes = emptyMap(),
        ))
        assertTrue(TemporaryProjectSettings.active)

        activity.runOnUiThread {
            activity.showThreeMfChoiceForTest(
                bed,
                ThreeMfProjectSettings(
                    sourceName = "cancelled.3mf",
                    process = mapOf("layer_height" to "0.24"),
                    filaments = emptyList(),
                    filamentTypes = emptyMap(),
                ),
                hasFileFilaments = false,
            )
        }
        compose.waitUntil(timeoutMillis = 5_000) { bed.isWorkspaceMessageDialogAttached() }
        compose.onNodeWithText(activity.getString(android.R.string.cancel)).performClick()
        compose.waitUntil(timeoutMillis = 5_000) { !bed.isWorkspaceMessageDialogAttached() }
        assertEquals("Cancelled file choice must not retain the previous project's settings", false,
            TemporaryProjectSettings.active)
    }

    private fun MainActivity.showThreeMfChoiceForTest(
        bed: BedFragment,
        settings: ThreeMfProjectSettings,
        hasFileFilaments: Boolean,
    ) {
        val resultType = Class.forName("ru.ytkab0bp.slicebeam.MainActivity\$Project3mfImportResult")
        val result = resultType.getDeclaredConstructor().apply { isAccessible = true }.newInstance()
        resultType.getDeclaredField("settings").apply { isAccessible = true }.set(result, settings)
        resultType.getDeclaredField("hasFileFilaments").apply { isAccessible = true }.setBoolean(result, hasFileFilaments)
        MainActivity::class.java.getDeclaredMethod("show3mfImportChoice", BedFragment::class.java, resultType)
            .apply { isAccessible = true }
            .invoke(this, bed, result)
    }

    @Test
    fun printProfileFlushEditorIsHostedInComposeAndCancelKeepsSavedMatrix() {
        val activity = compose.activity
        activity.runOnUiThread { (activity.navigationDelegate as MobileNavigationDelegate).switchToSlot(2) }
        compose.waitUntil(timeoutMillis = 15_000) {
            activity.navigationDelegate.getCurrentFragment() is PrintConfigFragment
        }
        val context = activity
        val profile = SliceBeam.requireConfig().findPrint(SliceBeam.requireConfig().presets?.get("print"))
        val savedBefore = profile?.get("flush_volumes_matrix")
        val flushButtonLabel = context.getString(R.string.FlushVolumes)
        val flushButtonDescription = context.getString(R.string.PrintConfigFlushVolumesHelp)
        compose.onNodeWithContentDescription("$flushButtonLabel. $flushButtonDescription").performClick()
        compose.onNodeWithTag("flush-cell-1").performTextClearance()
        compose.onNodeWithTag("flush-cell-1").performTextInput("-1")
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.onNodeWithText(context.getString(android.R.string.ok)).performClick()
        compose.onNodeWithText(context.getString(R.string.PrintConfigFlushVolumesInvalid)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(android.R.string.cancel)).performClick()
        compose.onAllNodesWithText(context.getString(R.string.PrintConfigFlushVolumesHelp)).assertCountEquals(0)
        assertEquals("Invalid edit and Cancel leave the saved profile unchanged", savedBefore,
            profile?.get("flush_volumes_matrix"))
    }

    @Test
    fun printProfilePrimeTowerComposeDialogCancelsWithoutChangingSavedPositions() {
        val activity = compose.activity
        activity.runOnUiThread { (activity.navigationDelegate as MobileNavigationDelegate).switchToSlot(2) }
        compose.waitUntil(timeoutMillis = 15_000) {
            activity.navigationDelegate.getCurrentFragment() is PrintConfigFragment
        }
        val context = activity
        val profile = SliceBeam.requireConfig().findPrint(SliceBeam.requireConfig().presets?.get("print"))
        val xBefore = profile?.get("wipe_tower_x")
        val yBefore = profile?.get("wipe_tower_y")
        val towerButtonLabel = context.getString(R.string.PrimeTowerPositionShort)
        val towerButtonDescription = context.getString(R.string.PrimeTowerPositionInlineHelp)
        compose.onNodeWithContentDescription("$towerButtonLabel. $towerButtonDescription").performClick()
        compose.onNodeWithTag("prime-tower-x-0").performTextClearance()
        compose.onNodeWithTag("prime-tower-x-0").performTextInput("123.4")
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.onNodeWithText(context.getString(android.R.string.cancel)).assertIsDisplayed()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.onAllNodesWithText(context.getString(R.string.PrimeTowerPosition)).assertCountEquals(0)
        assertEquals("Cancel leaves the saved X coordinate unchanged", xBefore, profile?.get("wipe_tower_x"))
        assertEquals("Cancel leaves the saved Y coordinate unchanged", yBefore, profile?.get("wipe_tower_y"))
    }

    @Test
    fun bedPrimeTowerDialogUsesWorkspaceHostAndCancelOrApplyUpdatesOnlyTheDraft() {
        val activity = compose.activity
        val bed = activity.navigationDelegate.getCurrentFragment() as? BedFragment
        assertTrue("The main workspace should initially host BedFragment", bed != null)
        val config = SliceBeam.requireConfig()
        val profile = config.findPrint(config.presets?.get("print"))
        val xBefore = profile?.get("wipe_tower_x")
        val yBefore = profile?.get("wipe_tower_y")
        ProfileSettingsRepository.clearDraft(ConfigObject.PROFILE_LIST_PRINT)

        activity.runOnUiThread {
            BedFragment::class.java.getDeclaredMethod("showPrimeTowerPositionDialogForCurrentPlate")
                .apply { isAccessible = true }
                .invoke(requireNotNull(bed))
        }
        compose.waitUntil(timeoutMillis = 5_000) { requireNotNull(bed).isWorkspaceMessageDialogAttached() }
        compose.onNodeWithTag("prime-tower-x-0").performTextClearance()
        compose.onNodeWithTag("prime-tower-x-0").performTextInput("123.456")
        compose.onNodeWithTag("prime-tower-y-0").performTextClearance()
        compose.onNodeWithTag("prime-tower-y-0").performTextInput("67.89")

        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.onNodeWithText(activity.getString(android.R.string.cancel)).assertIsDisplayed().performClick()
        compose.waitUntil(timeoutMillis = 5_000) { !requireNotNull(bed).isWorkspaceMessageDialogAttached() }

        assertEquals("Cancel must not stage an X coordinate", emptyMap<String, String>(),
            ProfileSettingsRepository.pendingEntries(ConfigObject.PROFILE_LIST_PRINT))
        assertEquals("Cancel must keep the saved X coordinate", xBefore, profile?.get("wipe_tower_x"))
        assertEquals("Cancel must keep the saved Y coordinate", yBefore, profile?.get("wipe_tower_y"))

        activity.runOnUiThread {
            BedFragment::class.java.getDeclaredMethod("showPrimeTowerPositionDialogForCurrentPlate")
                .apply { isAccessible = true }
                .invoke(requireNotNull(bed))
        }
        compose.waitUntil(timeoutMillis = 5_000) { requireNotNull(bed).isWorkspaceMessageDialogAttached() }
        compose.onNodeWithTag("prime-tower-x-0").performTextClearance()
        compose.onNodeWithTag("prime-tower-x-0").performTextInput("123.456")
        compose.onNodeWithTag("prime-tower-y-0").performTextClearance()
        compose.onNodeWithTag("prime-tower-y-0").performTextInput("67.89")
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.onNodeWithText(activity.getString(android.R.string.ok)).assertIsDisplayed().performClick()
        compose.waitUntil(timeoutMillis = 5_000) { !requireNotNull(bed).isWorkspaceMessageDialogAttached() }

        val pending = ProfileSettingsRepository.pendingEntries(ConfigObject.PROFILE_LIST_PRINT)
        assertEquals("Apply stages X without committing the preset", "123.456", pending["wipe_tower_x"])
        assertEquals("Apply stages Y without committing the preset", "67.89", pending["wipe_tower_y"])
        ProfileSettingsRepository.clearDraft(ConfigObject.PROFILE_LIST_PRINT)
        assertEquals("The device test leaves the saved X coordinate intact", xBefore, profile?.get("wipe_tower_x"))
        assertEquals("The device test leaves the saved Y coordinate intact", yBefore, profile?.get("wipe_tower_y"))
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

@Composable
private fun WorkspaceImeBackProbe(dismiss: () -> Unit) {
    val requester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    var value by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        androidx.compose.runtime.withFrameNanos { }
        requester.requestFocus()
        keyboard?.show()
    }
    Column {
        Text("Workspace dialog keyboard probe")
        OutlinedTextField(
            value = value,
            onValueChange = { value = it },
            modifier = Modifier.fillMaxWidth().focusRequester(requester),
        )
        androidx.compose.material3.TextButton(onClick = dismiss) { Text("Close") }
    }
}
