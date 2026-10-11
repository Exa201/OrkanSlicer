package ru.ytkab0bp.slicebeam.utils

/** Shared image-to-relief mapping used by lithophane previews and STL generation. */
object LithophaneToneMapper {
    @JvmStatic
    fun gray(
        red: Float,
        green: Float,
        blue: Float,
        alpha: Float,
        brightness: Float,
        contrast: Float,
        toneMode: Int,
        invert: Boolean
    ): Float {
        var value = when (toneMode) {
            2 -> (red + green + blue) / 3f
            3 -> red
            4 -> green
            5 -> blue
            else -> 0.2126f * red + 0.7152f * green + 0.0722f * blue
        }
        value = value * alpha + (1f - alpha) // Composite transparency against white.
        value = clamp((value - 0.5f) * contrast + 0.5f + brightness, 0f, 1f)
        if (toneMode == 1) value = if (value >= 0.5f) 1f else 0f
        return if (invert) 1f - value else value
    }

    @JvmStatic
    fun thickness(gray: Float, base: Float, maximum: Float, mode: Int): Float {
        if (mode == 1) return if (gray < 0.5f) java.lang.Math.max(0.4f, base + maximum) else java.lang.Math.max(0.4f, base)
        if (mode == 2 || mode == 3) {
            val range = java.lang.Math.max(0f, maximum - base)
            return java.lang.Math.max(0.4f, base + (1f - gray) * range)
        }
        return java.lang.Math.max(0.4f, base + (1f - gray) * maximum)
    }

    private fun clamp(value: Float, min: Float, max: Float): Float =
        java.lang.Math.max(min, java.lang.Math.min(max, value))
}
