package ru.ytkab0bp.slicebeam.smartslice

class SmartSliceIntent(
    @JvmField val purpose: Purpose,
    @JvmField val priority: Priority,
    @JvmField val requestedWallMm: Double?,
    @JvmField val requestedLayerMm: Double?
) {
    enum class Purpose {
        DECORATION, FUNCTIONAL, MECHANICAL, PRECISION, MINIATURE, VASE, PROTOTYPE
    }

    enum class Priority {
        QUALITY, BALANCED, FAST, STRENGTH, DIMENSIONAL
    }
}
