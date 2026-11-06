package ru.ytkab0bp.slicebeam.render

/** Retries a whole-plate pack with compact spacing after restoring the first attempt. */
internal object PlateArrangeFallback {
    inline fun run(
        arrangeWithNormalSpacing: () -> Boolean,
        restoreOriginalPositions: () -> Unit,
        arrangeCompactly: () -> Boolean,
    ): Boolean {
        if (arrangeWithNormalSpacing()) return true
        restoreOriginalPositions()
        val compactSucceeded = arrangeCompactly()
        if (!compactSucceeded) restoreOriginalPositions()
        return compactSucceeded
    }
}
