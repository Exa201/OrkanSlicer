package ru.ytkab0bp.slicebeam.render

import android.util.Log
import ru.ytkab0bp.slicebeam.slic3r.Model
import ru.ytkab0bp.slicebeam.utils.FillBedPlanner

internal data class FillBedResult(val addedCopies: Int, val rearrangedExistingObjects: Boolean)
private data class FillBedTrial(val arrangementState: DoubleArray, val movedExistingObjects: Boolean)

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
    ): Int = runDetailed(activeModel, objectIndex, viewerEnabled, estimatedCapacity) { candidate, _, reflow ->
        !reflow && arrangeCandidate(candidate)
    }.addedCopies

    fun run(
        activeModel: Model,
        objectIndex: Int,
        viewerEnabled: Boolean,
        estimatedCapacity: Int,
        arrangeCandidate: (Model, firstNewObject: Int) -> Boolean,
    ): Int = runDetailed(activeModel, objectIndex, viewerEnabled, estimatedCapacity) { candidate, firstNewObject, reflow ->
        !reflow && arrangeCandidate(candidate, firstNewObject)
    }.addedCopies

    fun run(
        activeModel: Model,
        objectIndex: Int,
        viewerEnabled: Boolean,
        estimatedCapacity: Int,
        arrangeCandidate: (Model, firstNewObject: Int, rearrangeExistingObjects: Boolean) -> Boolean,
    ): Int = runDetailed(activeModel, objectIndex, viewerEnabled, estimatedCapacity, arrangeCandidate).addedCopies

    /** Retry failed fixed-layout placements with a full plate repack, requested by Fill Bed. */
    fun runDetailed(
        activeModel: Model,
        objectIndex: Int,
        viewerEnabled: Boolean,
        estimatedCapacity: Int,
        arrangeCandidate: (Model, firstNewObject: Int, rearrangeExistingObjects: Boolean) -> Boolean,
    ): FillBedResult {
        if (viewerEnabled || objectIndex !in 0 until activeModel.objectsCount) return FillBedResult(0, false)
        val maxAttempts = FillBedPlanner.copyAttemptsForLimit(activeModel.objectsCount, MAX_OBJECTS)
        if (maxAttempts == 0) return FillBedResult(0, false)
        // The grid estimate is only a search hint. A zero can mean the object's axis-aligned
        // bounds exceed the plate even though its rotated or concave footprint still packs.
        // Always ask the exact native arranger about at least one copy before reporting no room.
        // Never add hundreds of speculative copies when an estimate is invalid.
        val estimatedCopiesToAdd = minOf(
            maxOf(estimatedCapacity - activeModel.objectsCount, 1), maxAttempts,
        )
        val originalCount = activeModel.objectsCount
        fun captureState(): DoubleArray = activeModel.getArrangementState()
        val originalState = captureState()
        val sourceModel = Model()
        fun restoreState(objectCount: Int, state: DoubleArray) {
            while (activeModel.objectsCount > objectCount) {
                activeModel.deleteObject(activeModel.objectsCount - 1)
            }
            if (activeModel.objectsCount != objectCount || !activeModel.setArrangementState(state)) {
                throw IllegalStateException("Fill-bed trial could not restore the complete model transform state")
            }
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

        /** Try to keep user placements first, then permit a full repack only if necessary. */
        fun arrangeCopies(
            count: Int,
            baselineCount: Int,
            baselineState: DoubleArray,
            allowReflow: Boolean,
        ): FillBedTrial? {
            try {
                appendCopies(count)
                activeModel.resetBoundingBox()
                var arranged = arrangeCandidate(activeModel, baselineCount, false)
                if (!arranged && allowReflow) {
                    restoreState(baselineCount, baselineState)
                    appendCopies(count)
                    activeModel.resetBoundingBox()
                    arranged = arrangeCandidate(activeModel, baselineCount, true)
                }
                if (!arranged) return null
                val state = captureState()
                if (state.size <= baselineState.size || state.any { !it.isFinite() }) return null
                val movedExistingObjects = baselineState.indices.any { index ->
                    kotlin.math.abs(state[index] - baselineState[index]) > 1e-6
                }
                return FillBedTrial(state, movedExistingObjects)
            } finally {
                restoreState(baselineCount, baselineState)
            }
        }

        fun applyState(state: DoubleArray) {
            check(activeModel.setArrangementState(state)) {
                "Fill-bed result does not contain a valid transform for every model instance"
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
            val initialState = captureState()
            val firstTrial = arrangeCopies(1, initialCount, initialState, allowReflow = true)
                ?: return FillBedResult(0, false)
            appendCopies(1)
            applyState(firstTrial.arrangementState)
            totalAdded = 1
            var movedExistingObjects = firstTrial.movedExistingObjects
            while (totalAdded < estimatedCopiesToAdd) {
                val baselineCount = activeModel.objectsCount
                val baselineState = captureState()
                val batchLimit = minOf(COPIES_PER_BATCH, estimatedCopiesToAdd - totalAdded)
                var bestTrial: FillBedTrial? = null
                val addedThisBatch = FillBedPlanner.maximumPackableCopies(batchLimit) { candidate ->
                    // Don't ask the expensive whole-plate packer to probe batches that are
                    // already known not to fit. Binary search eventually tests one copy; only
                    // that final single-copy probe may reflow the plate.
                    bestTrial = arrangeCopies(
                        candidate, baselineCount, baselineState, allowReflow = candidate == 1,
                    )
                    bestTrial != null
                }
                if (addedThisBatch == 0) break
                appendCopies(addedThisBatch)
                val trial = requireNotNull(bestTrial)
                applyState(trial.arrangementState)
                movedExistingObjects = movedExistingObjects || trial.movedExistingObjects
                totalAdded += addedThisBatch
            }
            return FillBedResult(totalAdded, movedExistingObjects)
        } catch (error: Exception) {
            val rollbackFailure = runCatching { restoreState(originalCount, originalState) }.exceptionOrNull()
            if (rollbackFailure != null) error.addSuppressed(rollbackFailure)
            Log.e("FillBedModelOperation", "Fill-bed packing failed; restored the working model", error)
            return FillBedResult(0, false)
        } finally {
            sourceModel.release()
        }
    }
}
