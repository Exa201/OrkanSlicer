package ru.ytkab0bp.slicebeam.render

import ru.ytkab0bp.slicebeam.slic3r.Model

/** GL-thread snapshot used to keep delayed model-edit dialogs bound to the selection they opened for. */
internal class ModelSelectionSnapshot(
    val model: Model,
    val geometryRevision: Long,
    val objectCount: Int,
    val primaryObject: Int,
    selectedIndices: IntArray,
) {
    private val selectedIndicesSnapshot = selectedIndices.copyOf()

    /** Expose a copy so delayed UI callbacks cannot mutate the captured selection. */
    val selectedIndices: IntArray
        get() = selectedIndicesSnapshot.copyOf()

    fun matches(
        currentModel: Model?,
        currentPrimaryObject: Int,
        currentSelectedIndices: IntArray,
    ): Boolean = currentModel === model &&
        model.geometryRevision == geometryRevision &&
        model.objectsCount == objectCount &&
        currentPrimaryObject == primaryObject &&
        currentSelectedIndices.contentEquals(selectedIndicesSnapshot)
}
