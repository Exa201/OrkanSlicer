package ru.ytkab0bp.slicebeam.utils

/** Stable largest-footprint-first order used only when an imported batch must overflow a bed. */
object ModelImportPackingOrder {
    @JvmStatic
    fun largestFirst(footprintAreas: List<Double>): List<Int> =
        footprintAreas.indices.sortedWith(
            compareByDescending<Int> { index ->
                footprintAreas[index].takeIf { it.isFinite() && it > 0.0 } ?: 0.0
            }.thenBy { it },
        )
}
