package ru.ytkab0bp.slicebeam.render

/** Converts a failed native edit into an ordinary result so it cannot terminate GLThread. */
internal object ModelEditOperation {
    fun runSafely(
        failureResult: Int,
        onFailure: (Exception) -> Unit = {},
        operation: () -> Int,
    ): Int = try {
        operation()
    } catch (error: Exception) {
        onFailure(error)
        failureResult
    }
}
