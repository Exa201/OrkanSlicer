package ru.ytkab0bp.slicebeam.slic3r

/** Keeps stored profile enum values compatible with the native slicer bundled in this build. */
object NativeEnumValueSanitizer {
    @JvmStatic
    fun normalize(value: String?, acceptedValues: Array<String>?, defaultValue: String?): String? {
        if (value == null || acceptedValues.isNullOrEmpty()) return value
        if (acceptedValues.contains(value)) return value
        if (defaultValue != null && acceptedValues.contains(defaultValue)) return defaultValue
        return acceptedValues[0]
    }

    /** Migrates the former bool-backed arc-fitting option before enum sanitization. */
    @JvmStatic
    fun normalize(optionKey: String, value: String?, acceptedValues: Array<String>?, defaultValue: String?): String? {
        if (optionKey == "enable_arc_fitting" && value != null) {
            val legacyValue = when {
                value == "1" || value.equals("true", ignoreCase = true) -> "enabled"
                value == "0" || value.equals("false", ignoreCase = true) -> "disabled"
                else -> value
            }
            return normalize(legacyValue, acceptedValues, defaultValue)
        }
        return normalize(value, acceptedValues, defaultValue)
    }
}
