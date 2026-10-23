package ru.ytkab0bp.slicebeam.render

/** Applies a group placement only if the follow-up model join succeeds. */
internal object ModelPlacementTransaction {
    fun <T> applyAndCommit(
        items: List<T>,
        apply: (T) -> Unit,
        rollback: (T) -> Unit,
        commit: () -> Int,
        onError: (Exception) -> Unit = {},
    ): Int {
        val applied = ArrayList<T>(items.size)
        fun restoreApplied() {
            var rollbackFailure: Exception? = null
            applied.asReversed().forEach { item ->
                try {
                    rollback(item)
                } catch (error: Exception) {
                    val first = rollbackFailure
                    if (first == null) rollbackFailure = error else first.addSuppressed(error)
                }
            }
            rollbackFailure?.let { runCatching { onError(it) } }
        }

        try {
            items.forEach { item ->
                apply(item)
                applied += item
            }
            val result = commit()
            if (result < 0) restoreApplied()
            return result
        } catch (error: Exception) {
            restoreApplied()
            runCatching { onError(error) }
            return -1
        }
    }
}
