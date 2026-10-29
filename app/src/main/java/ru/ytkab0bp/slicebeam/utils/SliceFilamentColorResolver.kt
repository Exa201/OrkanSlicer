package ru.ytkab0bp.slicebeam.utils

import ru.ytkab0bp.slicebeam.config.ProfileStringVectorCodec

/** Resolves the colors passed to native slicing from the same effective config the engine reads. */
object SliceFilamentColorResolver {
    @JvmStatic
    fun shouldUseToolPreview(modelIsMultiColor: Boolean, sliceColors: IntArray?): Boolean =
        modelIsMultiColor || (sliceColors?.size ?: 0) > 1
    /** Palette that must be used by the G-code viewer for the generated slice. */
    @JvmStatic
    fun previewColors(sliceColors: IntArray?, fallbackColors: IntArray): IntArray =
        (sliceColors?.takeIf { it.isNotEmpty() } ?: fallbackColors).map { it and 0xFFFFFF }.toIntArray()

    @JvmStatic
    fun resolve(configColors: String?, toolCount: Int, fallbackColors: IntArray): IntArray {
        val parsed = configColors?.let(ProfileStringVectorCodec::parse).orEmpty()
            .mapNotNull(::parseRgb)
        val fallback = fallbackColors.takeIf { it.isNotEmpty() } ?: intArrayOf(0x19C8A0)
        val source = parsed.ifEmpty { fallback.toList() }
        return IntArray(toolCount.coerceAtLeast(0)) { index ->
            // A short active config vector must not paint every extra model tool with
            // the last configured color. Use the app's matching slot color when present.
            (parsed.getOrNull(index) ?: fallback.getOrNull(index)
                ?: source.lastOrNull() ?: 0x19C8A0) and 0xFFFFFF
        }
    }

    private fun parseRgb(raw: String): Int? {
        val value = raw.trim().removePrefix("#")
        val rgb = if (value.length == 8) value.takeLast(6) else value
        if (rgb.length != 6 || !rgb.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) return null
        return rgb.toLongOrNull(16)?.toInt()
    }
}
