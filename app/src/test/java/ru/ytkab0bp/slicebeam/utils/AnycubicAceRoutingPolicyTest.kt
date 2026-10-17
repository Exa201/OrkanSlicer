package ru.ytkab0bp.slicebeam.utils

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AnycubicAceRoutingPolicyTest {
    @Test fun keepsToolAndSlotIndicesAcrossAnUnusedEmptyGate() {
        val routing = AnycubicAceRoutingPolicy.resolve(
            toolCount = 3,
            usedTools = intArrayOf(0, 2),
            loadedGates = intArrayOf(0, 2, 3),
            toolToGate = intArrayOf(0, 1, 2),
        )

        assertNull(routing.unavailableTool)
        assertArrayEquals(intArrayOf(0, 1, 2), routing.toolCommands)
        assertArrayEquals(intArrayOf(0, 1, 2), routing.gates)
    }

    @Test fun neverMovesAToolToAnotherLoadedGateWhenItsMatchingSlotIsEmpty() {
        val routing = AnycubicAceRoutingPolicy.resolve(
            toolCount = 3,
            usedTools = intArrayOf(1, 2),
            loadedGates = intArrayOf(0, 2),
            toolToGate = intArrayOf(0, 1, 2),
        )

        assertEquals(1, routing.unavailableTool)
        assertArrayEquals(intArrayOf(0, 1, 2), routing.toolCommands)
        assertArrayEquals(intArrayOf(0, 1, 2), routing.gates)
    }

    @Test fun rejectsFirmwareToolMapsThatWouldRedirectTheFixedSlotCommand() {
        val routing = AnycubicAceRoutingPolicy.resolve(
            toolCount = 3,
            usedTools = intArrayOf(2),
            loadedGates = intArrayOf(0, 2),
            toolToGate = intArrayOf(0, 2, 1),
        )

        assertEquals(2, routing.unavailableTool)
    }
}
