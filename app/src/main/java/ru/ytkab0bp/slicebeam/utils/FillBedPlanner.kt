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
    }
}
