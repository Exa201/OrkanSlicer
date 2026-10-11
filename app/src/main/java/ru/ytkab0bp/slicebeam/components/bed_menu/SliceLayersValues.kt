package ru.ytkab0bp.slicebeam.components.bed_menu

import kotlin.math.roundToInt

/** One-based, ordered layer selection and the highest selectable layer. */
internal data class SliceLayerRange(
    val from: Int,
    val to: Int,
    val maximum: Int,
)

/** Converts the viewer's zero-based native range to a safe, one-based UI range. */
internal fun normalizeSliceLayerRange(layerCount: Long, first: Long, second: Long): SliceLayerRange {
    val maximum = layerCount.coerceIn(1L, Int.MAX_VALUE.toLong()).toInt()
    val lastIndex = maximum.toLong() - 1L
    val lower = minOf(first, second).coerceIn(0L, lastIndex)
    val upper = maxOf(first, second).coerceIn(lower, lastIndex)
    return SliceLayerRange(
        from = lower.toInt() + 1,
        to = upper.toInt() + 1,
        maximum = maximum,
    )
}

/** Maps a Compose slider fraction to a one-based layer without overflowing at large layer counts. */
internal fun layerFromSliderFraction(fraction: Float, maximum: Int): Int {
    val safeMaximum = maximum.coerceAtLeast(1)
    if (safeMaximum == 1) return 1
    val offset = (fraction.coerceIn(0f, 1f).toDouble() * (safeMaximum - 1)).roundToInt()
    return (offset + 1).coerceIn(1, safeMaximum)
}

/** Maps a one-based layer number to the stable [0, 1] range used by Compose sliders. */
internal fun layerSliderFraction(layer: Int, maximum: Int): Float {
    val safeMaximum = maximum.coerceAtLeast(1)
    if (safeMaximum == 1) return 0f
    return ((layer.coerceIn(1, safeMaximum) - 1).toDouble() / (safeMaximum - 1)).toFloat()
}

internal fun withSliceLayerFrom(range: SliceLayerRange, proposed: Int): SliceLayerRange =
    range.copy(from = proposed.coerceIn(1, range.to))

internal fun withSliceLayerTo(range: SliceLayerRange, proposed: Int): SliceLayerRange =
    range.copy(to = proposed.coerceIn(range.from, range.maximum))
