package ru.ytkab0bp.slicebeam.utils

/** Keeps source photos close to the AI upload's 1024 px limit before allocating bitmap memory. */
internal object ImageDecodePolicy {
    const val MAX_DIMENSION = 1024

    fun sampleSize(width: Int, height: Int, maxDimension: Int = MAX_DIMENSION): Int {
        if (width <= 0 || height <= 0 || maxDimension <= 0) return 1
        val sourceDimension = maxOf(width, height).toLong()
        var sample = 1
        while (sourceDimension / sample > maxDimension && sample <= Int.MAX_VALUE / 2) {
            sample *= 2
        }
        return sample
    }
}
