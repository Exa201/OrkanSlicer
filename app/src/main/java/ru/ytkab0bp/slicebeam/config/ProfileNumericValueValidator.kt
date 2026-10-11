package ru.ytkab0bp.slicebeam.config

/** Validation shared by numeric profile editors so Compose never stores values outside Orca's bounds. */
object ProfileNumericValueValidator {
    enum class Result { VALID, INVALID_NUMBER, OUT_OF_RANGE }

    @JvmStatic
    fun toggleSign(value: String): String {
        val normalized = value.trim()
        return when {
            normalized.startsWith("-") -> normalized.drop(1)
            normalized.startsWith("+") -> "-${normalized.drop(1)}"
            normalized.isEmpty() -> "-"
            else -> "-$normalized"
        }
    }

    @JvmStatic
    fun normalizeForStorage(value: String, integer: Boolean, percent: Boolean = false): String {
        val trimmed = value.trim().removeSuffix("%").trim()
        val normalized = if (integer || trimmed.count { it == ',' } != 1 || '.' in trimmed) {
            trimmed
        } else {
            trimmed.replace(',', '.')
        }
        return if (percent) "$normalized%" else normalized
    }

    @JvmStatic
    fun validate(value: String, minimum: Float, maximum: Float, integer: Boolean): Result {
        val normalized = normalizeForStorage(value, integer)
        if (normalized.isEmpty()) return Result.INVALID_NUMBER

        val number = if (integer) normalized.toIntOrNull()?.toFloat() else normalized.toFloatOrNull()
        if (number == null || !number.isFinite()) return Result.INVALID_NUMBER
        if (number < minimum || number > maximum) return Result.OUT_OF_RANGE
        return Result.VALID
    }
}
