package ru.ytkab0bp.slicebeam.render

/** Defaults for the viewport only; slicing always uses the original model. */
object DevicePerformancePolicy {
    fun defaultRenderScale(lowRamDevice: Boolean): Float = if (lowRamDevice) 0.7f else 1f

    /** A zero preference follows the device policy; explicit values are viewport-only. */
    fun renderScale(lowRamDevice: Boolean, overridePercent: Int): Float = when (overridePercent) {
        50, 65, 80, 100 -> overridePercent / 100f
        else -> defaultRenderScale(lowRamDevice)
    }

    /** Convert View pixels to the actual GL surface coordinate space. */
    fun mapTouchCoordinate(inputPx: Float, viewSizePx: Int, surfaceSizePx: Int): Float =
        if (viewSizePx > 0 && surfaceSizePx > 0) inputPx * surfaceSizePx / viewSizePx else inputPx

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
