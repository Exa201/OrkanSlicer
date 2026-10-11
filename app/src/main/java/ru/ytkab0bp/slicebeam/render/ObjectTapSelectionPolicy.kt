package ru.ytkab0bp.slicebeam.render

/** Keeps a repeated tap on the already-selected model from toggling the selection off. */
internal object ObjectTapSelectionPolicy {
    data class Result(
        val primaryIndex: Int,
        val selectedIndices: Set<Int>,
        val changed: Boolean,
    )

    fun apply(
        currentPrimaryIndex: Int,
        currentSelection: Set<Int>,
        tappedIndex: Int,
    ): Result {
        if (tappedIndex < 0) {
            val changed = currentPrimaryIndex != -1 || currentSelection.isNotEmpty()
            return Result(-1, emptySet(), changed)
        }

        val alreadySelectedOnly = currentPrimaryIndex == tappedIndex &&
            currentSelection.size == 1 && currentSelection.contains(tappedIndex)
        if (alreadySelectedOnly) {
            return Result(currentPrimaryIndex, currentSelection.toSet(), changed = false)
        }

        return Result(tappedIndex, setOf(tappedIndex), changed = true)
    }

    fun clearForPlateSwitch(
        currentPrimaryIndex: Int,
        currentSelection: Set<Int>,
    ): Result {
        val changed = currentPrimaryIndex != -1 || currentSelection.isNotEmpty()
        return Result(-1, emptySet(), changed)
    }
}
