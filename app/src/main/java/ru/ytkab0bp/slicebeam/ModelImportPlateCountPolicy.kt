package ru.ytkab0bp.slicebeam

/** Keeps single-model imports on one plate when a 3MF has missing or invalid plate metadata. */
internal object ModelImportPlateCountPolicy {
    @JvmStatic
    fun resolve(isThreeMfProject: Boolean, detectedPlateCount: Int): Int =
        if (!isThreeMfProject) 1 else detectedPlateCount.coerceAtLeast(1)
}
