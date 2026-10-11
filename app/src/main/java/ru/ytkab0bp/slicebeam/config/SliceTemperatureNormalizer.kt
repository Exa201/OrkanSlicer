package ru.ytkab0bp.slicebeam.config

/** The filament editor exposes one bed temperature, so every Orca plate must use it. */
object SliceTemperatureNormalizer {
    private val surfaces = listOf(
        "cool_plate_temp", "eng_plate_temp", "textured_plate_temp",
        "textured_cool_plate_temp", "supertack_plate_temp",
    )

    fun applyGeneralBedTemperature(config: ConfigObject) {
        for (suffix in listOf("", "_initial_layer")) {
            val general = config["hot_plate_temp$suffix"]?.takeIf { it.isNotBlank() } ?: continue
            for (surface in surfaces) config.put("$surface$suffix", general)
        }
    }
}
