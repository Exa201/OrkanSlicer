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
        onError: (Exception) -> Unit = {},
    ): Int {
        val snapshots = try {
            items.map { item -> item to snapshot(item) }
        } catch (error: Exception) {
            runCatching { onError(error) }
            return failureResult
        }
        var attemptedCount = 0
        fun restoreAttempted() {
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
        }

        try {
            snapshots.forEach { (item, _) ->
                // Restore from a snapshot even if apply mutates and then throws.
                attemptedCount++
                apply(item)
            }
            val result = commit()
            if (result < 0) restoreAttempted()
            return result
        } catch (error: Exception) {
            restoreAttempted()
            runCatching { onError(error) }
            return failureResult
        }
    }
}
