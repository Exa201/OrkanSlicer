package ru.ytkab0bp.slicebeam

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.height
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.ui.BeamComposeTheme
import ru.ytkab0bp.slicebeam.ui.workspace.PlateTabs
import ru.ytkab0bp.slicebeam.ui.workspace.PlateThumbnail
import ru.ytkab0bp.slicebeam.components.AcePhysicalSlotsBar
import ru.ytkab0bp.slicebeam.utils.MmuFilamentSync
import ru.ytkab0bp.slicebeam.view.FilamentPaletteContent
import ru.ytkab0bp.slicebeam.view.PaletteSlotSnapshot
import ru.ytkab0bp.slicebeam.components.bed_menu.BedComposeMenuAction
import ru.ytkab0bp.slicebeam.components.bed_menu.BedMenuActionsContent
import ru.ytkab0bp.slicebeam.components.WorkspaceFilamentPaletteSheet
import ru.ytkab0bp.slicebeam.components.WorkspaceProfileSheet
import ru.ytkab0bp.slicebeam.components.FilamentSlotSheet
import ru.ytkab0bp.slicebeam.components.ColorChooserDialog
import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.config.ThreeMfProjectSettings
import ru.ytkab0bp.slicebeam.config.TemporaryProjectSettings
import ru.ytkab0bp.slicebeam.slic3r.Slic3rConfigWrapper
import ru.ytkab0bp.slicebeam.utils.Prefs
import ru.ytkab0bp.slicebeam.utils.FilamentSlot
import ru.ytkab0bp.slicebeam.view.FilamentPaletteController
import ru.ytkab0bp.slicebeam.SliceBeam
import ru.ytkab0bp.slicebeam.ui.settings.ComposeProfileColorOption

