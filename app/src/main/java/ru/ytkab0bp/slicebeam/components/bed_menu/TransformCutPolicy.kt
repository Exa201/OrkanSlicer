package ru.ytkab0bp.slicebeam.components.bed_menu

import kotlin.math.round
import kotlin.math.roundToInt

/** Keeps cut controls inside finite native bounds and preserves the renderer's degree units. */
internal object TransformCutPolicy {
    internal data class ZRange(val min: Float, val max: Float, val initial: Float)

    private val fallbackRange = ZRange(0f, 1f, 0.5f)

    fun zRange(first: Double, second: Double): ZRange {
        if (!first.isFinite() || !second.isFinite()) return fallbackRange

        val lower = minOf(first, second).toFloat()
        val upper = maxOf(first, second).toFloat()
        if (!lower.isFinite() || !upper.isFinite()) return fallbackRange

        val expandedUpper = maxOf(upper, lower + MINIMUM_Z_RANGE_MM)
            .takeIf { it.isFinite() && it > lower }
            ?: Math.nextUp(lower).takeIf { it.isFinite() && it > lower }
            ?: return fallbackRange
        val midpoint = ((first + second) / 2.0).toFloat()
            .takeIf(Float::isFinite)
            ?: lower

        return ZRange(lower, expandedUpper, midpoint.coerceIn(lower, expandedUpper))
    }

    fun roundHundredths(value: Float): Float =
        if (value.isFinite()) round(value * 100f) / 100f else 0f

    fun degreesToRadians(degrees: Float): Float {
        val bounded = if (degrees.isFinite()) degrees.coerceIn(-180f, 180f) else 0f
        return Math.toRadians(bounded.toDouble()).toFloat()
    }

    fun connectorType(value: Float): Int =
        if (value.isFinite()) value.roundToInt().coerceIn(0, 2) else 0

    private const val MINIMUM_Z_RANGE_MM = 0.01f
}
