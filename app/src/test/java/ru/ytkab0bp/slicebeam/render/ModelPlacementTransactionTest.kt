package ru.ytkab0bp.slicebeam.render

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelPlacementTransactionTest {
    @Test
    fun successfulJoinKeepsTheAppliedPlacement() {
        val positions = mutableMapOf(1 to 4.0, 2 to 8.0)
        val result = ModelPlacementTransaction.applyAndCommit(
            items = listOf(1, 2),
            snapshot = positions::getValue,
            apply = { positions[it] = positions.getValue(it) + 10.0 },
            restore = { item, before -> positions[item] = before },
            commit = { 0 },
        )

        assertEquals(0, result)
        assertEquals(mapOf(1 to 14.0, 2 to 18.0), positions)
    }

    @Test
    fun rejectedJoinRestoresAllMovedObjectsInReverseOrder() {
        val positions = mutableMapOf(1 to 4.0, 2 to 8.0)
        val rollbackOrder = mutableListOf<Int>()
        val result = ModelPlacementTransaction.applyAndCommit(
            items = listOf(1, 2),
            snapshot = positions::getValue,
            apply = { positions[it] = positions.getValue(it) + 10.0 },
            restore = { item, before ->
                rollbackOrder += item
                positions[item] = before
            },
            commit = { -1 },
        )

        assertEquals(-1, result)
        assertEquals(listOf(2, 1), rollbackOrder)
        assertEquals(mapOf(1 to 4.0, 2 to 8.0), positions)
    }

    @Test
    fun thrownJoinRestoresMovedObjectsAndReportsTheNativeFailure() {
        val positions = mutableMapOf(1 to 4.0, 2 to 8.0)
        val errors = mutableListOf<Exception>()
        val result = ModelPlacementTransaction.applyAndCommit(
            items = listOf(1, 2),
            snapshot = positions::getValue,
            apply = { positions[it] = positions.getValue(it) + 10.0 },
            restore = { item, before -> positions[item] = before },
            commit = { throw IllegalStateException("native join rejected the placement") },
            onError = errors::add,
        )

        assertEquals(-1, result)
        assertEquals(mapOf(1 to 4.0, 2 to 8.0), positions)
        assertEquals(1, errors.size)
        assertTrue(errors.single().message.orEmpty().contains("native join"))
    }

    @Test
    fun thrownNativeJoinReturnsDedicatedFailureStatusAfterPlacementRollback() {
        val positions = mutableMapOf(1 to 4.0, 2 to 8.0)
        val failureStatus = MODEL_EDIT_OPERATION_FAILED
        val result = ModelPlacementTransaction.applyAndCommit(
            items = listOf(1, 2),
            snapshot = positions::getValue,
            apply = { positions[it] = positions.getValue(it) + 10.0 },
            restore = { item, before -> positions[item] = before },
            commit = { throw IllegalStateException("native join failed") },
            failureResult = failureStatus,
        )

        assertEquals(failureStatus, result)
        assertEquals(mapOf(1 to 4.0, 2 to 8.0), positions)
    }

    @Test
    fun rollbackFailureOverridesRejectedJoinStatus() {
        val positions = mutableMapOf(1 to 4.0, 2 to 8.0)
        val rollbackFailureStatus = -5
        val result = ModelPlacementTransaction.applyAndCommit(
            items = listOf(1, 2),
            snapshot = positions::getValue,
            apply = { positions[it] = positions.getValue(it) + 10.0 },
            restore = { item, before ->
                if (item == 2) throw IllegalStateException("restore failed")
                positions[item] = before
            },
            commit = { -1 },
            failureResult = MODEL_EDIT_OPERATION_FAILED,
            rollbackFailureResult = rollbackFailureStatus,
        )

        assertEquals(rollbackFailureStatus, result)
        assertEquals(4.0, positions.getValue(1), 0.0)
        assertEquals(18.0, positions.getValue(2), 0.0)
    }

    @Test
    fun rollbackFailureIsReportedWhenPlacementThrowsAfterMutation() {
        val positions = mutableMapOf(1 to 4.0, 2 to 8.0)
        val errors = mutableListOf<Exception>()
        val rollbackFailureStatus = -5
        val result = ModelPlacementTransaction.applyAndCommit(
            items = listOf(1, 2),
            snapshot = positions::getValue,
            apply = {
                positions[it] = positions.getValue(it) + 10.0
                if (it == 2) throw IllegalStateException("placement failed")
            },
            restore = { item, before ->
                if (item == 2) throw IllegalStateException("restore failed")
                positions[item] = before
            },
            commit = { 0 },
            failureResult = MODEL_EDIT_OPERATION_FAILED,
            rollbackFailureResult = rollbackFailureStatus,
            onError = errors::add,
        )

        assertEquals(rollbackFailureStatus, result)
        assertEquals(4.0, positions.getValue(1), 0.0)
        assertEquals(18.0, positions.getValue(2), 0.0)
        assertEquals(2, errors.size)
    }

    @Test
    fun partiallyAppliedSecondMoveRestoresCurrentAndEarlierMoves() {
        val positions = mutableMapOf(1 to 4.0, 2 to 8.0)
        val errors = mutableListOf<Exception>()
        val restoreOrder = mutableListOf<Int>()
        val result = ModelPlacementTransaction.applyAndCommit(
            items = listOf(1, 2),
            snapshot = positions::getValue,
            apply = {
                positions[it] = positions.getValue(it) + 10.0
                if (it == 2) throw IllegalStateException("second move failed after partial mutation")
            },
            restore = { item, before ->
                restoreOrder += item
                positions[item] = before
            },
            commit = { 0 },
            onError = errors::add,
        )

        assertEquals(-1, result)
        assertEquals(mapOf(1 to 4.0, 2 to 8.0), positions)
        assertEquals(listOf(2, 1), restoreOrder)
        assertEquals(1, errors.size)
    }

    @Test
    fun snapshotFailureDoesNotBeginAnyPlacement() {
        val positions = mutableMapOf(1 to 4.0, 2 to 8.0)
        val errors = mutableListOf<Exception>()
        val result = ModelPlacementTransaction.applyAndCommit(
            items = listOf(1, 2),
            snapshot = { item ->
                if (item == 2) throw IllegalStateException("could not snapshot target")
                positions.getValue(item)
            },
            apply = { positions[it] = positions.getValue(it) + 10.0 },
            restore = { item, before -> positions[item] = before },
            commit = { 0 },
            onError = errors::add,
        )

        assertEquals(-1, result)
        assertEquals(mapOf(1 to 4.0, 2 to 8.0), positions)
        assertEquals(1, errors.size)
    }
}
