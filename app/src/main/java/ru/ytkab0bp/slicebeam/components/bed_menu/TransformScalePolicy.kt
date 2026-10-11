package ru.ytkab0bp.slicebeam.components.bed_menu

import kotlin.math.ceil
import kotlin.math.floor

internal enum class TransformScaleAxis { X, Y, Z }

internal data class TransformScaleRulerValues(val x: Int, val y: Int, val z: Int)

internal data class TransformScaleLinkOffsets(val yFromX: Long, val zFromX: Long)

internal data class TransformScaleRulerDrag(val value: Int, val progress: Float)

/** Pure scale-editor math shared by the Compose ruler and its regression tests. */
internal object TransformScalePolicy {
    const val MIN_PERCENT = 1
    const val PIXELS_PER_PERCENT_DP = 10f

    fun linkOffsets(values: TransformScaleRulerValues) = TransformScaleLinkOffsets(
        yFromX = values.y.toLong() - values.x,
        zFromX = values.z.toLong() - values.x,
    )

    /** Keeps the other axis offsets stable while a linked ruler is dragged. */
    fun moveRuler(
        current: TransformScaleRulerValues,
        axis: TransformScaleAxis,
        newValue: Int,
        linked: Boolean,
        offsets: TransformScaleLinkOffsets,
    ): TransformScaleRulerValues {
        val target = newValue.coerceAtLeast(MIN_PERCENT)
        if (!linked) return when (axis) {
            TransformScaleAxis.X -> current.copy(x = target)
            TransformScaleAxis.Y -> current.copy(y = target)
            TransformScaleAxis.Z -> current.copy(z = target)
        }

        fun bounded(value: Long): Int = value.coerceIn(MIN_PERCENT.toLong(), Int.MAX_VALUE.toLong()).toInt()
        val x: Long
        val y: Long
        val z: Long
        when (axis) {
            TransformScaleAxis.X -> {
                x = target.toLong()
                y = x + offsets.yFromX
                z = x + offsets.zFromX
            }
            TransformScaleAxis.Y -> {
                y = target.toLong()
                x = y - offsets.yFromX
                z = x + offsets.zFromX
            }
            TransformScaleAxis.Z -> {
                z = target.toLong()
                x = z - offsets.zFromX
                y = x + offsets.yFromX
            }
        }
        return TransformScaleRulerValues(bounded(x), bounded(y), bounded(z))
    }

    /** A 10 dp finger movement advances exactly one percentage tick. */
    fun drag(
        currentValue: Int,
        progress: Float,
        dragAmountPx: Float,
        density: Float,
        min: Int = MIN_PERCENT,
        max: Int = Int.MAX_VALUE,
    ): TransformScaleRulerDrag {
        if (min > max) return TransformScaleRulerDrag(currentValue, 0f)
        if (!progress.isFinite() || !dragAmountPx.isFinite() || !density.isFinite() || density <= 0f) {
            return TransformScaleRulerDrag(currentValue.coerceIn(min, max), 0f)
        }
        var value = currentValue.coerceIn(min, max)
        val nextProgress = progress.toDouble() + dragAmountPx / (PIXELS_PER_PERCENT_DP * density)
        val wholeSteps = if (nextProgress >= 0.0) floor(nextProgress) else ceil(nextProgress)
        val boundedSteps = wholeSteps.coerceIn(Int.MIN_VALUE.toDouble(), Int.MAX_VALUE.toDouble()).toLong()
        value = (value.toLong() - boundedSteps).coerceIn(min.toLong(), max.toLong()).toInt()
        var remainingProgress = (nextProgress - boundedSteps).toFloat()
        if ((value == min && remainingProgress > 0f) || (value == max && remainingProgress < 0f)) remainingProgress = 0f
        return TransformScaleRulerDrag(value, remainingProgress.coerceIn(-1f, 1f))
    }

    /** Snaps the final partial ruler movement to the nearest percentage tick. */
    fun settle(value: Int, progress: Float, min: Int = MIN_PERCENT, max: Int = Int.MAX_VALUE): Int {
        if (min > max) return value
        return when {
        !progress.isFinite() -> value.coerceIn(min, max)
        progress >= 0.75f -> (value - 1).coerceAtLeast(min)
        progress <= -0.75f -> (value + 1).coerceAtMost(max)
        else -> value.coerceIn(min, max)
        }
    }

    /** Parses percent or millimeter input without accepting zero, infinity, or invalid dimensions. */
    fun scaleFactor(input: String, inMillimeters: Boolean, axisSizeMm: Double): Double? {
        val entered = input.trim().replace(',', '.').toDoubleOrNull()
            ?.takeIf { it.isFinite() && it > 0.0 }
            ?: return null
        val factor = if (inMillimeters) {
            if (!axisSizeMm.isFinite() || axisSizeMm <= 0.0) return null
            entered / axisSizeMm
        } else {
            entered / 100.0
        }
        return factor.takeIf { it.isFinite() && it > 0.0 }
    }

    fun roundedPercent(factor: Double): Int = when {
        !factor.isFinite() || factor <= 0.0 -> MIN_PERCENT
        factor * 100.0 >= Int.MAX_VALUE -> Int.MAX_VALUE
        else -> floor(factor * 100.0 + 0.5).toInt().coerceAtLeast(MIN_PERCENT)
    }
}
