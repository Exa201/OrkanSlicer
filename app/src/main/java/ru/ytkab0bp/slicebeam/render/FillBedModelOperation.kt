package ru.ytkab0bp.slicebeam.render

import android.util.Log
import ru.ytkab0bp.slicebeam.slic3r.Model
import ru.ytkab0bp.slicebeam.utils.FillBedPlanner
import ru.ytkab0bp.slicebeam.utils.Vec3d

/** Plans fill-bed copies on a private model snapshot and commits only a complete arrangement. */
internal object FillBedModelOperation {
    private const val MAX_OBJECTS = 256

    fun run(
        activeModel: Model,
        objectIndex: Int,
        viewerEnabled: Boolean,
        estimatedCapacity: Int,
        arrangeCandidate: (Model) -> Boolean,
    ): Int {
        if (viewerEnabled || objectIndex !in 0 until activeModel.objectsCount) return 0
        val maxAttempts = FillBedPlanner.copyAttemptsForLimit(activeModel.objectsCount, MAX_OBJECTS)
        if (maxAttempts == 0) return 0
        if (estimatedCapacity == 0) return 0
        // Allow one conservative trial for heterogeneous plates, but never add hundreds of
        // speculative copies when an estimate is invalid or no grid capacity remains.
        val copiesToAdd = minOf(maxOf(estimatedCapacity - activeModel.objectsCount, 1), maxAttempts)
        val originalCount = activeModel.objectsCount
        val originalTranslations = ArrayList<Vec3d>(originalCount)
        for (index in 0 until originalCount) {
            val translation = Vec3d()
            activeModel.getTranslation(index, translation)
            originalTranslations.add(translation)
        }
        val sourceModel = Model()
        fun restoreOriginalState() {
            while (activeModel.objectsCount > originalCount) {
                activeModel.deleteObject(activeModel.objectsCount - 1)
            }
            originalTranslations.forEachIndexed { index, before ->
                if (index >= activeModel.objectsCount) {
                    throw IllegalStateException("Fill-bed trial removed an existing model object")
                }
                val after = Vec3d()
                activeModel.getTranslation(index, after)
                activeModel.translate(index, before.x - after.x, before.y - after.y, before.z - after.z)
            }
            activeModel.resetBoundingBox()
        }

        fun appendCopies(count: Int) {
            repeat(count) {
                val expectedCount = activeModel.objectsCount + 1
                activeModel.addObject(sourceModel, 0)
                if (activeModel.objectsCount != expectedCount) {
                    throw IllegalStateException("Could not add a complete fill-bed copy")
                }
            }
        }

        try {
            sourceModel.addObject(activeModel, objectIndex)
            if (sourceModel.objectsCount != 1) {
                throw IllegalStateException("Could not snapshot the selected object for fill-bed")
            }
            // Arrange candidate counts against the same original state, then keep the largest
            // successful packing result. A failed native trial must never leak into the next.
            var bestTranslations: List<Vec3d>? = null
            val added = FillBedPlanner.maximumPackableCopies(copiesToAdd) { candidate ->
                restoreOriginalState()
                appendCopies(candidate)
                activeModel.resetBoundingBox()
                if (arrangeCandidate(activeModel)) {
                    val placements = (0 until activeModel.objectsCount).map { index ->
                        Vec3d().also { activeModel.getTranslation(index, it) }
                    }
                    if (placements.size == originalCount + candidate && placements.all {
                            it.x.isFinite() && it.y.isFinite() && it.z.isFinite()
                        }
                    ) {
                        bestTranslations = placements
                        true
                    } else {
                        restoreOriginalState()
                        false
                    }
                } else {
                    restoreOriginalState()
                    false
                }
            }
            if (added > 0) {
                val placements = requireNotNull(bestTranslations)
                restoreOriginalState()
                appendCopies(added)
                placements.forEachIndexed { index, target ->
                    val current = Vec3d()
                    activeModel.getTranslation(index, current)
                    activeModel.translate(index, target.x - current.x, target.y - current.y, target.z - current.z)
                }
            } else {
                restoreOriginalState()
            }
            return added
        } catch (error: Exception) {
            val rollbackFailure = runCatching { restoreOriginalState() }.exceptionOrNull()
            if (rollbackFailure != null) error.addSuppressed(rollbackFailure)
            Log.e("FillBedModelOperation", "Fill-bed packing failed; restored the working model", error)
            return 0
        } finally {
            sourceModel.release()
        }
    }
}
