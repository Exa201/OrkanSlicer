package ru.ytkab0bp.slicebeam.slic3r

/**
 * A multi-color painting session bound to one object's first volume. All calls must happen on the
 * GL thread (they touch the native model/selector). See Native.paint_* for the engine bridge.
 */
open class PaintSession(model: Model, objIdx: Int, mode: Int) {
    private var ptr: Long = Native.paint_begin(model.pointer, objIdx, mode)

    open fun isValid(): Boolean = ptr != 0L

    /** Ray-cast a camera ray (mesh-local coords) -> {facetIdx, x, y, z} or empty if no hit. */
    open fun raycast(origin: DoubleArray?, dir: DoubleArray?): DoubleArray? {
        if (origin == null || dir == null) return null
        return Native.paint_raycast(ptr, origin, dir)
    }

    open fun brush(
        hit: DoubleArray,
        facetStart: Int,
        radius: Double,
        filamentIdx: Int,
        cameraPos: DoubleArray,
        sphere: Boolean,
    ) = Native.paint_brush(ptr, hit, facetStart, radius, filamentIdx, cameraPos, sphere)

    open fun bucket(hit: DoubleArray, facetStart: Int, filamentIdx: Int, propagate: Boolean, angle: Double) =
        Native.paint_bucket(ptr, hit, facetStart, filamentIdx, propagate, angle)

    open fun height(zMin: Double, zMax: Double, filamentIdx: Int) = Native.paint_height(ptr, zMin, zMax, filamentIdx)

    /** Stroke boundaries are owned by the GL touch stream; snapshots stay bounded in native memory. */
    open fun beginStroke() = Native.paint_begin_stroke(ptr)

    open fun endStroke() = Native.paint_end_stroke(ptr)

    open fun canUndoStroke(): Boolean = Native.paint_can_undo(ptr)

    open fun canRedoStroke(): Boolean = Native.paint_can_redo(ptr)

    open fun undoStroke(): Boolean = Native.paint_undo_stroke(ptr)

    open fun redoStroke(): Boolean = Native.paint_redo_stroke(ptr)

    open fun resetHistory() = Native.paint_reset_history(ptr)

    open fun clear() = Native.paint_clear(ptr)

    /** Write the painting back into the model so it slices to multi-color gcode. */
    open fun commit() = Native.paint_commit(ptr)

    /** Fill a GLModel with the facets of the given filament; returns triangle count. */
    open fun buildOverlay(gl: GLModel, filamentIdx: Int, baseFilamentPreview: Int = 0): Int =
        gl.initFromPaint(ptr, filamentIdx, baseFilamentPreview)

    /**
     * Build every live color overlay in one native pass. Multi-volume 3MF assemblies
     * otherwise deserialize each untouched volume's facet assignments once per filament.
     */
    open fun buildOverlays(glModels: Array<GLModel>, baseFilamentPreview: Int = 0): IntArray =
        Native.paint_build_live_overlays(ptr, LongArray(glModels.size) { glModels[it].pointer }, baseFilamentPreview)

    open fun end() {
        if (ptr != 0L) {
            Native.paint_end(ptr)
            ptr = 0L
        }
    }
}
