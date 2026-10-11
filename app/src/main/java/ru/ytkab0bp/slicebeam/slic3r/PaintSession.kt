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

    open fun clear() = Native.paint_clear(ptr)

    /** Write the painting back into the model so it slices to multi-color gcode. */
    open fun commit() = Native.paint_commit(ptr)

    /** Fill a GLModel with the facets of the given filament; returns triangle count. */
    open fun buildOverlay(gl: GLModel, filamentIdx: Int): Int = gl.initFromPaint(ptr, filamentIdx)

    open fun end() {
        if (ptr != 0L) {
            Native.paint_end(ptr)
            ptr = 0L
        }
    }
}
