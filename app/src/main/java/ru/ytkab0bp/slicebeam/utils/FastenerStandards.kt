package ru.ytkab0bp.slicebeam.utils

import java.util.Locale

/** Metric coarse-thread and common hex-nut/washer starting dimensions in millimetres. */
object FastenerStandards {
    private val DIAMETERS = floatArrayOf(2f, 2.5f, 3f, 4f, 5f, 6f, 8f, 10f, 12f)
    private val PITCH = floatArrayOf(.4f, .45f, .5f, .7f, .8f, 1f, 1.25f, 1.5f, 1.75f)
    private val HEX_FLATS = floatArrayOf(4f, 5f, 5.5f, 7f, 8f, 10f, 13f, 16f, 18f)
    private val WASHER_OD = floatArrayOf(5f, 6f, 7f, 9f, 10f, 12f, 16f, 20f, 24f)
    private val NUT_HEIGHT = floatArrayOf(1.6f, 2f, 2.4f, 3.2f, 4f, 5f, 6.5f, 8f, 10f)
    private val WASHER_THICKNESS = floatArrayOf(.8f, .8f, .8f, .8f, 1f, 1.2f, 1.5f, 2f, 2.5f)

    /** Compact locale-independent decimal text safe for generated STL filenames. */
    @JvmStatic
    fun fileNumber(value: Float): String {
        if (!java.lang.Float.isFinite(value)) throw IllegalArgumentException("A finite dimension is required.")
        val text = String.format(Locale.US, "%.2f", value)
            .replace(Regex("0+$"), "")
            .replace(Regex("\\.$"), "")
        return text.replace('.', 'p')
    }

    @JvmStatic fun count(): Int = DIAMETERS.size
    @JvmStatic fun diameter(i: Int): Float = DIAMETERS[checked(i)]
    @JvmStatic fun coarsePitch(i: Int): Float = PITCH[checked(i)]
    @JvmStatic fun hexNutAcrossFlats(i: Int): Float = HEX_FLATS[checked(i)]
    @JvmStatic fun washerOuterDiameter(i: Int): Float = WASHER_OD[checked(i)]
    @JvmStatic fun nutHeight(i: Int): Float = NUT_HEIGHT[checked(i)]
    @JvmStatic fun washerThickness(i: Int): Float = WASHER_THICKNESS[checked(i)]

    /** Radial allowance for a printable threaded fit: tight, balanced, or loose. */
    @JvmStatic
    fun threadClearance(fit: Int): Float = when (fit) {
        0 -> .12f
        1 -> .20f
        2 -> .30f
        else -> throw IndexOutOfBoundsException("Unknown thread-fit preset: $fit")
    }

    /** Printable FDM start value; user can tune it for the printer/material. */
    @JvmStatic
    fun clearanceBore(i: Int): Float {
        val position = checked(i)
        return DIAMETERS[position] + if (position == 0) .2f else .4f
    }

    private fun checked(i: Int): Int {
        if (i < 0 || i >= DIAMETERS.size) throw IndexOutOfBoundsException("Unknown metric size index: $i")
        return i
    }
}
