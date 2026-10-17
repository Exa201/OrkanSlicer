package ru.ytkab0bp.slicebeam.components.bed_menu

import ru.ytkab0bp.slicebeam.config.ConfigObject

/** Maps a visually evaluated Orca calibration to the profile setting it measures. */
internal object CalibrationResultPolicy {
    data class Target(
        val profileType: Int,
        val key: String,
        val min: Double,
        val max: Double,
        val vectorIndex: Int = -1,
        val integerValue: Boolean = false,
    )

    fun targetFor(mode: Int): Target? = when (mode) {
        CalibrationPresets.PA_LINE, CalibrationPresets.PA_TOWER ->
            Target(ConfigObject.PROFILE_LIST_FILAMENT, "pressure_advance", 0.0, 2.0, 0)
        CalibrationPresets.TEMP_TOWER ->
            Target(ConfigObject.PROFILE_LIST_FILAMENT, "nozzle_temperature", 100.0, 300.0, 0, true)
        CalibrationPresets.VOLUMETRIC_SPEED_TOWER ->
            Target(ConfigObject.PROFILE_LIST_FILAMENT, "filament_max_volumetric_speed", 0.1, 100.0, 0)
        CalibrationPresets.VFA_TOWER ->
            Target(ConfigObject.PROFILE_LIST_PRINT, "outer_wall_speed", 1.0, 500.0)
        CalibrationPresets.RETRACTION_TOWER ->
            Target(ConfigObject.PROFILE_LIST_FILAMENT, "filament_retraction_length", 0.0, 20.0, 0)
        else -> null
    }

    fun parseValue(raw: String, target: Target): String? {
        val parsed = raw.trim().replace(',', '.').toDoubleOrNull() ?: return null
        if (!parsed.isFinite() || parsed !in target.min..target.max) return null
        if (target.integerValue && parsed % 1.0 != 0.0) return null
        if (target.integerValue) return parsed.toInt().toString()
        return java.lang.Double.toString(parsed)
    }
}
