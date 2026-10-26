package ru.ytkab0bp.slicebeam

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.config.ProfileSettingsRepository
import ru.ytkab0bp.slicebeam.config.TemporaryProjectSettings
import ru.ytkab0bp.slicebeam.config.ThreeMfImportSelection
import ru.ytkab0bp.slicebeam.config.ThreeMfProjectSettingsReader
import ru.ytkab0bp.slicebeam.slic3r.GCodeProcessorResult
import ru.ytkab0bp.slicebeam.slic3r.Model
import ru.ytkab0bp.slicebeam.slic3r.PrintConfigDef
import ru.ytkab0bp.slicebeam.slic3r.Slic3rConfigWrapper
import ru.ytkab0bp.slicebeam.render.FillBedModelOperation
import ru.ytkab0bp.slicebeam.ui.settings.OrcaFilamentSettingsCatalog
import ru.ytkab0bp.slicebeam.ui.settings.OrcaProcessSettingsCatalog
import ru.ytkab0bp.slicebeam.utils.FilamentSlot
import ru.ytkab0bp.slicebeam.utils.Vec3d
import java.io.File

/** Reproducible bundled 3MF fixture keeps multicolor integration coverage runnable on every device. */
@RunWith(AndroidJUnit4::class)
class ThreeMfMulticolorDeviceTest {
    @After fun clearProject() {
        TemporaryProjectSettings.clear()
        ProfileSettingsRepository.clearAllDrafts()
    }

