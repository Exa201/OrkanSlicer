package ru.ytkab0bp.slicebeam.render

/** Retries a whole-plate pack with compact spacing after restoring the first attempt. */
internal object PlateArrangeFallback {
    inline fun run(
        arrangeWithNormalSpacing: () -> Boolean,
        restoreOriginalPositions: () -> Unit,
        arrangeCompactly: () -> Boolean,
        arrangeWithMinimumGap: () -> Boolean,
    ): Boolean {
        if (arrangeWithNormalSpacing()) return true
        restoreOriginalPositions()
        if (arrangeCompactly()) return true
        restoreOriginalPositions()
        val minimumGapSucceeded = arrangeWithMinimumGap()
        if (!minimumGapSucceeded) restoreOriginalPositions()
        return minimumGapSucceeded
    }
}
