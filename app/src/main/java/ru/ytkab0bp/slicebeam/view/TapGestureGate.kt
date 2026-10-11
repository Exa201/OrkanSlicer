package ru.ytkab0bp.slicebeam.view

/** Allows a click only for one complete, unmoved, single-pointer down/up sequence. */
internal class TapGestureGate {
    private var armed = false
    private var singlePointerSequence = false
    private var downX = 0f
    private var downY = 0f
    private var downTimeMs: Long? = null
    private var lastAcceptedTapTimeMs = Long.MIN_VALUE
    private var lastAcceptedTapX = 0f
    private var lastAcceptedTapY = 0f

    fun begin(pointerCount: Int, x: Float = 0f, y: Float = 0f, downTimeMs: Long? = null) {
        armed = pointerCount == 1
        singlePointerSequence = pointerCount == 1
        downX = x
        downY = y
        this.downTimeMs = downTimeMs
    }

    fun invalidate() {
        armed = false
    }

    /** Uses the full displacement from ACTION_DOWN, not just the latest event delta. */
    fun movedBeyondTouchSlop(x: Float, y: Float, touchSlop: Int): Boolean {
        if (!singlePointerSequence) return false
        val dx = x - downX
        val dy = y - downY
        val slop = touchSlop.toFloat()
        return dx * dx + dy * dy >= slop * slop
    }

    fun finish(
        pointerCount: Int,
        moved: Boolean,
        cancelled: Boolean,
        eventTimeMs: Long? = null,
        x: Float = downX,
        y: Float = downY,
        duplicateWindowMs: Long = 300L,
        duplicateRadiusPx: Float = 0f,
        maxTapDurationMs: Long = Long.MAX_VALUE,
    ): Boolean {
        val pressDuration = downTimeMs?.let { down -> eventTimeMs?.minus(down) }
        val heldTooLong = pressDuration != null && pressDuration >= maxTapDurationMs
        val isTap = armed && pointerCount == 1 && !moved && !cancelled && !heldTooLong
        armed = false
        singlePointerSequence = false
        downTimeMs = null
        if (!isTap) return false

        if (eventTimeMs != null && lastAcceptedTapTimeMs != Long.MIN_VALUE) {
            val elapsedMs = eventTimeMs - lastAcceptedTapTimeMs
            val dx = x - lastAcceptedTapX
            val dy = y - lastAcceptedTapY
            val radiusSquared = duplicateRadiusPx * duplicateRadiusPx
            val sameTouchArea = dx * dx + dy * dy <= radiusSquared
            if (elapsedMs in 0..duplicateWindowMs && sameTouchArea) return false
        }

        if (eventTimeMs != null) {
            lastAcceptedTapTimeMs = eventTimeMs
            lastAcceptedTapX = x
            lastAcceptedTapY = y
        }
        return true
    }
}