    @Test fun generatedThreeColorProjectKeepsOwnTemperaturesWhenOnlyItsColorsAreImported() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = copyFixture(context.cacheDir, "three-color-import.3mf")
        try {
            val settings = readSettings(source)
            assertEquals(3, settings.filaments.size)
            assertEquals(0, settings.ignoredValues)

            val own = listOf(
                FilamentSlot(0x111111, "PLA"),
                FilamentSlot(0x222222, "PETG"),
                FilamentSlot(0x333333, "PLA"),
            )
            val selected = requireNotNull(
                ThreeMfImportSelection.select(
                    settings,
                    printSettings = false,
                    filamentSettings = false,
                    ownSlots = own,
                    printerSettings = false,
                ),
            )

            assertEquals(3, selected.filaments.size)
            assertTrue(selected.process.isEmpty())
            assertEquals(listOf("PLA", "PETG", "PLA"), selected.filaments.map { it["filament_type"] })
            assertEquals(
                listOf("#C8C8C8", "#8671CB", "#D02727"),
                selected.filaments.map { it["filament_colour"] },
            )
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
        } finally {
            source.delete()
        }
    }

    @Test fun generatedThreeColorProjectUsesThreeNativeModelAssignments() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = copyFixture(context.cacheDir, "three-color-native.3mf")
        var model: Model? = null
        try {
            val settings = readSettings(source)
            model = Model(source, 1)

            assertEquals(3, settings.filaments.size)
            assertEquals(0, settings.ignoredValues)
            assertEquals(3, model.objectsCount)
            assertEquals(3, model.requiredFilamentCount)
            for (objectIndex in 0 until model.objectsCount) {
                assertArrayEquals(
                    "Each cube must retain its own 3MF tool assignment",
                    intArrayOf(objectIndex + 1),
                    model.getUsedFilaments(objectIndex),
                )
            }
        } finally {
            model?.release()
            source.delete()
        }
    }

    @Test fun generatedThreeColorProjectSlicesAllToolsAndPreservesSavedProfiles() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        SliceBeam.INSTANCE = context.applicationContext as SliceBeam
        val previousConfig = SliceBeam.CONFIG
        val configFile = SliceBeam.getCurrentConfigFile()
        val previousConfigFile = configFile.takeIf { it.isFile }?.readBytes()
        val source = copyFixture(context.cacheDir, "three-color-slice.3mf")
        val destination = File(context.cacheDir, "three-color-slice.gcode")
        var model: Model? = null
        var result: GCodeProcessorResult? = null
        try {
            context.assets.open("orca_profiles/Anycubic.ini").use {
                SliceBeam.CONFIG = Slic3rConfigWrapper(it)
            }
            // This regression targets 3MF color/tool routing. The bundled Anycubic PLA profile
            // contains Bambu-only start-G-code syntax, so keep the disposable test profile valid
            // for Orca's native parser without touching any saved user profile.
            requireNotNull(SliceBeam.requireConfig().findFilament("Anycubic Generic PLA"))
                .put("filament_start_gcode", "")
            SliceBeam.requireConfig().presets = ConfigObject("presets").apply {
                put("printer", "Anycubic Kobra 3 0.4 nozzle")
                put("print", "0.16mm Standard")
                put("filament", "Anycubic Generic PLA")
            }
            val settings = readSettings(source)
            val before = SliceBeam.requireConfig().serialize()
            model = Model(source, 1)
            TemporaryProjectSettings.activate(settings)
            assertEquals(3, model.requiredFilamentCount)

            SliceBeam.genCurrentConfig(model)
            result = model.slice(SliceBeam.getCurrentConfigFile().absolutePath, destination.absolutePath, null)

            val tools = BooleanArray(3)
            val toolCommand = Regex("^T([0-2])(?:\\s|$)")
            var advertisedLayers = -1
            var writtenLayers = 0
            var complete = false
            var currentVersion = false
            var correctPalette = false
            destination.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    if (line.startsWith("T")) {
                        toolCommand.find(line)?.groupValues?.get(1)?.toInt()?.let { tools[it] = true }
                    }
                    if (line.startsWith("; total layer number: ")) {
                        advertisedLayers = line.substringAfter(": ").toInt()
                    }
                    if (line == ";LAYER_CHANGE") writtenLayers++
                    if (line.contains("CONFIG_BLOCK_END")) complete = true
                    if (line.startsWith("; generated by ") && line.contains(BuildConfig.VERSION_NAME)) currentVersion = true
                    if (line == "; filament_colour = #C8C8C8;#8671CB;#D02727") correctPalette = true
                }
            }

            for (tool in 0..2) assertTrue("Tool T$tool must be used in generated G-code", tools[tool])
            assertTrue("G-code must be generated by this app version", currentVersion)
            assertTrue("G-code must contain the fixture palette", correctPalette)
            assertTrue("G-code must advertise a positive layer count", advertisedLayers > 0)
            assertEquals("Every advertised layer must be present", advertisedLayers, writtenLayers)
            assertTrue("G-code must contain the closing configuration block", complete)
            assertEquals("Project-local overrides must not mutate saved profiles", before, SliceBeam.requireConfig().serialize())
        } finally {
            result?.release()
            model?.release()
            destination.delete()
            source.delete()
            SliceBeam.CONFIG = previousConfig
            if (previousConfigFile == null) configFile.delete() else configFile.writeBytes(previousConfigFile)
        }
    }

    @Test fun fillBedCopiesSelectedThreeMfColorWithoutChangingOtherObjectsAndSlicesAllTools() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        SliceBeam.INSTANCE = context.applicationContext as SliceBeam
        val previousConfig = SliceBeam.CONFIG
        val configFile = SliceBeam.getCurrentConfigFile()
        val previousConfigFile = configFile.takeIf { it.isFile }?.readBytes()
        val source = copyFixture(context.cacheDir, "three-color-fill-bed-source.3mf")
        val project = File(context.cacheDir, "three-color-fill-bed-roundtrip.3mf")
        val destination = File(context.cacheDir, "three-color-fill-bed.gcode")
        var model: Model? = null
        var restored: Model? = null
        var result: GCodeProcessorResult? = null
        try {
            context.assets.open("orca_profiles/Anycubic.ini").use {
                SliceBeam.CONFIG = Slic3rConfigWrapper(it)
            }
            requireNotNull(SliceBeam.requireConfig().findFilament("Anycubic Generic PLA"))
                .put("filament_start_gcode", "")
            SliceBeam.requireConfig().presets = ConfigObject("presets").apply {
                put("printer", "Anycubic Kobra 3 0.4 nozzle")
                put("print", "0.16mm Standard")
                put("filament", "Anycubic Generic PLA")
            }
            model = Model(source, 1)
            assertEquals(3, model.objectsCount)
            val originalAssignments = listOf(intArrayOf(1), intArrayOf(2), intArrayOf(3))
            originalAssignments.forEachIndexed { index, assignment ->
                assertArrayEquals("source object $index should retain its original slot", assignment,
                    model.getUsedFilaments(index))
            }

            val added = FillBedModelOperation.run(
                activeModel = model,
                objectIndex = 1,
                viewerEnabled = false,
                estimatedCapacity = 5,
            ) { candidate ->
                for (index in 0 until candidate.objectsCount) {
                    val current = Vec3d()
                    candidate.getTranslation(index, current)
                    candidate.translate(index, index * 30.0 - current.x, -current.y, -current.z)
                }
                candidate.objectsCount <= 5
            }
            assertEquals("two copies of the selected model should be committed", 2, added)
            assertEquals(5, model.objectsCount)
            listOf(intArrayOf(1), intArrayOf(2), intArrayOf(3), intArrayOf(2), intArrayOf(2))
                .forEachIndexed { index, expected ->
                    assertArrayEquals("bed-filled object $index should preserve the correct source slot", expected,
                        model.getUsedFilaments(index))
                }

            SliceBeam.genCurrentConfig(model)
            model.export3mf(configFile.absolutePath, project.absolutePath)
            restored = Model(project, 1)
            assertEquals(5, restored.objectsCount)
            listOf(intArrayOf(1), intArrayOf(2), intArrayOf(3), intArrayOf(2), intArrayOf(2))
                .forEachIndexed { index, expected ->
                    assertArrayEquals("3MF reload should preserve the slot for object $index", expected,
                        restored.getUsedFilaments(index))
                }

            result = restored.slice(configFile.absolutePath, destination.absolutePath, null)
            val toolsUsed = BooleanArray(3)
            val toolCommand = Regex("^T([0-2])(?:\\s|$)")
            destination.useLines { lines -> lines.forEach { line ->
                toolCommand.find(line)?.groupValues?.get(1)?.toInt()?.let { toolsUsed[it] = true }
            } }
            for (tool in toolsUsed.indices) {
                assertTrue("Slicing the filled multicolor project must emit T$tool", toolsUsed[tool])
            }
            assertTrue("Slicing must produce deposited extrusion", destination.useLines { lines ->
                lines.any { it.startsWith("G1 ") && Regex("\\bE[0-9.-]+").containsMatchIn(it) }
            })
        } finally {
            result?.release()
            restored?.release()
            model?.release()
            destination.delete()
            project.delete()
            source.delete()
            SliceBeam.CONFIG = previousConfig
            if (previousConfigFile == null) configFile.delete() else configFile.writeBytes(previousConfigFile)
        }
    }

    @Test fun individuallyAssignedObjectsSliceWithTheirSelectedSlotsAndPalette() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        SliceBeam.INSTANCE = context.applicationContext as SliceBeam
        val previousConfig = SliceBeam.CONFIG
        val configFile = SliceBeam.getCurrentConfigFile()
        val previousConfigFile = configFile.takeIf { it.isFile }?.readBytes()
        val source = copyFixture(context.cacheDir, "three-color-individual-assignments.3mf")
        val destination = File(context.cacheDir, "three-color-individual-assignments.gcode")
        var model: Model? = null
        var result: GCodeProcessorResult? = null
        try {
            context.assets.open("orca_profiles/Anycubic.ini").use {
                SliceBeam.CONFIG = Slic3rConfigWrapper(it)
            }
            requireNotNull(SliceBeam.requireConfig().findFilament("Anycubic Generic PLA"))
                .put("filament_start_gcode", "")
            SliceBeam.requireConfig().presets = ConfigObject("presets").apply {
                put("printer", "Anycubic Kobra 3 0.4 nozzle")
                put("print", "0.16mm Standard")
                put("filament", "Anycubic Generic PLA")
            }
            model = Model(source, 1)
            val assignments = intArrayOf(4, 1, 2) // visible slot 4, slot 1, slot 2
            assignments.forEachIndexed { index, oneBasedSlot -> model.setUniformFilament(index, oneBasedSlot) }
            assertEquals(4, model.requiredFilamentCount)
            assertArrayEquals(intArrayOf(4), model.getUsedFilaments(0))
            assertArrayEquals(intArrayOf(1), model.getUsedFilaments(1))
            assertArrayEquals(intArrayOf(2), model.getUsedFilaments(2))

            SliceBeam.genCurrentConfig(model)
            val configColors = configFile.useLines { lines ->
                lines.lastOrNull { it.startsWith("filament_colour = ") }?.substringAfter("filament_colour = ")
            }
            val expectedPreviewColors = ru.ytkab0bp.slicebeam.utils.SliceFilamentColorResolver.resolve(
                configColors, model.requiredFilamentCount, ru.ytkab0bp.slicebeam.utils.Prefs.getFilamentPalette(),
            )
            result = model.slice(configFile.absolutePath, destination.absolutePath, null)
            assertArrayEquals(
                "G-code tool preview must retain the exact palette passed to native slicing",
                expectedPreviewColors,
                result.previewToolColors,
            )

            val toolsUsed = BooleanArray(4)
            val toolCommand = Regex("^T([0-3])(?:\\s|$)")
            destination.useLines { lines -> lines.forEach { line ->
                toolCommand.find(line)?.groupValues?.get(1)?.toInt()?.let { toolsUsed[it] = true }
            } }
            assertTrue("Slot 1 must be emitted as T0", toolsUsed[0])
            assertTrue("Slot 2 must be emitted as T1", toolsUsed[1])
            assertFalse("Unassigned slot 3 must not be emitted", toolsUsed[2])
            assertTrue("Slot 4 must be emitted as T3", toolsUsed[3])
        } finally {
            result?.release()
            model?.release()
            destination.delete()
            source.delete()
            SliceBeam.CONFIG = previousConfig
            if (previousConfigFile == null) configFile.delete() else configFile.writeBytes(previousConfigFile)
        }
    }

    @Test fun separateObjectsAssignedToOneSharedWhiteSlotSliceOnlyThatTool() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        SliceBeam.INSTANCE = context.applicationContext as SliceBeam
        val previousConfig = SliceBeam.CONFIG
        val configFile = SliceBeam.getCurrentConfigFile()
        val previousConfigFile = configFile.takeIf { it.isFile }?.readBytes()
        val previousSlots = ru.ytkab0bp.slicebeam.utils.Prefs.getFilamentSlots()
        val source = copyFixture(context.cacheDir, "three-color-shared-white.3mf")
        val destination = File(context.cacheDir, "three-color-shared-white.gcode")
        var model: Model? = null
        var result: GCodeProcessorResult? = null
        try {
            context.assets.open("orca_profiles/Anycubic.ini").use {
                SliceBeam.CONFIG = Slic3rConfigWrapper(it)
            }
            requireNotNull(SliceBeam.requireConfig().findFilament("Anycubic Generic PLA"))
                .put("filament_start_gcode", "")
            SliceBeam.requireConfig().presets = ConfigObject("presets").apply {
                put("printer", "Anycubic Kobra 3 0.4 nozzle")
                put("print", "0.16mm Standard")
                put("filament", "Anycubic Generic PLA")
            }
            ru.ytkab0bp.slicebeam.utils.Prefs.setFilamentSlots(listOf(
                FilamentSlot(0xFF212721.toInt(), "PETG"),
                FilamentSlot(0xFFFFFFFF.toInt(), "PLA"),
                FilamentSlot(0xFFFFFFFF.toInt(), "PLA"),
            ))
            model = Model(source, 1)
            assertEquals(3, model.objectsCount)
            for (objectIndex in 0 until model.objectsCount) model.setUniformFilament(objectIndex, 2)
            assertEquals(2, model.requiredFilamentCount)
            for (objectIndex in 0 until model.objectsCount) {
                assertArrayEquals("Object $objectIndex must use shared slicer slot 2", intArrayOf(2), model.getUsedFilaments(objectIndex))
            }

            SliceBeam.genCurrentConfig(model)
            result = model.slice(configFile.absolutePath, destination.absolutePath, null)
            val toolsUsed = BooleanArray(4)
            val toolCommand = Regex("^T([0-3])(?:\\s|$)")
            destination.useLines { lines -> lines.forEach { line ->
                toolCommand.find(line)?.groupValues?.get(1)?.toInt()?.let { toolsUsed[it] = true }
            } }
            assertTrue("Shared slot 2 must map to G-code tool T1", toolsUsed[1])
            assertFalse("Slot 1 must not appear after every object is remapped to slot 2", toolsUsed[0])
            assertFalse("Slot 3 must not appear after every object is remapped to slot 2", toolsUsed[2])
            assertFalse("Slot 4 must not appear after every object is remapped to slot 2", toolsUsed[3])
            assertTrue("Slicing must produce a non-empty G-code file", destination.length() > 0L)
        } finally {
            result?.release()
            model?.release()
            destination.delete()
            source.delete()
            ru.ytkab0bp.slicebeam.utils.Prefs.setFilamentSlots(previousSlots)
            SliceBeam.CONFIG = previousConfig
            if (previousConfigFile == null) configFile.delete() else configFile.writeBytes(previousConfigFile)
        }
    }

    private fun copyFixture(directory: File, name: String): File {
        val file = File(directory, name)
        InstrumentationRegistry.getInstrumentation().context.assets
            .open("models/three-color-project.3mf").use { input ->
                file.outputStream().use { input.copyTo(it) }
            }
        return file
    }

    private fun readSettings(file: File) = ThreeMfProjectSettingsReader.read(
        file,
        PrintConfigDef.getInstance().options,
        Slic3rConfigWrapper.PRINT_CONFIG_KEYS.toSet() +
            OrcaProcessSettingsCatalog.categories().flatMap { it.sections }.flatMap { it.keys },
        Slic3rConfigWrapper.FILAMENT_CONFIG_KEYS.toSet() +
            OrcaFilamentSettingsCatalog.categories().flatMap { it.sections }.flatMap { it.keys },
    )
}
