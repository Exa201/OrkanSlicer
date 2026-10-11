package ru.ytkab0bp.slicebeam.components

/** Validated values passed from the LitoGEN form to its existing STL generation pipeline. */
internal data class LitoGenOptions(
    val widthMm: Float,
    val heightMm: Float,
    val minThicknessMm: Float,
    val maxThicknessMm: Float,
    val brightness: Int,
    val contrast: Int,
    val invert: Boolean,
    val detailIndex: Int,
)

internal enum class LitoGenValidation {
    SIZE,
    THICKNESS,
    BRIGHTNESS,
    CONTRAST,
    DETAIL,
}

/** Pure input policy shared by the Compose dialog and JVM tests. */
internal object LitoGenFormLogic {
    fun parseNumber(value: String): Float = try {
        value.trim().replace(',', '.').toFloat()
    } catch (_: Exception) {
        Float.NaN
    }

    fun validate(options: LitoGenOptions): LitoGenValidation? {
        if (!finiteRange(options.widthMm, 35f, 100f) || !finiteRange(options.heightMm, 35f, 100f)) {
            return LitoGenValidation.SIZE
        }
        if (!finiteRange(options.minThicknessMm, .8f, 3f) ||
            !finiteRange(options.maxThicknessMm, 2f, 6f) || options.maxThicknessMm <= options.minThicknessMm
        ) {
            return LitoGenValidation.THICKNESS
        }
        if (options.brightness !in -50..50) return LitoGenValidation.BRIGHTNESS
        if (options.contrast !in 50..180) return LitoGenValidation.CONTRAST
        if (options.detailIndex !in 0..3) return LitoGenValidation.DETAIL
        return null
    }

    private fun finiteRange(value: Float, min: Float, max: Float): Boolean =
        value.isFinite() && value >= min && value <= max
}
