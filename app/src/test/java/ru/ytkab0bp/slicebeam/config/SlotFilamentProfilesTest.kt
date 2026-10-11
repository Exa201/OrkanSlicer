package ru.ytkab0bp.slicebeam.config

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.ytkab0bp.slicebeam.slic3r.ConfigOptionDef
import ru.ytkab0bp.slicebeam.slic3r.ConfigOptionDef.ConfigOptionType

class SlotFilamentProfilesTest {
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
