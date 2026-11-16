package ru.ytkab0bp.slicebeam

/** Maps Orca's 0..1 import callback to the UI's 0..90% read steps and preparation state. */
internal object StlImportProgressPolicy {
    fun stepForFraction(fraction: Float): Int = if (fraction >= 0.5f) {
        10
    } else {
        (fraction.coerceAtLeast(0f) * 20f).toInt().coerceIn(0, 9)
    }
}
