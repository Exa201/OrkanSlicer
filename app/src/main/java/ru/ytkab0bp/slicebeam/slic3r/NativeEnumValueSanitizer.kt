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
}
