package ru.ytkab0bp.slicebeam.config

import ru.ytkab0bp.slicebeam.slic3r.ConfigOptionDef

/** Applies Orca's declared numeric bounds to the uncommon profile values handled by the fallback editor. */
object ProfileFallbackValueValidator {
    enum class Result { VALID, INVALID_NUMBER, OUT_OF_RANGE, INVALID_VECTOR }

    @JvmStatic
    fun validate(definition: ConfigOptionDef, input: String): Result = when (definition.type) {
        ConfigOptionDef.ConfigOptionType.FLOAT -> numeric(definition, input, integer = false)
        ConfigOptionDef.ConfigOptionType.INT -> numeric(definition, input, integer = true)
        ConfigOptionDef.ConfigOptionType.PERCENT -> {
            val percentValue = input.trim()
            if (!percentValue.endsWith('%')) Result.INVALID_NUMBER
            else numeric(definition, percentValue.removeSuffix("%"), integer = false)
        }
        ConfigOptionDef.ConfigOptionType.FLOAT_OR_PERCENT -> numeric(definition, input, integer = false)
        ConfigOptionDef.ConfigOptionType.FLOATS -> vector(definition, input, ProfileVectorValueValidator.Kind.FLOATS)
        ConfigOptionDef.ConfigOptionType.INTS -> vector(definition, input, ProfileVectorValueValidator.Kind.INTS)
        ConfigOptionDef.ConfigOptionType.PERCENTS -> vector(definition, input, ProfileVectorValueValidator.Kind.PERCENTS)
        ConfigOptionDef.ConfigOptionType.FLOATS_OR_PERCENTS ->
            vector(definition, input, ProfileVectorValueValidator.Kind.FLOATS_OR_PERCENTS)
        else -> Result.VALID
    }

    private fun numeric(definition: ConfigOptionDef, input: String, integer: Boolean): Result =
        when (ProfileNumericValueValidator.validate(input, definition.min, definition.max, integer)) {
            ProfileNumericValueValidator.Result.VALID -> Result.VALID
            ProfileNumericValueValidator.Result.INVALID_NUMBER -> Result.INVALID_NUMBER
            ProfileNumericValueValidator.Result.OUT_OF_RANGE -> Result.OUT_OF_RANGE
        }

    private fun vector(
        definition: ConfigOptionDef,
        input: String,
        kind: ProfileVectorValueValidator.Kind,
    ): Result = when (
        ProfileVectorValueValidator.validate(input, definition.min, definition.max, kind, definition.nullable)
    ) {
        ProfileVectorValueValidator.Result.VALID -> Result.VALID
        ProfileVectorValueValidator.Result.EMPTY -> Result.INVALID_VECTOR
        ProfileVectorValueValidator.Result.INVALID_NUMBER -> Result.INVALID_NUMBER
        ProfileVectorValueValidator.Result.OUT_OF_RANGE -> Result.OUT_OF_RANGE
    }
}
