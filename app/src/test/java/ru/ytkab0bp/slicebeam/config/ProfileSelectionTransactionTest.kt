package ru.ytkab0bp.slicebeam.config

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ru.ytkab0bp.slicebeam.fragment.ProfileConfigValueController
import ru.ytkab0bp.slicebeam.slic3r.ConfigOptionDef
import ru.ytkab0bp.slicebeam.slic3r.Slic3rConfigWrapper

class ProfileSelectionTransactionTest {
    @Before
    fun clearDraftsBeforeTest() {
        ProfileSettingsRepository.clearAllDrafts()
    }

    @After
    fun clearDraftsAfterTest() {
        ProfileSettingsRepository.clearAllDrafts()
    }

    @Test
    fun printerSwitchSavesDraftsToTheOldProfilesBeforeChangingSelection() {
        val config = sampleConfig()
        ProfileSettingsRepository.setPendingValue(
            ConfigObject.PROFILE_LIST_PRINTER, "z_offset", "0", "0.15",
        )
        val savedPrinter = config.findPrinter("Printer A")!!
        ProfileConfigValueController.stageValue(
            ConfigObject.PROFILE_LIST_PRINTER, savedPrinter, "nozzle_diameter",
            ConfigOptionDef.GUIType.UNDEFINED, 0, "0.5",
        )
        ProfileConfigValueController.stageValue(
            ConfigObject.PROFILE_LIST_PRINTER, savedPrinter, "nozzle_diameter",
            ConfigOptionDef.GUIType.UNDEFINED, 1, "0.8",
        )
        ProfileSettingsRepository.setPendingValue(
            ConfigObject.PROFILE_LIST_PRINT, "layer_height", "0.20", "0.24",
        )
        ProfileSettingsRepository.setPendingValue(
            ConfigObject.PROFILE_LIST_FILAMENT, "pressure_advance", "0.04", "0.08",
        )
        var persisted = ""

        val applied = ProfileSelectionTransaction.apply(
            config = config,
            printer = "Printer B",
            filament = null,
            process = null,
            synchronizeCompatible = { false },
            persist = { persisted = config.serialize(); true },
        )

        assertTrue(applied)
        assertEquals("Printer B", config.presets?.get("printer"))
        assertEquals("0.15", config.findPrinter("Printer A")?.get("z_offset"))
        assertEquals("0.5,0.8", config.findPrinter("Printer A")?.get("nozzle_diameter"))
        assertEquals("0.24", config.findPrint("Balanced")?.get("layer_height"))
        assertEquals("0.08", config.findFilament("PLA")?.get("pressure_advance"))
        assertEquals(0, ProfileSettingsRepository.pendingCountFor(ConfigObject.PROFILE_LIST_PRINTER))
        assertEquals(0, ProfileSettingsRepository.pendingCountFor(ConfigObject.PROFILE_LIST_PRINT))
        assertEquals(0, ProfileSettingsRepository.pendingCountFor(ConfigObject.PROFILE_LIST_FILAMENT))
        val reloaded = Slic3rConfigWrapper(persisted.byteInputStream())
        assertEquals("Printer B", reloaded.presets?.get("printer"))
        assertEquals("0.15", reloaded.findPrinter("Printer A")?.get("z_offset"))
        assertEquals("0.5,0.8", reloaded.findPrinter("Printer A")?.get("nozzle_diameter"))
    }

    @Test
    fun failedWriteRestoresProfilesAndSelectionAndKeepsDrafts() {
        val config = sampleConfig()
        ProfileSettingsRepository.setPendingValue(
            ConfigObject.PROFILE_LIST_PRINTER, "z_offset", "0", "0.15",
        )
        val originalPresets = HashMap(config.presets!!.values)

        val applied = ProfileSelectionTransaction.apply(
            config = config,
            printer = "Printer B",
            filament = null,
            process = null,
            synchronizeCompatible = { false },
            persist = { false },
        )

        assertFalse(applied)
        assertEquals(originalPresets, config.presets!!.values)
        assertEquals("0", config.findPrinter("Printer A")?.get("z_offset"))
        assertEquals("0.15", ProfileSettingsRepository.pendingEntries(ConfigObject.PROFILE_LIST_PRINTER)["z_offset"])
    }

    @Test
    fun selectingTheAlreadyActiveProfilePersistsPendingEditsInsteadOfDroppingThem() {
        val config = sampleConfig()
        ProfileSettingsRepository.setPendingValue(
            ConfigObject.PROFILE_LIST_PRINT, "layer_height", "0.20", "0.28",
        )
        var writes = 0

        val applied = ProfileSelectionTransaction.apply(
            config = config,
            printer = "Printer A",
            filament = null,
            process = null,
            synchronizeCompatible = { false },
            persist = { writes++; true },
        )

        assertTrue(applied)
        assertEquals(1, writes)
        assertEquals("0.28", config.findPrint("Balanced")?.get("layer_height"))
        assertEquals(0, ProfileSettingsRepository.pendingCountFor(ConfigObject.PROFILE_LIST_PRINT))
    }

    @Test
    fun invalidSelectionDoesNotCommitOrClearDrafts() {
        val config = sampleConfig()
        ProfileSettingsRepository.setPendingValue(
            ConfigObject.PROFILE_LIST_PRINTER, "z_offset", "0", "0.15",
        )
        var writes = 0

        val applied = ProfileSelectionTransaction.apply(
            config = config,
            printer = "Missing printer",
            filament = null,
            process = null,
            synchronizeCompatible = { false },
            persist = { writes++; true },
        )

        assertFalse(applied)
        assertEquals(0, writes)
        assertEquals("Printer A", config.presets?.get("printer"))
        assertEquals("0", config.findPrinter("Printer A")?.get("z_offset"))
        assertEquals(1, ProfileSettingsRepository.pendingCountFor(ConfigObject.PROFILE_LIST_PRINTER))
    }

    private fun sampleConfig(): Slic3rConfigWrapper = Slic3rConfigWrapper().apply {
        printerConfigs.add(ConfigObject("Printer A").apply {
            profileListType = ConfigObject.PROFILE_LIST_PRINTER
            put("z_offset", "0")
            put("nozzle_diameter", "0.4,0.6")
        })
        printerConfigs.add(ConfigObject("Printer B").apply {
            profileListType = ConfigObject.PROFILE_LIST_PRINTER
            put("z_offset", "0.1")
        })
        printConfigs.add(ConfigObject("Balanced").apply {
            profileListType = ConfigObject.PROFILE_LIST_PRINT
            put("layer_height", "0.20")
        })
        filamentConfigs.add(ConfigObject("PLA").apply {
            profileListType = ConfigObject.PROFILE_LIST_FILAMENT
            put("pressure_advance", "0.04")
        })
        presets = ConfigObject().apply {
            put("printer", "Printer A")
            put("print", "Balanced")
            put("filament", "PLA")
        }
    }
}
