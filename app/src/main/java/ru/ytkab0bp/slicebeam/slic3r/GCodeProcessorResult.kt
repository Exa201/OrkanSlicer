package ru.ytkab0bp.slicebeam.slic3r

import java.io.File

/** Owns the native analysis result for one G-code file. */
open class GCodeProcessorResult {
    /** Kept with this result across GL context recreation and screen rotation. */
    @Volatile var previewSelection: GCodePreviewSelection? = null
    @Volatile var previewLayerCount: Int = 0

    /** Exact tool palette passed to native slicing, in 0-based RGB values. */
    @Volatile var previewToolColors: IntArray? = null

    @JvmField
    internal var pointer: Long

    constructor(file: File) {
        pointer = Native.gcoderesult_load_file(file.absolutePath, file.name)
    }

    internal constructor(ptr: Long) {
        pointer = ptr
    }

    open fun getUsedFilamentMM(@GCodeViewer.ExtrusionRole role: Int): Double =
        Native.gcoderesult_get_used_filament_mm(pointer, role)

    open fun getUsedFilamentG(@GCodeViewer.ExtrusionRole role: Int): Double =
        Native.gcoderesult_get_used_filament_g(pointer, role)

    open fun getRecommendedName(): String = Native.gcoderesult_get_recommended_name(pointer)

    open fun release() {
        if (pointer != 0L) {
            Native.gcoderesult_release(pointer)
            pointer = 0L
        }
    }

    @Suppress("DEPRECATION")
    @Throws(Throwable::class)
    protected open fun finalize() {
        release()
    }
}
