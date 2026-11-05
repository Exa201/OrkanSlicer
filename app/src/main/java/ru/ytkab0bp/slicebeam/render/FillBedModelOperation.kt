package ru.ytkab0bp.slicebeam.render

import android.util.Log
import ru.ytkab0bp.slicebeam.slic3r.Model
import ru.ytkab0bp.slicebeam.utils.FillBedPlanner
import ru.ytkab0bp.slicebeam.utils.Vec3d

/** Plans fill-bed copies on a private model snapshot and commits only a complete arrangement. */
internal object FillBedModelOperation {
    private const val MAX_OBJECTS = 256
    // Very large one-shot NFP arrangements can monopolize the renderer thread on phones.
    // Fill in small batches; each batch places only its new copies against the fixed layout.
    private const val COPIES_PER_BATCH = 16

    /** Compatibility overload for tests and callers that provide their own all-object planner. */
    fun run(
        activeModel: Model,
        objectIndex: Int,
        viewerEnabled: Boolean,
        estimatedCapacity: Int,
        arrangeCandidate: (Model) -> Boolean,
    ): Int = run(activeModel, objectIndex, viewerEnabled, estimatedCapacity) { candidate, _ ->
        arrangeCandidate(candidate)
    }

    fun run(
        activeModel: Model,
        objectIndex: Int,
        viewerEnabled: Boolean,
        estimatedCapacity: Int,
        arrangeCandidate: (Model, firstNewObject: Int) -> Boolean,
    ): Int {
        if (viewerEnabled || objectIndex !in 0 until activeModel.objectsCount) return 0
        val maxAttempts = FillBedPlanner.copyAttemptsForLimit(activeModel.objectsCount, MAX_OBJECTS)
        if (maxAttempts == 0) return 0
        // The grid estimate is only a search hint. A zero can mean the object's axis-aligned
        // bounds exceed the plate even though its rotated or concave footprint still packs.
        // Always ask the exact native arranger about at least one copy before reporting no room.
        // Never add hundreds of speculative copies when an estimate is invalid.
        val estimatedCopiesToAdd = minOf(
            maxOf(estimatedCapacity - activeModel.objectsCount, 1), maxAttempts,
        )
        val originalCount = activeModel.objectsCount
        fun captureTranslations(count: Int): List<Vec3d> = List(count) { index ->
            Vec3d().also { activeModel.getTranslation(index, it) }
        }
        val originalTranslations = captureTranslations(originalCount)
        val sourceModel = Model()
        fun restoreState(objectCount: Int, translations: List<Vec3d>) {
            while (activeModel.objectsCount > objectCount) {
                activeModel.deleteObject(activeModel.objectsCount - 1)
            }
            translations.forEachIndexed { index, before ->
                if (index >= activeModel.objectsCount) {
                    throw IllegalStateException("Fill-bed trial removed a committed model object")
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
            var totalAdded = 0
            // Check one copy before asking the packer to solve a large batch. On a packed bed,
            // the largest-first binary search needlessly tries several impossible layouts and
            // can keep the GL thread busy even though the very first copy cannot fit.
            val initialCount = activeModel.objectsCount
            val initialTranslations = captureTranslations(initialCount)
            appendCopies(1)
            activeModel.resetBoundingBox()
            val firstFits = arrangeCandidate(activeModel, initialCount)
            val firstPlacement = if (firstFits) {
                Vec3d().also { activeModel.getTranslation(initialCount, it) }
            } else null
            restoreState(initialCount, initialTranslations)
            if (!firstFits || firstPlacement == null) return 0
            appendCopies(1)
            val firstCurrent = Vec3d()
            activeModel.getTranslation(initialCount, firstCurrent)
            activeModel.translate(
                initialCount,
                firstPlacement.x - firstCurrent.x,
                firstPlacement.y - firstCurrent.y,
                firstPlacement.z - firstCurrent.z,
            )
            activeModel.resetBoundingBox()
            totalAdded = 1
            while (totalAdded < estimatedCopiesToAdd) {
                val baselineCount = activeModel.objectsCount
                val baselineTranslations = captureTranslations(baselineCount)
                val batchLimit = minOf(COPIES_PER_BATCH, estimatedCopiesToAdd - totalAdded)
                var bestNewTranslations: List<Vec3d>? = null
                val addedThisBatch = FillBedPlanner.maximumPackableCopies(batchLimit) { candidate ->
                    restoreState(baselineCount, baselineTranslations)
                    appendCopies(candidate)
                    activeModel.resetBoundingBox()
                    val fits = try {
                        arrangeCandidate(activeModel, baselineCount)
                    } catch (error: Throwable) {
                        restoreState(baselineCount, baselineTranslations)
                        throw error
                    }
                    if (fits) {
                        val placements = (baselineCount until activeModel.objectsCount).map { index ->
                            Vec3d().also { activeModel.getTranslation(index, it) }
                        }
                        if (placements.size == candidate && placements.all {
                                it.x.isFinite() && it.y.isFinite() && it.z.isFinite()
                            }
                        ) bestNewTranslations = placements
                    }
                    restoreState(baselineCount, baselineTranslations)
                    fits && bestNewTranslations?.size == candidate
                }
                if (addedThisBatch == 0) break
                val placements = requireNotNull(bestNewTranslations)
                appendCopies(addedThisBatch)
                placements.forEachIndexed { offset, target ->
                    val index = baselineCount + offset
                    val current = Vec3d()
                    activeModel.getTranslation(index, current)
                    activeModel.translate(index, target.x - current.x, target.y - current.y, target.z - current.z)
                }
                activeModel.resetBoundingBox()
                totalAdded += addedThisBatch
            }
            return totalAdded
        } catch (error: Exception) {
            val rollbackFailure = runCatching { restoreState(originalCount, originalTranslations) }.exceptionOrNull()
            if (rollbackFailure != null) error.addSuppressed(rollbackFailure)
            Log.e("FillBedModelOperation", "Fill-bed packing failed; restored the working model", error)
            return 0
        } finally {
            sourceModel.release()
        }
    }
}
