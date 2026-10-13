package ru.ytkab0bp.slicebeam.render

/** Defaults for the viewport only; slicing always uses the original model. */
object DevicePerformancePolicy {
    fun defaultRenderScale(lowRamDevice: Boolean): Float = if (lowRamDevice) 0.7f else 1f

    fun useSimpleInactivePlatePreview(performanceMode: Boolean, lowRamDevice: Boolean): Boolean =
        performanceMode || lowRamDevice

    fun initialLayerBlockSize(lowRamDevice: Boolean): Int = if (lowRamDevice) 25 else 50

    /** Limit only the on-screen mesh. The Model retained for slicing is never changed. */
    fun activePreviewTriangleBudget(performanceMode: Boolean, lowRamDevice: Boolean): Int? = when {
        lowRamDevice -> 120_000
        performanceMode -> 250_000
        else -> null
    }
}
