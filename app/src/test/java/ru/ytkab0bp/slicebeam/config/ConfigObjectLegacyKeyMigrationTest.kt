package ru.ytkab0bp.slicebeam.config

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.ytkab0bp.slicebeam.slic3r.Slic3rConfigWrapper
import ru.ytkab0bp.slicebeam.ui.settings.OrcaPrinterSettingsCatalog

class ConfigObjectLegacyKeyMigrationTest {
    @Test
    fun oldFeatureFilamentSelectorsMapToCurrentKeysAndInheritByDefault() {
        val selectors = mapOf(
            "perimeter_extruder" to "outer_wall_filament_id",
            "wall_filament" to "outer_wall_filament_id",
            "wall_filament_id" to "outer_wall_filament_id",
            "outer_wall_filament" to "outer_wall_filament_id",
            "inner_wall_filament" to "inner_wall_filament_id",
            "infill_extruder" to "sparse_infill_filament_id",
            "sparse_infill_filament" to "sparse_infill_filament_id",
            "solid_infill_extruder" to "internal_solid_filament_id",
            "solid_infill_filament" to "internal_solid_filament_id",
            "top_solid_infill_filament" to "top_surface_filament_id",
            "bottom_solid_infill_filament" to "bottom_surface_filament_id",
        )

        selectors.forEach { (legacyKey, currentKey) ->
            val oldDefault = ConfigObject().apply { put(legacyKey, "1") }
            val oldExplicitSlot = ConfigObject().apply { put(legacyKey, "2") }
            assertEquals("$legacyKey default", "0", oldDefault.get(currentKey))
            assertEquals("$legacyKey explicit slot", "2", oldExplicitSlot.get(currentKey))
        }
    }

    @Test
    fun currentFeatureFilamentIdsKeepSlotOneAndOldTopWallValuesBecomeBoolean() {
        val profile = ConfigObject().apply {
            put("outer_wall_filament_id", "1")
        }
        val oldTopEnabled = ConfigObject().apply { put("top_one_perimeter_type", "topmost") }
        val oldTopDisabled = ConfigObject().apply { put("top_one_wall_type", "none") }

        assertEquals("1", profile.get("outer_wall_filament_id"))
        assertEquals("1", oldTopEnabled.get("only_one_wall_top"))
        assertEquals("0", oldTopDisabled.get("only_one_wall_top"))
    }

    @Test
    fun everyOldOrcaAliasIsRecognizedDuringProcessProfileImport() {
        assertTrue(
            Slic3rConfigWrapper.PRINT_CONFIG_KEYS.containsAll(ConfigObject.printConfigKeyAliases())
        )
    }

    @Test
    fun combinedLegacyProfileRoutesAndNormalizesFeatureFilamentValues() {
        val text = """
            print_settings_id = Legacy process
            perimeter_extruder = 1
            sparse_infill_filament = 2
            solid_infill_extruder = 1
            top_one_wall_type = top
        """.trimIndent()
        val imported = Slic3rConfigWrapper(
            ByteArrayInputStream(text.toByteArray(StandardCharsets.UTF_8))
        )
        val process = imported.findPrint("Legacy process")

        assertEquals("0", process?.get("outer_wall_filament_id"))
        assertEquals("2", process?.get("sparse_infill_filament_id"))
        assertEquals("0", process?.get("internal_solid_filament_id"))
        assertEquals("1", process?.get("only_one_wall_top"))
    }

    @Test
    fun collisionVolumePrinterSettingsSurviveProfileRoundTripAndAreExposed() {
        val original = Slic3rConfigWrapper().apply {
            importPrinter(ConfigObject("Collision printer").apply {
                put("bed_exclude_volumes", "0..35;10x10,20x10,20x20,10x20")
                put("bed_exclude_volume_mode", "per_extruder")
                put("extruder_bed_exclude_volumes", "|30..;40x40,50x40,50x50,40x50")
                put("extruder_clearance_dist_to_rod", "56.5")
            })
        }

        val reloaded = Slic3rConfigWrapper(original.serialize().byteInputStream())
        val profile = reloaded.findPrinter("Collision printer")

        assertEquals("0..35;10x10,20x10,20x20,10x20", profile?.get("bed_exclude_volumes"))
        assertEquals("per_extruder", profile?.get("bed_exclude_volume_mode"))
        assertEquals("|30..;40x40,50x40,50x50,40x50", profile?.get("extruder_bed_exclude_volumes"))
        assertEquals("56.5", profile?.get("extruder_clearance_dist_to_rod"))
        assertTrue(OrcaPrinterSettingsCatalog.fixedKeys().containsAll(
            listOf("bed_exclude_volumes", "bed_exclude_volume_mode", "extruder_clearance_dist_to_rod")))
        assertTrue(OrcaPrinterSettingsCatalog.extruderSections().flatMap { it.keys }
            .contains("extruder_bed_exclude_volumes"))
    }
}
