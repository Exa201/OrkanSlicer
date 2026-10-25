package ru.ytkab0bp.slicebeam.render

/** Applies a group placement only if the follow-up model join succeeds. */
internal object ModelPlacementTransaction {
    fun <T, S> applyAndCommit(
        items: List<T>,
        snapshot: (T) -> S,
        apply: (T) -> Unit,
        restore: (T, S) -> Unit,
        commit: () -> Int,
        failureResult: Int = -1,
        rollbackFailureResult: Int = failureResult,
        onError: (Exception) -> Unit = {},
    ): Int {
        val snapshots = try {
            items.map { item -> item to snapshot(item) }
        } catch (error: Exception) {
            runCatching { onError(error) }
            return failureResult
        }
        var attemptedCount = 0
        fun restoreAttempted(): Boolean {
            var rollbackFailure: Exception? = null
            snapshots.take(attemptedCount).asReversed().forEach { (item, before) ->
                try {
                    restore(item, before)
                } catch (error: Exception) {
                    val first = rollbackFailure
                    if (first == null) rollbackFailure = error else first.addSuppressed(error)
                }
            }
            rollbackFailure?.let { runCatching { onError(it) } }
            return rollbackFailure != null
        }

        try {
            snapshots.forEach { (item, _) ->
                // Restore from a snapshot even if apply mutates and then throws.
                attemptedCount++
                apply(item)
            }
            val result = commit()
            if (result < 0 && restoreAttempted()) return rollbackFailureResult
            return result
        } catch (error: Exception) {
            val rollbackFailed = restoreAttempted()
            runCatching { onError(error) }
            return if (rollbackFailed) rollbackFailureResult else failureResult
        }
    }
}
