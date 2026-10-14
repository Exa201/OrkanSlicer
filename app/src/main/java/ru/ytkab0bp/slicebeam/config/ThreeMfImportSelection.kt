package ru.ytkab0bp.slicebeam.config

import ru.ytkab0bp.slicebeam.utils.FilamentSlot

/** Keep file colors visible while choosing print and material values independently. */
internal object ThreeMfImportSelection {
    fun select(
        source: ThreeMfProjectSettings,
        printSettings: Boolean,
        filamentSettings: Boolean,
        ownSlots: List<FilamentSlot>,
        printerSettings: Boolean,
    ): ThreeMfProjectSettings? {
        val colors = source.filaments.mapIndexed { index, row ->
            val own = ownSlots.getOrNull(index) ?: ownSlots.lastOrNull()
            mapOf(
                "filament_colour" to (row["filament_colour"] ?: own?.let { TemporaryProjectSettings.colorText(it.color) } ?: "#1AC5A2"),
                "filament_type" to (own?.type ?: "PLA"),
            )
        }
        val process = if (printSettings) source.process.filterKeys { it != "curr_bed_type" } else emptyMap()
        val plate = if (printerSettings) source.process["curr_bed_type"]?.let { mapOf("curr_bed_type" to it) }.orEmpty()
            else emptyMap()
        val selected = source.copy(
            process = process + plate,
            printSettingsImported = printSettings,
            printerSettingsImported = printerSettings,
            filaments = if (filamentSettings) source.filaments else colors,
            filamentSettingsImported = filamentSettings,
            preferOwnFilamentTemperatures = !filamentSettings,
        )
        return selected.takeIf { it.hasSettings }
    }
}
