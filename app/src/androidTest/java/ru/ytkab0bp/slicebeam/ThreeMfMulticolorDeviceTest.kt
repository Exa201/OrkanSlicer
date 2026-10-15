package ru.ytkab0bp.slicebeam

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.config.ThreeMfProjectSettingsReader
import ru.ytkab0bp.slicebeam.config.ThreeMfImportSelection
import ru.ytkab0bp.slicebeam.config.TemporaryProjectSettings
import ru.ytkab0bp.slicebeam.config.ProfileSettingsRepository
import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.slic3r.Model
import ru.ytkab0bp.slicebeam.slic3r.GLModel
import ru.ytkab0bp.slicebeam.slic3r.GCodeProcessorResult
import ru.ytkab0bp.slicebeam.slic3r.PrintConfigDef
import ru.ytkab0bp.slicebeam.slic3r.Slic3rConfigWrapper
import ru.ytkab0bp.slicebeam.ui.settings.OrcaFilamentSettingsCatalog
import ru.ytkab0bp.slicebeam.ui.settings.OrcaProcessSettingsCatalog
import ru.ytkab0bp.slicebeam.utils.FilamentSlot
import java.io.File
import org.junit.After

@RunWith(AndroidJUnit4::class)
class ThreeMfMulticolorDeviceTest {
    @After fun clearProject() { TemporaryProjectSettings.clear(); ProfileSettingsRepository.clearAllDrafts() }
    @Test fun realThreeColorProjectKeepsOwnTemperaturesWhenOnlyItsColorsAreImported() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = File(context.getExternalFilesDir(null), "user-multicolor-repro.3mf")
        assertTrue(source.isFile)
        val definitions = PrintConfigDef.getInstance().options
        val processKeys = Slic3rConfigWrapper.PRINT_CONFIG_KEYS.toSet() +
            OrcaProcessSettingsCatalog.categories().flatMap { it.sections }.flatMap { it.keys }
        val filamentKeys = Slic3rConfigWrapper.FILAMENT_CONFIG_KEYS.toSet() +
            OrcaFilamentSettingsCatalog.categories().flatMap { it.sections }.flatMap { it.keys }
        val fileSettings = ThreeMfProjectSettingsReader.read(source, definitions, processKeys, filamentKeys)
        val own = listOf(FilamentSlot(0x111111, "PLA"), FilamentSlot(0x222222, "PETG"),
            FilamentSlot(0x333333, "PLA"))
        val selected = requireNotNull(ThreeMfImportSelection.select(fileSettings,
            printSettings = false, filamentSettings = false, ownSlots = own, printerSettings = false))
        assertEquals(3, selected.filaments.size)
        assertTrue(selected.process.isEmpty())
        assertEquals(listOf("PLA", "PETG", "PLA"), selected.filaments.map { it["filament_type"] })
        assertTrue(selected.filaments.none { "nozzle_temperature" in it || "hot_plate_temp" in it })
        TemporaryProjectSettings.activate(selected)
        val effective = ConfigObject().apply {
            put("nozzle_temperature", "205,238,210")
            put("hot_plate_temp", "55,75,60")
            put("layer_height", "0.16")
        }
        TemporaryProjectSettings.applyTo(effective, 3)
        assertEquals("205,238,210", effective["nozzle_temperature"])
        assertEquals("55,75,60", effective["hot_plate_temp"])
        assertEquals("0.16", effective["layer_height"])
        assertEquals("#C8C8C8;#8671CB;#D02727", effective["filament_colour"])
    }

    @Test fun inspectUserProjectAgainstActualNativeSchema() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.getExternalFilesDir(null), "user-multicolor-repro.3mf")
        assertTrue("Reproduction file must be supplied explicitly", file.isFile)
        val definitions = PrintConfigDef.getInstance().options
        val processKeys = Slic3rConfigWrapper.PRINT_CONFIG_KEYS.toSet() +
            OrcaProcessSettingsCatalog.categories().flatMap { it.sections }.flatMap { it.keys }
        val filamentKeys = Slic3rConfigWrapper.FILAMENT_CONFIG_KEYS.toSet() +
            OrcaFilamentSettingsCatalog.categories().flatMap { it.sections }.flatMap { it.keys }
        val settings = ThreeMfProjectSettingsReader.read(file, definitions, processKeys, filamentKeys)
        val model = Model(file, 1)
        try {
            val report = mapOf("filaments" to settings.filaments.size, "ignored" to settings.ignoredValues,
                "invalid" to settings.invalidValues, "definitions" to definitions,
                "objects" to (0 until model.objectsCount).map { mapOf("base" to model.getExtruder(it), "paint" to model.paintMaxFilament(it)) },
                "requiredFilaments" to model.requiredFilamentCount)
            File(context.getExternalFilesDir(null), "multicolor-diagnosis.json").writeText(Gson().toJson(report))
            assertEquals(3, settings.filaments.size)
            assertEquals("Every Bambu value is compatible after migration", 0, settings.ignoredValues)
            assertEquals(3, model.requiredFilamentCount)
            assertArrayEquals(intArrayOf(1,2,3), model.getUsedFilaments(0))
            val expectedTriangles = listOf(523066,432058,1966)
            for (filament in 1..3) {
                val overlay = GLModel()
                try { assertEquals("Every part appears in its original color", expectedTriangles[filament-1], model.buildPaintOverlay(0, overlay, filament)) }
                finally { overlay.release() }
            }
            model.setUniformFilament(0, 2)
            assertArrayEquals(intArrayOf(2), model.getUsedFilaments(0))
            assertFalse(model.hasPaint(0))
        } finally { model.release() }
    }

    @Test fun sliceActualAssemblyUsesAllThreeToolsWithoutChangingSavedProfiles() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        SliceBeam.INSTANCE = context.applicationContext as SliceBeam
        val previous = SliceBeam.CONFIG
        context.assets.open("orca_profiles/Anycubic.ini").use { SliceBeam.CONFIG = Slic3rConfigWrapper(it) }
        SliceBeam.requireConfig().presets = ConfigObject("presets").apply {
            put("printer", "Anycubic Kobra 3 0.4 nozzle"); put("print", "0.16mm Standard"); put("filament", "Anycubic Generic PLA")
        }
        val source = File(context.getExternalFilesDir(null), "user-multicolor-repro.3mf")
        val defs = PrintConfigDef.getInstance().options
        val processKeys = Slic3rConfigWrapper.PRINT_CONFIG_KEYS.toSet() + OrcaProcessSettingsCatalog.categories().flatMap { it.sections }.flatMap { it.keys }
        val filamentKeys = Slic3rConfigWrapper.FILAMENT_CONFIG_KEYS.toSet() + OrcaFilamentSettingsCatalog.categories().flatMap { it.sections }.flatMap { it.keys }
        val settings = ThreeMfProjectSettingsReader.read(source, defs, processKeys, filamentKeys)
        val before = SliceBeam.requireConfig().serialize()
        val model = Model(source, 1)
        var result: GCodeProcessorResult? = null
        try {
            TemporaryProjectSettings.activate(settings)
            assertEquals(3, model.requiredFilamentCount)
            SliceBeam.genCurrentConfig(model)
            val destination = File(context.getExternalFilesDir(null), "user-multicolor-fixed.gcode")
            val reuse = InstrumentationRegistry.getArguments().getString("reuse_generated") == "true"
            if (!reuse || !destination.isFile)
                result = model.slice(SliceBeam.getCurrentConfigFile().absolutePath, destination.absolutePath, null)
            val tools = BooleanArray(3)
            val toolCommand = Regex("^T([0-2])(?:\\s|$)")
            var advertisedLayers = -1
            var writtenLayers = 0
            var complete = false
            var currentVersion = false
            var correctPalette = false
            destination.bufferedReader().useLines { lines -> lines.forEach { line ->
                if (line.startsWith("T")) toolCommand.find(line)?.groupValues?.get(1)?.toInt()?.let { tools[it] = true }
                if (line.startsWith("; total layer number: ")) advertisedLayers = line.substringAfter(": ").toInt()
                if (line == ";LAYER_CHANGE") writtenLayers++
                if (line.contains("CONFIG_BLOCK_END")) complete = true
                if (line.startsWith("; generated by ") && line.contains(BuildConfig.VERSION_NAME)) currentVersion = true
                if (line == "; filament_colour = #C8C8C8;#8671CB;#D02727") correctPalette = true
            } }
            for (tool in 0..2) assertTrue("Tool T$tool must be printed", tools[tool])
            assertTrue("File must belong to the newly built core", currentVersion)
            assertTrue("File must preserve the project's palette", correctPalette)
            assertTrue(advertisedLayers > 0)
            assertEquals(advertisedLayers, writtenLayers)
            assertTrue(complete)
            assertEquals(before, SliceBeam.requireConfig().serialize())
        } finally { result?.release(); model.release(); SliceBeam.CONFIG = previous }
    }
}
