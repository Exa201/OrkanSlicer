package ru.ytkab0bp.slicebeam.config

import org.junit.Assert.assertEquals
import org.junit.Test

class SliceTemperatureNormalizerTest {
    @Test
    fun generalBedTemperatureOverridesHiddenPlateValuesForEveryFilament() {
        val config = ConfigObject().apply {
            put("hot_plate_temp", "70,100,70")
            put("hot_plate_temp_initial_layer", "70,100,70")
            put("cool_plate_temp", "35,0,35")
            put("cool_plate_temp_initial_layer", "35,0,35")
            put("textured_plate_temp", "45,100,45")
            put("nozzle_temperature", "230,205,230")
        }

        SliceTemperatureNormalizer.applyGeneralBedTemperature(config)

        for (surface in listOf("cool_plate", "eng_plate", "textured_plate", "textured_cool_plate", "supertack_plate")) {
            assertEquals("70,100,70", config["${surface}_temp"])
            assertEquals("70,100,70", config["${surface}_temp_initial_layer"])
        }
        assertEquals("230,205,230", config["nozzle_temperature"])
    }
}
