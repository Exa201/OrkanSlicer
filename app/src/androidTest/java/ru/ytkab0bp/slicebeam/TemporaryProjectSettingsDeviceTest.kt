package ru.ytkab0bp.slicebeam

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Before
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.config.*
import ru.ytkab0bp.slicebeam.slic3r.*
import ru.ytkab0bp.slicebeam.ui.settings.OrcaFilamentSettingsCatalog
import ru.ytkab0bp.slicebeam.ui.settings.OrcaProcessSettingsCatalog
import ru.ytkab0bp.slicebeam.utils.*
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Real native schema, persistent storage and local slices in the isolated .slicetest app. */
@RunWith(AndroidJUnit4::class)
class TemporaryProjectSettingsDeviceTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val savedPreferences = LinkedHashMap<String, String?>()
    private var previousConfig: Slic3rConfigWrapper? = null
    private var savedFile: ByteArray? = null
    private val keys = listOf("filament_palette", "mmu_synced_printer", "mmu_choice_printer", "mmu_choice_gates")

    @Before fun setup() {
        TemporaryProjectSettings.clear()
        ProfileSettingsRepository.clearAllDrafts()
        SliceBeam.INSTANCE = context.applicationContext as SliceBeam
        Prefs.init(SliceBeam.requireInstance())
        keys.forEach { savedPreferences[it] = Prefs.getPrefs()!!.getString(it, null) }
        previousConfig = SliceBeam.CONFIG
        savedFile = SliceBeam.getConfigFile().takeIf { it.exists() }?.readBytes()
        context.assets.open("orca_profiles/Anycubic.ini").use { SliceBeam.CONFIG = Slic3rConfigWrapper(it) }
        SliceBeam.requireConfig().presets = ConfigObject("presets").apply {
            put("printer", "Anycubic Kobra 3 0.4 nozzle")
            put("print", "0.16mm Standard")
            put("filament", "Anycubic Generic PLA")
        }
        Prefs.setFilamentSlots(listOf(FilamentSlot(0xFF000000.toInt(), "ABS")))
        MmuFilamentSync.setSelectedGates("Anycubic Kobra 3 0.4 nozzle", intArrayOf(3))
        assertTrue(SliceBeam.saveConfig())
    }

    @After fun cleanup() {
        TemporaryProjectSettings.clear()
        ProfileSettingsRepository.clearAllDrafts()
        SliceBeam.CONFIG = previousConfig
        val editor = Prefs.getPrefs()!!.edit()
        savedPreferences.forEach { (key, value) -> if (value == null) editor.remove(key) else editor.putString(key, value) }
        editor.commit()
        savedFile?.let { SliceBeam.getConfigFile().writeBytes(it) } ?: SliceBeam.getConfigFile().delete()
    }

    private fun loadProject(
        liftAbove: List<String> = emptyList(),
        liftEnforce: List<String> = emptyList(),
    ): ThreeMfProjectSettings {
        val file = File(context.cacheDir, "temporary-project-device-test.3mf")
        try {
            val liftOverrides = buildString {
                if (liftAbove.isNotEmpty()) append(",\"filament_retract_lift_above\":${liftAbove.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }}")
                if (liftEnforce.isNotEmpty()) append(",\"filament_retract_lift_enforce\":${liftEnforce.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }}")
            }
            ZipOutputStream(file.outputStream()).use { zip ->
                zip.putNextEntry(ZipEntry("Metadata/project_settings.config"))
                zip.write("""{
                    "layer_height":"0.24", "initial_layer_print_height":"0.24", "wall_loops":"3",
                    "sparse_infill_density":"25%", "enable_prime_tower":false,
                    "filament_colour":["#112233","#FFEEDD"], "filament_type":["PLA","PETG"],
                    "nozzle_temperature":["211","236"], "nozzle_temperature_initial_layer":["212","237"],
                    "hot_plate_temp":["61","72"], "hot_plate_temp_initial_layer":["62","73"],
                    "pressure_advance":["0.04","0.06"], "enable_pressure_advance":[true,true],
                    "printer_settings_id":"Untrusted file printer",
                    "z_offset":"5", "print_host":"attacker.invalid", "host_type":"moonraker",
                    "printhost_apikey":"synthetic-project-secret", "printhost_webui":"https://attacker.invalid",
                    "nozzle_diameter":["0.8"],
                    "machine_start_gcode":"BAD" $liftOverrides }""".toByteArray())
                zip.closeEntry()
            }
            val processKeys = Slic3rConfigWrapper.PRINT_CONFIG_KEYS.toSet() +
                OrcaProcessSettingsCatalog.categories().flatMap { it.sections }.flatMap { it.keys }
            val filamentKeys = Slic3rConfigWrapper.FILAMENT_CONFIG_KEYS.toSet() +
                OrcaFilamentSettingsCatalog.categories().flatMap { it.sections }.flatMap { it.keys }
            return ThreeMfProjectSettingsReader.read(file, PrintConfigDef.getInstance().options, processKeys, filamentKeys)
                .also { assertTrue(it.hasSettings); assertEquals(0, it.ignoredValues) }
                .copy(preferOwnFilamentTemperatures = false)
        } finally { file.delete() }
    }

    @Test fun importedValuesAndPauseStyleSavingLeavePresetFileAndPreferencesUntouched() {
        val storedBytes = SliceBeam.getConfigFile().readBytes()
        val storedPalette = Prefs.getPrefs()!!.getString("filament_palette", null)
        val own = SliceBeam.buildCurrentConfigObject()
        TemporaryProjectSettings.activate(loadProject())
        assertEquals(2, Prefs.getFilamentSlots().size)
        assertEquals("PETG", Prefs.getFilamentSlots()[1].type)
        assertEquals(0xFF112233.toInt(), Prefs.getFilamentSlots()[0].color)
        val effective = SliceBeam.buildCurrentConfigObject()
        assertEquals("0.24", effective["layer_height"])
        assertEquals("211,236", effective["nozzle_temperature"])
        assertEquals("0.04,0.06", effective["pressure_advance"])
        assertEquals(own["z_offset"], effective["z_offset"])
        assertEquals(own["nozzle_diameter"], effective["nozzle_diameter"])
        assertEquals(own["machine_start_gcode"], effective["machine_start_gcode"])
        listOf("print_host", "host_type", "printhost_apikey", "printhost_webui")
            .forEach { key -> assertEquals("3MF must not replace physical printer setting $key", own[key], effective[key]) }
        assertEquals("Anycubic Kobra 3 0.4 nozzle", SliceBeam.requireConfig().presets?.get("printer"))
        TemporaryProjectSettings.selectFilament(1)
        ProfileSettingsRepository.setPendingValue(ConfigObject.PROFILE_LIST_FILAMENT, "nozzle_temperature", "236", "240")
        ProfileSettingsRepository.setPendingValue(ConfigObject.PROFILE_LIST_PRINT, "wall_loops", "3", "4")
        Prefs.setFilamentSlots(listOf(FilamentSlot(0xFFABCDEF.toInt(), "TPU")))
        MmuFilamentSync.setSelectedGates("Anycubic Kobra 3 0.4 nozzle", intArrayOf(0))
        Prefs.setMmuSyncedPrinter("Anycubic Kobra 3 0.4 nozzle")
        assertTrue(SliceBeam.savePendingProfileSettings())
        assertArrayEquals(storedBytes, SliceBeam.getConfigFile().readBytes())
        assertEquals(storedPalette, Prefs.getPrefs()!!.getString("filament_palette", null))
        assertEquals("3", Prefs.getPrefs()!!.getString("mmu_choice_gates", null))
        TemporaryProjectSettings.clear()
        assertEquals("ABS", Prefs.getFilamentSlots().single().type)
        assertEquals(own["layer_height"], SliceBeam.buildCurrentConfigObject()["layer_height"])
        assertArrayEquals(intArrayOf(3), MmuFilamentSync.getSelectedGates("Anycubic Kobra 3 0.4 nozzle", 1))
    }

    @Test fun selectingPresetDuringProjectUsesACopyWithoutPersistingTheSelection() {
        val bytes = SliceBeam.getConfigFile().readBytes()
        val selected = HashMap(SliceBeam.requireConfig().presets!!.values)
        TemporaryProjectSettings.activate(loadProject())
        assertTrue(SliceBeam.applyProfileSelection(process = "0.16mm Standard", filament = "Anycubic Generic PLA"))
        ProfileSettingsRepository.setPendingValue(ConfigObject.PROFILE_LIST_PRINT, "wall_loops", null, "7")
        assertTrue(SliceBeam.savePendingProfileSettings())
        assertEquals(selected, SliceBeam.requireConfig().presets!!.values)
        assertArrayEquals(bytes, SliceBeam.getConfigFile().readBytes())
        assertNotEquals("7", SliceBeam.requireConfig().findPrint("0.16mm Standard")!!["wall_loops"])
    }

    @Test fun projectSettingsReachNativeGcodeForBothFilamentsAndCompleteTheSlice() {
        TemporaryProjectSettings.activate(loadProject())
        val modelFile = File(context.cacheDir, "temporary-project-native.stl")
        val gcodeFile = File(context.cacheDir, "temporary-project-native.gcode")
        var model: Model? = null
        var result: GCodeProcessorResult? = null
        try {
            context.assets.open("models/box.stl").use { input -> modelFile.outputStream().use { input.copyTo(it) } }
            val loadedModel = Model(modelFile)
            model = loadedModel
            loadedModel.addObject(loadedModel, 0)
            loadedModel.translate(1, 30.0, 0.0, 0.0)
            loadedModel.setExtruder(0, 1)
            loadedModel.setExtruder(1, 2)
            assertEquals(2, loadedModel.requiredFilamentCount)
            SliceBeam.genCurrentConfig(loadedModel)
            val configText = SliceBeam.getCurrentConfigFile().readText()
            assertTrue(configText.contains("nozzle_temperature = 211,236"))
            assertTrue(configText.contains("pressure_advance = 0.04,0.06"))
            assertTrue(configText.contains("layer_height = 0.24"))
            result = loadedModel.slice(SliceBeam.getCurrentConfigFile().absolutePath, gcodeFile.absolutePath, null)
            val gcode = gcodeFile.readText()
            assertTrue(gcode.length > 1000)
            assertTrue("PLA temperature command", Regex("M10[49].*S21[12]\\b").containsMatchIn(gcode))
            assertTrue("PETG temperature command", Regex("M10[49].*S23[67]\\b").containsMatchIn(gcode))
            assertTrue("PLA pressure advance command", gcode.contains("SET_PRESSURE_ADVANCE ADVANCE=0.04"))
            assertTrue("PETG pressure advance command", gcode.contains("SET_PRESSURE_ADVANCE ADVANCE=0.06"))
            assertTrue("General bed temperature reaches the active plate's heating command", gcode.contains("G9111 bedTemp=62"))
            val advertisedLayers = Regex("; total layer number: (\\d+)").find(gcode)!!.groupValues[1].toInt()
            assertEquals("Every announced layer must be present", advertisedLayers, Regex("(?m)^;LAYER_CHANGE$").findAll(gcode).count())
            assertTrue("Final Orca config block", gcode.contains("CONFIG_BLOCK_END"))
            assertTrue("Tool 0 present", Regex("(?m)^T0(?:\\s|$)").containsMatchIn(gcode))
            assertTrue("Tool 1 present", Regex("(?m)^T1(?:\\s|$)").containsMatchIn(gcode))
            val savedGcode = File(context.getExternalFilesDir(null), "temporary-project-native.gcode")
            savedGcode.writeText(gcode)
        } finally {
            result?.release(); model?.release(); modelFile.delete(); gcodeFile.delete()
        }
    }

    @Test fun nativeGcodeUsesTheActiveFilamentsLiftThresholdAndEnforcement() {
        val modelFile = File(context.cacheDir, "filament-lift-overrides.stl")
        val gcodeFile = File(context.cacheDir, "filament-lift-overrides.gcode")
        var model: Model? = null
        var result: GCodeProcessorResult? = null
        try {
            context.assets.open("models/box.stl").use { input -> modelFile.outputStream().use { input.copyTo(it) } }
            val loadedModel = Model(modelFile)
            model = loadedModel
            loadedModel.addObject(loadedModel, 0)
            loadedModel.translate(1, 30.0, 0.0, 0.0)

            fun liftLayers(gcode: String): Set<Double> {
                val lifts = sortedSetOf<Double>()
                var layerZ = 0.0
                gcode.lineSequence().forEach { line ->
                    if (line.startsWith(";Z:")) {
                        layerZ = line.substringAfter(";Z:").toDouble()
                    } else if (layerZ > 0.0 && (line.startsWith("G1 ") || line.startsWith("G0 "))) {
                        val move = line.substringBefore(';')
                        Regex("\\bZ([-0-9.]+)").find(move)?.groupValues?.get(1)?.toDoubleOrNull()
                            ?.takeIf { it > layerZ + 0.3 }?.let { lifts += layerZ }
                    }
                }
                return lifts
            }

            // A single-nozzle printer with two logical filaments: each object is assigned
            // to the same active filament for one run, just as Orca's native regression.
            for ((setting, filamentId, values) in listOf(
                Triple("lift threshold", 1, listOf("0", "1")),
                Triple("lift threshold", 2, listOf("0", "1")),
                Triple("lift enforcement", 1, listOf("All Surfaces", "Bottom Only")),
                Triple("lift enforcement", 2, listOf("All Surfaces", "Bottom Only")),
            )) {
                TemporaryProjectSettings.activate(
                    if (setting == "lift threshold") loadProject(liftAbove = values)
                    else loadProject(liftEnforce = values),
                )
                loadedModel.setExtruder(0, filamentId)
                loadedModel.setExtruder(1, filamentId)
                SliceBeam.genCurrentConfig(loadedModel)
                val sliced = loadedModel.slice(SliceBeam.getCurrentConfigFile().absolutePath, gcodeFile.absolutePath, null)
                result = sliced
                val lifts = liftLayers(gcodeFile.readText())
                assertTrue("$setting for filament $filamentId should produce at least one lifted travel", lifts.isNotEmpty())
                if (setting == "lift threshold" && filamentId == 2) {
                    assertTrue("Filament 2 must honor its 1 mm lower Z-hop boundary", lifts.first() > 1.0 - 1e-6)
                } else if (setting == "lift enforcement" && filamentId == 2) {
                    assertTrue("Bottom-only filament must not lift after the first layer", lifts.last() <= 0.24 + 1e-6)
                } else {
                    assertTrue("Unrestricted filament should lift on early layers", lifts.first() <= 0.24 + 1e-6)
                    assertTrue("Unrestricted filament should lift on higher layers", lifts.last() > 1.0)
                }
                sliced.release()
                result = null
                gcodeFile.delete()
                SliceBeam.getCurrentConfigFile().delete()
            }
        } finally {
            result?.release()
            model?.release()
            modelFile.delete()
            gcodeFile.delete()
            SliceBeam.getCurrentConfigFile().delete()
        }
    }
}
