package ru.ytkab0bp.slicebeam.config

/** Validates Orca's numeric vectors and converts their editor text to Orca's comma-separated format. */
object ProfileVectorValueValidator {
    enum class Kind { FLOATS, INTS, PERCENTS, FLOATS_OR_PERCENTS }
    enum class Result { VALID, EMPTY, INVALID_NUMBER, OUT_OF_RANGE }

    private data class InputValues(val parts: List<String>, val decimalCommaAllowed: Boolean)

    private fun inputValues(value: String): InputValues {
        if (value.isBlank()) return InputValues(emptyList(), false)
        val allowDecimalComma = ';' in value || '\n' in value || '\r' in value
        val parts = if (allowDecimalComma) {
            value.split(Regex("[;\\r\\n]"))
        } else {
            value.split(',')
        }.map(String::trim)
        return InputValues(parts, allowDecimalComma)
    }

    private fun normalizedNumber(raw: String, decimalCommaAllowed: Boolean): String {
        if (!decimalCommaAllowed || raw.count { it == ',' } != 1 || '.' in raw) return raw
        return raw.replace(',', '.')
    }

    fun validate(value: String, minimum: Float, maximum: Float, kind: Kind, allowNil: Boolean = false): Result {
        val input = inputValues(value)
        if (input.parts.isEmpty() || input.parts.any(String::isBlank)) return Result.EMPTY
        var outOfRange = false
        for (part in input.parts) {
            if (part == "nil") {
                if (allowNil) continue
                return Result.INVALID_NUMBER
            }
            val hasPercent = part.endsWith("%")
            if (hasPercent && kind != Kind.PERCENTS && kind != Kind.FLOATS_OR_PERCENTS) {
                return Result.INVALID_NUMBER
            }
            val raw = if (hasPercent) part.dropLast(1).trim() else part
            val numberText = normalizedNumber(raw, input.decimalCommaAllowed)
            val number = if (kind == Kind.INTS) {
                numberText.toIntOrNull()?.toFloat()
            } else {
                numberText.toFloatOrNull()
            } ?: return Result.INVALID_NUMBER
            if (!number.isFinite()) return Result.INVALID_NUMBER
            if (number < minimum || number > maximum) outOfRange = true
        }
        return if (outOfRange) Result.OUT_OF_RANGE else Result.VALID
    }

    fun normalizeForStorage(value: String, kind: Kind, allowNil: Boolean = false): String {
        val input = inputValues(value)
        return input.parts.joinToString(",") { part ->
            if (allowNil && part.equals("nil", ignoreCase = true)) return@joinToString "nil"
            val hadPercent = part.endsWith("%")
            val raw = if (hadPercent) part.dropLast(1).trim() else part
            val number = normalizedNumber(raw, input.decimalCommaAllowed)
            when {
                kind == Kind.PERCENTS -> "$number%"
                kind == Kind.FLOATS_OR_PERCENTS && hadPercent -> "$number%"
                else -> number
            }
        }
    }

    fun preview(value: String, nilLabel: String = "nil"): String {
        val parts = inputValues(value).parts
        if (parts.isEmpty()) return "—"
        val visible = parts.take(2).joinToString(" · ") { part ->
            if (part.equals("nil", ignoreCase = true)) nilLabel else part
        }
        return if (parts.size > 2) "$visible …" else visible
    }

    /** Resets every currently represented filament entry to Orca's inherited/default marker. */
    fun inheritAll(value: String): String = List(inputValues(value).parts.size.coerceAtLeast(1)) { "nil" }
        .joinToString(",")
}
