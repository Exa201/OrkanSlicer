package ru.ytkab0bp.slicebeam.config

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.ytkab0bp.slicebeam.slic3r.ConfigOptionDef
import ru.ytkab0bp.slicebeam.slic3r.ConfigOptionDef.ConfigOptionType

class SlotFilamentProfilesTest {
    @Test fun swappedAceGatesUseTheMatchingPhysicalSlotTemperatures() {
        val slotOne = ConfigObject("Slot 1").apply {
            put("nozzle_temperature", "205")
            put("hot_plate_temp", "55")
        }
        val slotTwo = ConfigObject("Slot 2").apply {
            put("nozzle_temperature", "245")
            put("hot_plate_temp", "80")
        }
        val profiles = listOf(slotOne, slotTwo)
        val swappedGates = intArrayOf(1, 0)
        val ordered = (0..1).map { tool ->
            profiles[SlotFilamentProfiles.profileSlotForTool(tool, swappedGates, profiles.size)]
        }
        val target = ConfigObject().apply { put("filament_colour", "#111111;#EEEEEE") }
        val definitions = mapOf(
            "nozzle_temperature" to ConfigOptionDef().apply { key = "nozzle_temperature"; type = ConfigOptionType.INTS },
            "hot_plate_temp" to ConfigOptionDef().apply { key = "hot_plate_temp"; type = ConfigOptionType.INTS },
        )

        SlotFilamentProfiles.applyProfileVectors(target, ordered, definitions, temperatureOnly = true)

        assertEquals("245,205", target["nozzle_temperature"])
        assertEquals("80,55", target["hot_plate_temp"])
        assertEquals("#111111;#EEEEEE", target["filament_colour"])
        assertEquals(0, SlotFilamentProfiles.profileSlotForTool(0, intArrayOf(3, 2), profiles.size))
    }

    @Test fun differentToolProfilesBecomeSeparateNativeValuesWithoutChangingPrinterOffsets() {
        val pla = ConfigObject("PLA").apply {
            put("nozzle_temperature", "205")
            put("pressure_advance", "0.02")
        }
        val petg = ConfigObject("PETG").apply {
            put("nozzle_temperature", "245")
            put("pressure_advance", "0.05")
        }
        val target = ConfigObject().apply { put("z_offset", "0.10") }
        val definitions = mapOf(
            "nozzle_temperature" to ConfigOptionDef().apply { key = "nozzle_temperature"; type = ConfigOptionType.INTS },
            "pressure_advance" to ConfigOptionDef().apply { key = "pressure_advance"; type = ConfigOptionType.FLOATS },
            "z_offset" to ConfigOptionDef().apply { key = "z_offset"; type = ConfigOptionType.FLOAT },
        )

        SlotFilamentProfiles.applyProfileVectors(target, listOf(pla, petg), definitions)

        assertEquals("205,245", target["nozzle_temperature"])
        assertEquals("0.02,0.05", target["pressure_advance"])
        assertEquals("0.10", target["z_offset"])
    }
}
