package ru.ytkab0bp.slicebeam.view

/** Keeps GL-thread paint work bounded while preserving the latest pointer position. */
internal class PaintInputCoalescer {
    data class Point(val x: Float, val y: Float)

    private var pending: Point? = null
    private var drainScheduled = false

    @Synchronized
    fun submit(x: Float, y: Float): Boolean {
        pending = Point(x, y)
        if (drainScheduled) return false
        drainScheduled = true
        return true
    }

    @Synchronized
    fun takePending(): Point? {
        val point = pending
        pending = null
        return point
    }

    /** Returns true when another drain must be queued. */
    @Synchronized
    fun finishDrain(): Boolean {
        if (pending != null) return true
        drainScheduled = false
        return false
    }

    /** Flushes the last point before a stroke ends and releases the scheduled flag. */
    @Synchronized
    fun flush(): Point? {
        val point = pending
        pending = null
        drainScheduled = false
        return point
    }
}
