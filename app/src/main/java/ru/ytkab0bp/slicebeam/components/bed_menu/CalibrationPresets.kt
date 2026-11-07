package ru.ytkab0bp.slicebeam.components.bed_menu

import ru.ytkab0bp.slicebeam.R

/** Calibration modes supported by the bundled Orca core and safe to expose without printer commands. */
internal enum class CalibrationInputMode { RANGE, RANGE_AND_STEP, START_AND_STEP }

internal data class CalibrationPreset(
    val mode: Int,
    val titleRes: Int,
    val descriptionRes: Int,
    val inputMode: CalibrationInputMode,
    val start: Double,
    val endOrStep: Double,
    val step: Double? = null,
)

internal object CalibrationPresets {
    // Keep these numeric values aligned with Slic3r::CalibMode in libslic3r/calib.hpp.
    const val PA_LINE = 1
    const val PA_TOWER = 3
    const val TEMP_TOWER = 6
    const val VOLUMETRIC_SPEED_TOWER = 7
    const val VFA_TOWER = 8
    const val RETRACTION_TOWER = 9

    fun supported() = listOf(
        CalibrationPreset(PA_LINE, R.string.CalibrationPressureAdvance, R.string.CalibrationPressureAdvanceDescription,
            CalibrationInputMode.RANGE_AND_STEP, 0.0, 0.1, 0.002),
        CalibrationPreset(PA_TOWER, R.string.CalibrationPressureAdvanceTower, R.string.CalibrationPressureAdvanceTowerDescription,
            CalibrationInputMode.START_AND_STEP, 0.0, 0.005),
        CalibrationPreset(TEMP_TOWER, R.string.CalibrationTemperatureTower, R.string.CalibrationTemperatureTowerDescription,
            CalibrationInputMode.RANGE, 210.0, 190.0, 5.0),
        CalibrationPreset(VOLUMETRIC_SPEED_TOWER, R.string.CalibrationVolumetricSpeedTower, R.string.CalibrationVolumetricSpeedTowerDescription,
            CalibrationInputMode.START_AND_STEP, 20.0, 2.0),
        CalibrationPreset(VFA_TOWER, R.string.CalibrationVfaTower, R.string.CalibrationVfaTowerDescription,
            CalibrationInputMode.START_AND_STEP, 40.0, 5.0),
        CalibrationPreset(RETRACTION_TOWER, R.string.CalibrationRetractionTower, R.string.CalibrationRetractionTowerDescription,
            CalibrationInputMode.START_AND_STEP, 0.2, 0.1),
    )

    fun validate(preset: CalibrationPreset, start: Double, endOrStep: Double, step: Double?): Boolean {
        if (!start.isFinite() || !endOrStep.isFinite() || step?.isFinite() == false) return false
        if (preset.inputMode == CalibrationInputMode.RANGE || preset.inputMode == CalibrationInputMode.RANGE_AND_STEP) {
            if (kotlin.math.abs(endOrStep - start) <= 0.0) return false
            if (preset.mode == TEMP_TOWER) {
                return start in 100.0..300.0 && endOrStep in 100.0..300.0 && kotlin.math.abs(start - endOrStep) <= 100.0
            }
            val actualStep = step ?: return false
            if (actualStep <= 0.0 || kotlin.math.abs(endOrStep - start) < actualStep) return false
            val segments = kotlin.math.abs(endOrStep - start) / actualStep
            return start in 0.0..1.0 && endOrStep in 0.0..1.0 && segments <= 500.0
        }
        if (start < 0.0 || endOrStep <= 0.0) return false
        return when (preset.mode) {
            PA_TOWER -> start <= 1.0 && start + 20 * endOrStep <= 2.0
            VOLUMETRIC_SPEED_TOWER, VFA_TOWER -> start <= 500.0 && endOrStep <= 100.0
            RETRACTION_TOWER -> start <= 20.0 && endOrStep <= 5.0
            else -> false
        }
    }
}
