package ru.ytkab0bp.slicebeam.utils

class FillBedPlanner private constructor() {
    companion object {
        @JvmStatic
        fun copyAttemptsForLimit(currentObjectCount: Int, maxObjectCount: Int): Int {
            if (currentObjectCount <= 0 || maxObjectCount <= currentObjectCount) {
                return 0
            }
            return maxObjectCount - currentObjectCount
        }

        /** Returns a usable grid capacity, or zero when the selected object cannot fit. */
        @JvmStatic
        fun estimatedCapacity(bedWidth: Double, bedDepth: Double, objectWidth: Double, objectDepth: Double, spacing: Double): Int {
            if (!bedWidth.isFinite() || !bedDepth.isFinite() || !objectWidth.isFinite() || !objectDepth.isFinite()) return 0
            if (bedWidth <= 0.0 || bedDepth <= 0.0 || objectWidth <= 0.0 || objectDepth <= 0.0 ||
                !spacing.isFinite() || spacing < 0.0) return 0
            if (objectWidth > bedWidth || objectDepth > bedDepth) return 0
            val columns = ((bedWidth + spacing) / (objectWidth + spacing)).toInt()
            val rows = ((bedDepth + spacing) / (objectDepth + spacing)).toInt()
            if (columns <= 0 || rows <= 0) return 0
            return (columns.toLong() * rows.toLong()).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        }
    }
}