/** Exercises the real virtualized palette row with both supported and synthetic UI-only sizes. */
@RunWith(AndroidJUnit4::class)
class FilamentPaletteComposeDeviceTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun workspaceFilamentSheetRendersAndDismissesFromCancel() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val app = context.applicationContext as SliceBeam
        SliceBeam.INSTANCE = app
        Prefs.init(app)
        val dismissed = mutableStateOf(false)
        lateinit var sheet: WorkspaceFilamentPaletteSheet
        compose.runOnUiThread {
            sheet = WorkspaceFilamentPaletteSheet(
                context = context,
                controller = FilamentPaletteController(),
                hasModel = false,
                onAssignModel = {},
            )
        }
        compose.setContent {
            BeamComposeTheme {
                if (!dismissed.value) sheet.Content { dismissed.value = true }
            }
        }

        compose.onNodeWithText(context.getString(R.string.WorkspaceFilamentPaletteTitle)).assertIsDisplayed()
        val cancel = compose.onNodeWithText(context.getString(android.R.string.cancel))
            .assertIsDisplayed().assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        cancel.performClick()
        compose.runOnIdle { assertTrue(dismissed.value) }
        assertEquals(0, compose.onAllNodesWithText(
            context.getString(R.string.WorkspaceFilamentPaletteTitle),
        ).fetchSemanticsNodes().size)
    }

    @Test
    fun workspaceProfileSheetRendersAndAndroidBackDismisses() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val app = context.applicationContext as SliceBeam
        SliceBeam.INSTANCE = app
        Prefs.init(app)
        val previousConfig = SliceBeam.CONFIG
        val config = context.assets.open("orca_profiles/Anycubic.ini").use { Slic3rConfigWrapper(it) }
        val printer = requireNotNull(config.findPrinter("Anycubic Kobra 3 0.4 nozzle"))
        val process = requireNotNull(config.findPrint("0.16mm Standard"))
        val filament = requireNotNull(config.findFilament("Anycubic Generic PLA"))
        config.presets = ConfigObject("presets").apply {
            put("printer", printer.title)
            put("print", process.title)
            put("filament", filament.title)
        }
        SliceBeam.CONFIG = config
        val dismissed = mutableStateOf(false)
        lateinit var sheet: WorkspaceProfileSheet
        compose.runOnUiThread { sheet = WorkspaceProfileSheet(context, null) }
        compose.setContent {
            BeamComposeTheme {
                if (!dismissed.value) sheet.Content { dismissed.value = true }
            }
        }

        try {
            compose.onNodeWithText(context.getString(R.string.WorkspaceProfilesTitle)).assertIsDisplayed()
            compose.onNodeWithText(context.getString(R.string.WorkspaceProfilePrinter)).assertIsDisplayed()
            val apply = compose.onNodeWithText(context.getString(R.string.WorkspaceProfilesApply))
                .assertIsDisplayed().assertHeightIsAtLeast(48.dp)
            apply.assertWidthIsAtLeast(48.dp)
            instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
            compose.waitForIdle()
            compose.runOnIdle { assertTrue(dismissed.value) }
            assertEquals(0, compose.onAllNodesWithText(
                context.getString(R.string.WorkspaceProfilesTitle),
            ).fetchSemanticsNodes().size)
        } finally {
            SliceBeam.CONFIG = previousConfig
        }
    }

    @Test
    fun workspaceProfileSheetAppliesOnlyChosenProfileGroupInsideTemporaryProject() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val app = context.applicationContext as SliceBeam
        SliceBeam.INSTANCE = app
        Prefs.init(app)
        val previousConfig = SliceBeam.CONFIG
        val config = context.assets.open("orca_profiles/Anycubic.ini").use { Slic3rConfigWrapper(it) }
        val printer = requireNotNull(config.findPrinter("Anycubic Kobra 3 0.4 nozzle"))
        val selectedProcess = requireNotNull(config.findPrint("0.16mm Standard"))
        val selectedFilament = requireNotNull(config.findFilament("Anycubic Generic PLA"))
        val nextProcess = requireNotNull(config.printConfigs.firstOrNull { it.title != selectedProcess.title })
        val nextProcessTitle = requireNotNull(nextProcess.title)
        config.presets = ConfigObject("presets").apply {
            put("printer", printer.title)
            put("print", selectedProcess.title)
            put("filament", selectedFilament.title)
        }
        val savedSelection = HashMap(config.presets!!.values)
        SliceBeam.CONFIG = config
        TemporaryProjectSettings.activate(
            ThreeMfProjectSettings(
                sourceName = "profile-sheet-ui-test",
                process = mapOf("layer_height" to "0.24", "wall_loops" to "9"),
                filaments = listOf(mapOf("filament_type" to "PETG", "nozzle_temperature" to "222")),
                filamentTypes = emptyMap(),
                printSettingsImported = true,
                filamentSettingsImported = true,
            ),
        )

        val dismissed = mutableStateOf(false)
        lateinit var sheet: WorkspaceProfileSheet
        compose.runOnUiThread { sheet = WorkspaceProfileSheet(context, null) }
        compose.setContent {
            BeamComposeTheme {
                if (!dismissed.value) sheet.Content { dismissed.value = true }
            }
        }

        try {
            compose.onNodeWithText(requireNotNull(selectedProcess.title)).performClick()
            compose.onNodeWithText(nextProcessTitle).performClick()
            compose.onNodeWithText(context.getString(R.string.WorkspaceProfilesApply)).performClick()
            compose.waitForIdle()

            assertEquals(
                "Only the process group selected in the sheet should replace the imported process values",
                nextProcess["wall_loops"],
                TemporaryProjectSettings.valueFor(ConfigObject.PROFILE_LIST_PRINT, "wall_loops", includeEdits = false),
            )
            assertEquals(
                "The unselected filament group must keep its project-imported value",
                "222",
                TemporaryProjectSettings.valueFor(ConfigObject.PROFILE_LIST_FILAMENT, "nozzle_temperature", includeEdits = false),
            )
            assertEquals("Quick profile changes in a project must not persist profile selection", savedSelection, config.presets!!.values)
            compose.runOnIdle { assertTrue("Applying the selected profile should close the sheet", dismissed.value) }
        } finally {
            TemporaryProjectSettings.clear()
            SliceBeam.CONFIG = previousConfig
        }
    }

    @Test
    fun filamentSlotEditorAndColorPaletteStayInComposeAndCommitChosenColor() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val slot = FilamentSlot(0xFFFF0000.toInt(), "PLA")
        var changes = 0
        var dismissed = false
        val sheet = FilamentSlotSheet(
            slot = slot,
            canDelete = true,
            listener = object : FilamentSlotSheet.Listener {
                override fun onChanged(slot: FilamentSlot) { changes++ }
                override fun onDelete() = Unit
            },
        )

        compose.setContent {
            BeamComposeTheme {
                if (!dismissed) sheet.Content { dismissed = true }
            }
        }
        compose.onNodeWithText(context.getString(R.string.SlotFilamentConfig)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.FilamentSlotColor)).performClick()
        compose.onNodeWithText(context.getString(R.string.ColorChooserCommon)).assertIsDisplayed()
        val black = compose.onAllNodesWithContentDescription(
            context.getString(R.string.ColorChooserSwatchDescription, "#000000"),
        ).onFirst().assertIsDisplayed()
        black.assertWidthIsAtLeast(44.dp).assertHeightIsAtLeast(44.dp).performClick()
        compose.runOnIdle {
            assertEquals(0xFF000000.toInt(), slot.color)
            assertEquals(1, changes)
        }
        compose.onNodeWithText(context.getString(R.string.SlotFilamentConfig)).assertIsDisplayed()
        instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        compose.runOnIdle { assertTrue(dismissed) }
    }

    @Test
    fun profileColorSettingOpensComposePaletteAndCommitsTheChosenColor() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        var selectedColor: Int? = null
        compose.setContent {
            BeamComposeTheme {
                ComposeProfileColorOption.Content(
                    title = "Profile color setting",
                    initialValue = "#FF0000",
                    onColorPicked = { selectedColor = it },
                    onLongClick = Runnable {},
                )
            }
        }

        compose.onNodeWithText("Profile color setting").performClick()
        compose.onNodeWithText(context.getString(R.string.ColorChooserCommon)).assertIsDisplayed()
        compose.onAllNodesWithContentDescription(
            context.getString(R.string.ColorChooserSwatchDescription, "#FF0000"),
        ).onFirst().performClick()
        compose.runOnIdle { assertEquals(0xFFFF0000.toInt(), selectedColor) }
    }

    @Test
    fun customColorPickerSupportsCancelChangeApplyAndExplicitReset() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val visible = mutableStateOf(true)
        val selectedColor = mutableIntStateOf(0xFFFF0000.toInt())
        compose.setContent {
            BeamComposeTheme {
                if (visible.value) {
                    ColorChooserDialog(
                        initialColor = selectedColor.intValue,
                        resetLabel = context.getString(R.string.SettingsInterfaceColorReset),
                        onDismiss = { visible.value = false },
                        onColorChosen = { selectedColor.intValue = it },
                        onReset = { selectedColor.intValue = 0xFF336699.toInt() },
                    )
                }
            }
        }

        compose.onNodeWithText(context.getString(R.string.OpenCustomColorPicker)).performClick()
        compose.onNodeWithText(context.getString(R.string.CustomColorHue)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(android.R.string.cancel)).performClick()
        compose.runOnIdle {
            assertTrue("Cancel returns to the palette", visible.value)
            assertEquals("Cancel leaves the existing color unchanged", 0xFFFF0000.toInt(), selectedColor.intValue)
        }

        compose.onNodeWithText(context.getString(R.string.OpenCustomColorPicker)).performClick()
        compose.onNodeWithContentDescription(context.getString(R.string.CustomColorHue))
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress ->
                assertTrue("Hue slider accepts an accessible value", setProgress(180f))
            }
        compose.onNodeWithText(context.getString(R.string.CustomColorApply)).performClick()
        compose.runOnIdle {
            assertEquals("Hue change is applied as an opaque color", 0xFF00FFFF.toInt(), selectedColor.intValue)
            assertFalse("Applying closes the palette", visible.value)
        }

        compose.runOnIdle { visible.value = true }
        compose.waitForIdle()
        compose.onNodeWithText(context.getString(R.string.SettingsInterfaceColorReset)).performClick()
        compose.runOnIdle {
            assertEquals("Reset is an explicit action", 0xFF336699.toInt(), selectedColor.intValue)
            assertFalse("Reset closes the palette", visible.value)
        }
    }

    @Test
    fun longAceGateRowHasAccessibleScrollControlAndReachesLastGate() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1.3f)) {
                BeamComposeTheme {
                    Box(Modifier.width(320.dp)) {
                        AcePhysicalSlotsBar(
                            gates = List(24) { index ->
                                MmuFilamentSync.Companion.PhysicalGate(
                                    index = index,
                                    printerTool = index,
                                    occupied = true,
                                    filament = FilamentSlot(0xFF000000.toInt() or (index * 0x010101), "Synthetic PLA $index"),
                                )
                            },
                        )
                    }
                }
            }
        }

        val scrollForward = compose.onNodeWithContentDescription(
            context.getString(R.string.AcePhysicalSlotsScrollForward),
        ).assertIsDisplayed()
        scrollForward.assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp).performClick()
        compose.onNodeWithText("Synthetic PLA 23").assertIsDisplayed()
    }

    @Test
    fun platePickerCancelHasAccessibleTouchTargetAndDismisses() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val open = mutableStateOf(false)
        var openCalls = 0
        var dismissCalls = 0
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1.5f)) {
                BeamComposeTheme {
                    PlateTabs(
                        openState = open,
                        plateCount = 3,
                        selectedIndex = 0,
                        objectCounts = listOf(0, 1, 2),
                        thumbnails = List(3) { PlateThumbnail(emptyList()) },
                        onOpen = { openCalls++ },
                        onDismiss = { dismissCalls++ },
                        onSelect = {},
                        onClose = {},
                        onAdd = {},
                    )
                }
            }
        }

        compose.onNodeWithContentDescription(context.getString(R.string.PlateNumber, "1")).performClick()
        val cancelLabel = context.getString(android.R.string.cancel)
        compose.onNodeWithText(cancelLabel).assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        assertEquals(0, compose.onAllNodesWithText(cancelLabel).fetchSemanticsNodes().size)
        compose.runOnIdle {
            assertFalse(open.value)
            assertEquals(1, openCalls)
            assertEquals(1, dismissCalls)
        }
    }

    @Test
    fun plateCounterExpandsAtLargeFontScaleInsteadOfClippingItsCount() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val open = mutableStateOf(false)
        compose.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(1f, 1.5f),
                LocalConfiguration provides Configuration().apply {
                    screenWidthDp = 360
                    screenHeightDp = 800
                },
            ) {
                BeamComposeTheme {
                    PlateTabs(
                        openState = open,
                        plateCount = 1,
                        selectedIndex = 0,
                        objectCounts = listOf(0),
                        thumbnails = listOf(PlateThumbnail(emptyList())),
                        onOpen = {}, onDismiss = {}, onSelect = {}, onClose = {}, onAdd = {},
                    )
                }
            }
        }

        compose.onNodeWithContentDescription(context.getString(R.string.PlateNumber, "1"))
            .assertIsDisplayed()
            .assertHeightIsAtLeast(52.dp)
    }

    @Test
    fun platePickerSelectsLastPlateAcrossStressProjectSizes() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val count = mutableIntStateOf(1)
        val selectedIndex = mutableIntStateOf(0)
        val open = mutableStateOf(false)
        compose.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(1f, 1.5f),
                LocalConfiguration provides Configuration().apply {
                    screenWidthDp = 320
                    screenHeightDp = 640
                },
            ) {
                BeamComposeTheme {
                    PlateTabs(
                        openState = open,
                        plateCount = count.intValue,
                        selectedIndex = selectedIndex.intValue,
                        objectCounts = List(count.intValue) { it },
                        thumbnails = List(count.intValue) { PlateThumbnail(emptyList()) },
                        onOpen = {},
                        onDismiss = {},
                        onSelect = { selectedIndex.intValue = it },
                        onClose = {},
                        onAdd = {},
                    )
                }
            }
        }

        listOf(1, 3, 6, 10, 20).forEach { plateCount ->
            compose.runOnUiThread {
                count.intValue = plateCount
                selectedIndex.intValue = 0
            }
            compose.waitForIdle()
            compose.onNodeWithContentDescription(context.getString(R.string.PlateNumber, "1")).performClick()
            if (plateCount > 2) {
                compose.onNode(hasScrollToIndexAction()).performScrollToIndex(plateCount - 1)
            }
            compose.onNodeWithContentDescription(
                context.getString(R.string.PlatePickerSelectDescription, plateCount.toString()),
            ).assertIsDisplayed().performClick()
            compose.runOnIdle {
                assertEquals(plateCount - 1, selectedIndex.intValue)
                assertFalse(open.value)
            }
        }
    }

    @Test
    fun paletteRowsFromOneThroughTwentyFourEntriesScrollAndSelectTheLastItem() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val count = mutableIntStateOf(1)
        val widthDp = mutableIntStateOf(320)
        val fontScale = mutableFloatStateOf(1f)
        var selectedIndex = -1

        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale.floatValue)) {
                BeamComposeTheme {
                    val slots = List(count.intValue) { index ->
                        PaletteSlotSnapshot(
                            color = 0xFF000000.toInt() or ((index * 0x010307) and 0x00FFFFFF),
                            type = "Synthetic PLA $index with a long material label",
                        )
                    }
                    Box(Modifier.width(widthDp.intValue.dp)) {
                        FilamentPaletteContent(
                            slots = slots,
                            selectedGates = null,
                            onSlotClick = { selectedIndex = it },
                            onAddClick = {},
                        )
                    }
                }
            }
        }

        listOf(320 to 1f, 360 to 1.3f, 400 to 1.5f).forEach { (screenWidth, textScale) ->
            compose.runOnUiThread {
                widthDp.intValue = screenWidth
                fontScale.floatValue = textScale
            }
            listOf(1, 4, 8, 16, 24).forEach { slotCount ->
                compose.runOnUiThread { count.intValue = slotCount }
                compose.waitForIdle()
                compose.onNode(hasScrollToIndexAction()).performScrollToIndex(slotCount - 1)
                val lastIndex = slotCount - 1
                val description = context.getString(
                    R.string.FilamentPaletteSlotDescription,
                    lastIndex,
                    "Synthetic PLA $lastIndex with a long material label",
                )
                val slot = compose.onNodeWithContentDescription(description).assertIsDisplayed()
                slot.assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
                slot.performClick()
                compose.runOnIdle { assertEquals(lastIndex, selectedIndex) }
            }
        }
    }

    @Test
    fun twoRowToolMenuKeepsLastActionReachableAtNarrowWidthAndLargeText() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val actionTitles = listOf(
            R.string.MenuCameraIsometric, R.string.MenuCameraTop, R.string.MenuCameraBottom,
            R.string.MenuCameraFront, R.string.MenuCameraBack, R.string.MenuCameraLeft,
            R.string.MenuCameraRight, R.string.MenuCameraOrtho, R.string.MenuCameraControlMode,
            R.string.MenuModel,
        )
        var selectedTitle = 0
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1.5f)) {
                BeamComposeTheme {
                    Box(Modifier.width(320.dp).height(150.dp)) {
                        BedMenuActionsContent(
                            actions = actionTitles.map { title ->
                                BedComposeMenuAction(
                                    titleRes = title,
                                    iconRes = R.drawable.camera_mode_0_28,
                                    onClick = { selectedTitle = title },
                                )
                            },
                            portrait = true,
                            multiRow = true,
                        )
                    }
                }
            }
        }

        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(actionTitles.lastIndex)
        val lastTitle = context.getString(actionTitles.last())
        val lastAction = compose.onNodeWithContentDescription(lastTitle).assertIsDisplayed()
        lastAction.assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        lastAction.performClick()
        compose.runOnIdle { assertEquals(actionTitles.last(), selectedTitle) }
    }
}
