package ru.ytkab0bp.slicebeam.components.bed_menu

/** Builds monotonic normalized boundaries for the G-code role timeline. */
internal fun sliceInfoSegmentValues(estimatedTimes: FloatArray, visibleRoles: BooleanArray): FloatArray {
    val boundaries = FloatArray(estimatedTimes.size + 2)
    var total = 0.0
    for (index in estimatedTimes.indices) {
        val time = estimatedTimes[index]
        if (index < visibleRoles.size && visibleRoles[index] && time > 0f && time.isFinite()) {
            total += time.toDouble()
        }
    }

    if (total > 0.0 && total.isFinite()) {
        var cumulative = 0.0
        for (index in estimatedTimes.indices) {
            val time = estimatedTimes[index]
            if (index < visibleRoles.size && visibleRoles[index] && time > 0f && time.isFinite()) {
                cumulative += time.toDouble() / total
            }
            boundaries[index + 1] = cumulative.toFloat().coerceIn(0f, 1f)
        }
    }
    boundaries[0] = 0f
    boundaries[boundaries.lastIndex] = 1f
    return boundaries
}

internal object SliceInfoFormatter {
    fun formatTime(time: Float, hourLabel: String, minuteLabel: String, secondLabel: String): String {
        val totalSeconds = if (time.isFinite() && time > 0f) kotlin.math.ceil(time.toDouble()).toInt() else 0
        val seconds = totalSeconds % 60
        val minutes = (totalSeconds / 60) % 60
        val hours = totalSeconds / 3600
        val parts = buildList {
            if (hours > 0) add("$hours $hourLabel")
            if (minutes > 0) add("$minutes $minuteLabel")
            if (seconds > 0 || isEmpty()) add("$seconds $secondLabel")
        }
        return parts.joinToString(" ")
    }
}
