package ru.ytkab0bp.slicebeam.components

import java.util.Locale

/** Pure progress policy shared by local model converters and their tests. */
internal object ConverterProgressModel {
    const val MAX_STAGE_PROGRESS = 94
    const val MAX_RUNNING_PROGRESS = 99
    private const val MIN_ESTIMATED_PERCENT = 3
    private const val STL_HEADER_BYTES = 84L
    private const val STL_FACET_BYTES = 50L
    private const val STL_FILE_PROGRESS_OFFSET = 8
    private const val STL_FILE_PROGRESS_SPAN = 84L

    data class Estimate(
        val percent: Int,
        val elapsedMillis: Long,
        val remainingMillis: Long?,
    )

    fun advanceStage(current: Int, requested: Int): Int =
        requested.coerceIn(0, MAX_STAGE_PROGRESS).coerceAtLeast(current.coerceIn(0, MAX_STAGE_PROGRESS))

    fun estimate(stageProgress: Int, fileProgress: Int, elapsedMillis: Long): Estimate {
        val percent = maxOf(stageProgress, fileProgress).coerceIn(0, MAX_RUNNING_PROGRESS)
        val elapsed = elapsedMillis.coerceAtLeast(0)
        val remaining = if (percent >= MIN_ESTIMATED_PERCENT && percent < 100) {
            (elapsed.toDouble() * (100 - percent) / percent).toLong().coerceAtLeast(0)
        } else {
            null
        }
        return Estimate(percent = percent, elapsedMillis = elapsed, remainingMillis = remaining)
    }

    fun stlFileProgress(fileLength: Long, facetCount: Long): Int {
        if (fileLength < STL_HEADER_BYTES || facetCount <= 0) return 0
        val expectedLength = STL_HEADER_BYTES + facetCount * STL_FACET_BYTES
        val sizeProgress = (fileLength.toDouble() * STL_FILE_PROGRESS_SPAN / expectedLength)
            .toLong()
            .coerceAtMost(STL_FILE_PROGRESS_SPAN)
            .toInt()
        return (STL_FILE_PROGRESS_OFFSET + sizeProgress).coerceIn(0, 92)
    }

    fun formatTime(millis: Long): String {
        var seconds = (millis / 1000).coerceAtLeast(0)
        val minutes = seconds / 60
        seconds %= 60
        return if (minutes >= 60) {
            String.format(Locale.GERMAN, "%d:%02d:%02d", minutes / 60, minutes % 60, seconds)
        } else {
            String.format(Locale.GERMAN, "%02d:%02d", minutes, seconds)
        }
    }
}
