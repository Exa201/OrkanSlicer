package ru.ytkab0bp.slicebeam.utils

/** Locale-tolerant numeric input helpers used by the image-to-lithophane generator. */
object LithophaneInput {
    @JvmStatic
    fun parseClamped(text: String?, fallback: Float, min: Float, max: Float): Float {
        if (!java.lang.Float.isFinite(fallback) || !java.lang.Float.isFinite(min)
            || !java.lang.Float.isFinite(max) || min > max
        ) {
            throw IllegalArgumentException("Invalid fallback or bounds.")
        }
        if (text == null) return fallback
        return try {
            val normalized = text.trim { it <= ' ' }.replace(',', '.')
            val value = java.lang.Float.parseFloat(normalized)
            if (!java.lang.Float.isFinite(value)) fallback else value.coerceIn(min, max)
        } catch (_: RuntimeException) {
            fallback
        }
    }
}
