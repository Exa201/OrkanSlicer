package ru.ytkab0bp.slicebeam.config

import ru.ytkab0bp.slicebeam.slic3r.ConfigOptionDef

/** Checks imported profile values against the native Orca option schema without rewriting them. */
object ProfileConfigImportValidator {
    @JvmStatic
    fun validate(
        profiles: Iterable<ConfigObject>,
        optionDefinitions: Map<String, ConfigOptionDef>,
    ): List<ProfileConfigLoadWarning.InvalidValue> {
        val warnings = LinkedHashSet<ProfileConfigLoadWarning.InvalidValue>()
        for (profile in profiles) {
            for ((rawKey, rawValue) in profile.values) {
                val key = ConfigObject.migrateKey(rawKey)
                val definition = optionDefinitions[key] ?: continue
                val value = rawValue.trim()
                val issue = validateValue(value, definition) ?: continue
                warnings += ProfileConfigLoadWarning.InvalidValue(
                    profile = profile.title.orEmpty(),
                    key = key,
                    value = rawValue,
                    issue = issue,
                )
            }
        }
        return warnings.toList()
    }

    private fun validateValue(
        value: String,
        definition: ConfigOptionDef,
    ): ProfileConfigLoadWarning.ValueIssue? {
        val minimum = definition.min.takeUnless { it == Float.MIN_VALUE } ?: Float.NEGATIVE_INFINITY
        val maximum = definition.max.takeUnless { it == Float.MAX_VALUE } ?: Float.POSITIVE_INFINITY

        fun numericIssue(result: ProfileNumericValueValidator.Result) = when (result) {
            ProfileNumericValueValidator.Result.VALID -> null
            ProfileNumericValueValidator.Result.INVALID_NUMBER -> ProfileConfigLoadWarning.ValueIssue.INVALID_NUMBER
            ProfileNumericValueValidator.Result.OUT_OF_RANGE -> ProfileConfigLoadWarning.ValueIssue.OUT_OF_RANGE
        }

        fun vectorIssue(result: ProfileVectorValueValidator.Result) = when (result) {
            ProfileVectorValueValidator.Result.VALID -> null
            ProfileVectorValueValidator.Result.EMPTY -> if (value.isBlank()) null
                else ProfileConfigLoadWarning.ValueIssue.INVALID_NUMBER
            ProfileVectorValueValidator.Result.INVALID_NUMBER -> ProfileConfigLoadWarning.ValueIssue.INVALID_NUMBER
            ProfileVectorValueValidator.Result.OUT_OF_RANGE -> ProfileConfigLoadWarning.ValueIssue.OUT_OF_RANGE
        }

        return when (definition.type) {
            ConfigOptionDef.ConfigOptionType.FLOAT,
            ConfigOptionDef.ConfigOptionType.PERCENT,
            ConfigOptionDef.ConfigOptionType.FLOAT_OR_PERCENT -> numericIssue(
                ProfileNumericValueValidator.validate(value, minimum, maximum, integer = false),
            )
            ConfigOptionDef.ConfigOptionType.INT -> numericIssue(
                ProfileNumericValueValidator.validate(value, minimum, maximum, integer = true),
            )
            ConfigOptionDef.ConfigOptionType.FLOATS -> vectorIssue(
                ProfileVectorValueValidator.validate(
                    value, minimum, maximum, ProfileVectorValueValidator.Kind.FLOATS,
                    allowNil = definition.nullable,
                ),
            )
            ConfigOptionDef.ConfigOptionType.INTS -> vectorIssue(
                ProfileVectorValueValidator.validate(
                    value, minimum, maximum, ProfileVectorValueValidator.Kind.INTS,
                    allowNil = definition.nullable,
                ),
            )
            ConfigOptionDef.ConfigOptionType.PERCENTS -> vectorIssue(
                ProfileVectorValueValidator.validate(
                    value, minimum, maximum, ProfileVectorValueValidator.Kind.PERCENTS,
                    allowNil = definition.nullable,
                ),
            )
            ConfigOptionDef.ConfigOptionType.FLOATS_OR_PERCENTS -> vectorIssue(
                ProfileVectorValueValidator.validate(
                    value, minimum, maximum, ProfileVectorValueValidator.Kind.FLOATS_OR_PERCENTS,
                    allowNil = definition.nullable,
                ),
            )
            ConfigOptionDef.ConfigOptionType.POINT -> pointIssue(value, ProfilePointValueValidator.Kind.POINT)
            ConfigOptionDef.ConfigOptionType.POINTS -> pointIssue(value, ProfilePointValueValidator.Kind.POINTS)
            ConfigOptionDef.ConfigOptionType.POINT3 -> pointIssue(value, ProfilePointValueValidator.Kind.POINT3)
            ConfigOptionDef.ConfigOptionType.BOOL -> {
                if (value == "0" || value == "1") null else ProfileConfigLoadWarning.ValueIssue.INVALID_BOOLEAN
            }
            ConfigOptionDef.ConfigOptionType.BOOLS -> {
                val parts = value.split(',').map(String::trim)
                if (value.isBlank() || parts.all { it == "0" || it == "1" || (definition.nullable && it == "nil") }) null
                else ProfileConfigLoadWarning.ValueIssue.INVALID_BOOLEAN
            }
            ConfigOptionDef.ConfigOptionType.ENUM -> validateEnum(value, definition)
            ConfigOptionDef.ConfigOptionType.ENUMS -> validateEnumVector(value, definition)
            else -> null
        }
    }

    private fun pointIssue(
        value: String,
        kind: ProfilePointValueValidator.Kind,
    ): ProfileConfigLoadWarning.ValueIssue? = when (ProfilePointValueValidator.validate(value, kind)) {
        ProfilePointValueValidator.Result.VALID,
        ProfilePointValueValidator.Result.EMPTY -> null
        ProfilePointValueValidator.Result.INVALID_COORDINATE -> ProfileConfigLoadWarning.ValueIssue.INVALID_POINT
    }

    private fun validateEnum(
        value: String,
        definition: ConfigOptionDef,
    ): ProfileConfigLoadWarning.ValueIssue? {
        if (isOpenSelection(definition)) return null
        val values = definition.enumValues?.takeIf { it.isNotEmpty() } ?: return null
        return if (ProfileEnumOptionResolver.selectedIndex(value, values) >= 0) null
        else ProfileConfigLoadWarning.ValueIssue.UNKNOWN_ENUM
    }

    private fun validateEnumVector(
        value: String,
        definition: ConfigOptionDef,
    ): ProfileConfigLoadWarning.ValueIssue? {
        if (isOpenSelection(definition)) return null
        val values = definition.enumValues?.takeIf { it.isNotEmpty() } ?: return null
        val selected = ProfileEnumOptionResolver.parseValues(value)
        return if (selected.all { ProfileEnumOptionResolver.selectedIndex(it, values) >= 0 }) null
        else ProfileConfigLoadWarning.ValueIssue.UNKNOWN_ENUM
    }

    private fun isOpenSelection(definition: ConfigOptionDef): Boolean = when (definition.guiType) {
        ConfigOptionDef.GUIType.I_ENUM_OPEN,
        ConfigOptionDef.GUIType.F_ENUM_OPEN,
        ConfigOptionDef.GUIType.SELECT_OPEN -> true
        else -> false
    }
}
