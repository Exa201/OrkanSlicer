package ru.ytkab0bp.slicebeam.render

/** Defaults for the viewport only; slicing always uses the original model. */
object DevicePerformancePolicy {
    /**
     * Keep the viewport responsive on devices whose GL implementation is known to be
     * software-rendered or from an older low-end GPU family. Unknown renderers retain
     * full quality; a renderer name is a conservative capability hint, not a benchmark.
     */
    fun isKnownLowEndRenderer(renderer: String?): Boolean {
        val name = renderer?.trim()?.lowercase().orEmpty()
        if (name.isEmpty()) return false
        if (listOf("swiftshader", "llvmpipe", "softpipe", "software rasterizer").any(name::contains)) return true
        if (listOf("mali-400", "mali-450", "mali-t", "powervr sgx", "vivante").any(name::contains)) return true

        val adreno = Regex("adreno(?: \\(tm\\))?\\s*(\\d{3,4})").find(name)?.groupValues?.get(1)?.toIntOrNull()
        if (adreno != null && adreno < 600) return true

        val maliG = Regex("mali-g(\\d+)").find(name)?.groupValues?.get(1)?.toIntOrNull()
        return maliG != null && maliG <= 57
    }

    fun defaultRenderScale(lowRamDevice: Boolean, renderer: String? = null): Float = when {
        lowRamDevice -> 0.7f
        isKnownLowEndRenderer(renderer) -> 0.65f
        else -> 1f
    }

    fun isLowPerformanceDevice(lowRamDevice: Boolean, renderer: String? = null): Boolean =
        lowRamDevice || isKnownLowEndRenderer(renderer)

    /** A zero preference follows the device policy; explicit values are viewport-only. */
    fun renderScale(lowRamDevice: Boolean, overridePercent: Int, renderer: String? = null): Float = when (overridePercent) {
        50, 65, 80, 100 -> overridePercent / 100f
        else -> defaultRenderScale(lowRamDevice, renderer)
    }

    /** Convert View pixels to the actual GL surface coordinate space. */
    fun mapTouchCoordinate(inputPx: Float, viewSizePx: Int, surfaceSizePx: Int): Float =
        if (viewSizePx > 0 && surfaceSizePx > 0) inputPx * surfaceSizePx / viewSizePx else inputPx

    fun useSimpleInactivePlatePreview(
        performanceMode: Boolean,
        lowRamDevice: Boolean,
        lowGpuDevice: Boolean = false,
    ): Boolean = performanceMode || lowRamDevice || lowGpuDevice

    fun initialLayerBlockSize(lowRamDevice: Boolean, lowGpuDevice: Boolean = false): Int = when {
        lowRamDevice -> 25
        lowGpuDevice -> 35
        else -> 50
    }

    /** Limit only the on-screen mesh. The Model retained for slicing is never changed. */
    fun activePreviewTriangleBudget(
        performanceMode: Boolean,
        lowRamDevice: Boolean,
        lowGpuDevice: Boolean = false,
    ): Int? = when {
        lowRamDevice -> 120_000
        lowGpuDevice -> 180_000
        performanceMode -> 250_000
        else -> null
    }
}
