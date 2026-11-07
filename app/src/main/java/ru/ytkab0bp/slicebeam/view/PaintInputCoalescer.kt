package ru.ytkab0bp.slicebeam.view

/** Keeps GL-thread paint work bounded while preserving the path covered by coalesced input. */
internal class PaintInputCoalescer {
    data class Point(val x: Float, val y: Float)
    data class Segment(val start: Point, val end: Point)

    private var pendingStart: Point? = null
    private var pendingEnd: Point? = null
    private var drainScheduled = false

    @Synchronized
    fun submit(x: Float, y: Float): Boolean {
        val point = Point(x, y)
        if (pendingEnd == null) pendingStart = point
        pendingEnd = point
        if (drainScheduled) return false
        drainScheduled = true
        return true
    }

    @Synchronized
    fun takePending(): Segment? {
        val end = pendingEnd ?: return null
        val start = pendingStart ?: end
        pendingStart = null
        pendingEnd = null
        return Segment(start, end)
    }

    /** Returns true when another drain must be queued. */
    @Synchronized
    fun finishDrain(): Boolean {
        if (pendingEnd != null) return true
        drainScheduled = false
        return false
    }

    /** Flushes the complete pending segment before a stroke ends and releases the scheduled flag. */
    @Synchronized
    fun flush(): Segment? {
        val end = pendingEnd
        val segment = end?.let { Segment(pendingStart ?: it, it) }
        pendingStart = null
        pendingEnd = null
        drainScheduled = false
        return segment
    }
}
