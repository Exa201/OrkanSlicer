package ru.ytkab0bp.slicebeam.render

import ru.ytkab0bp.slicebeam.slic3r.Model

/** GL-thread snapshot used to keep delayed model-edit dialogs bound to the selection they opened for. */
internal class ModelSelectionSnapshot(
    val model: Model,
    val geometryRevision: Long,
    val objectCount: Int,
    val primaryObject: Int,
    val selectedIndices: IntArray,
) {
    fun matches(
        currentModel: Model?,
        currentPrimaryObject: Int,
        currentSelectedIndices: IntArray,
    ): Boolean = currentModel === model &&
        model.geometryRevision == geometryRevision &&
        model.objectsCount == objectCount &&
        currentPrimaryObject == primaryObject &&
        currentSelectedIndices.contentEquals(selectedIndices)
}
