package ru.ytkab0bp.slicebeam.view

import kotlin.math.abs

internal data class SnackbarSwipeRelease(val targetX: Float, val dismiss: Boolean)

/** Keeps fling-to-dismiss behavior deterministic and testable outside Android touch dispatch. */
internal object SnackbarSwipePolicy {
    const val DISMISS_FLING_THRESHOLD_PX_PER_SECOND = 1500f

    fun release(
        lifetimeMs: Int,
        velocityX: Float,
        viewWidth: Int,
        marginPx: Int,
    ): SnackbarSwipeRelease {
        val dismiss = lifetimeMs > 0 &&
            velocityX.isFinite() &&
            abs(velocityX) >= DISMISS_FLING_THRESHOLD_PX_PER_SECOND &&
            viewWidth > 0
        if (!dismiss) return SnackbarSwipeRelease(targetX = 0f, dismiss = false)

        val distance = (viewWidth + marginPx.coerceAtLeast(0)).toFloat()
        return SnackbarSwipeRelease(
            targetX = if (velocityX > 0f) distance else -distance,
            dismiss = true,
        )
    }
}
