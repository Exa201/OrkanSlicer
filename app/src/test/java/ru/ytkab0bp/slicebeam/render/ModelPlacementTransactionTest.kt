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
            apply = { positions[it] = positions.getValue(it) + 10.0 },
            rollback = { positions[it] = positions.getValue(it) - 10.0 },
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
            apply = { positions[it] = positions.getValue(it) + 10.0 },
            rollback = {
                rollbackOrder += it
                positions[it] = positions.getValue(it) - 10.0
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
            apply = { positions[it] = positions.getValue(it) + 10.0 },
            rollback = { positions[it] = positions.getValue(it) - 10.0 },
            commit = { throw IllegalStateException("native join rejected the placement") },
            onError = errors::add,
        )

        assertEquals(-1, result)
        assertEquals(mapOf(1 to 4.0, 2 to 8.0), positions)
        assertEquals(1, errors.size)
        assertTrue(errors.single().message.orEmpty().contains("native join"))
    }

    @Test
    fun failedSecondMoveRollsBackEarlierMoves() {
        val positions = mutableMapOf(1 to 4.0, 2 to 8.0)
        val errors = mutableListOf<Exception>()
        val result = ModelPlacementTransaction.applyAndCommit(
            items = listOf(1, 2),
            apply = {
                if (it == 2) throw IllegalStateException("second move failed before mutation")
                positions[it] = positions.getValue(it) + 10.0
            },
            rollback = { positions[it] = positions.getValue(it) - 10.0 },
            commit = { 0 },
            onError = errors::add,
        )

        assertEquals(-1, result)
        assertEquals(mapOf(1 to 4.0, 2 to 8.0), positions)
        assertEquals(1, errors.size)
    }
}
