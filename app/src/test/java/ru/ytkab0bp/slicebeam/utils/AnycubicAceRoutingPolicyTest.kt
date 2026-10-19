package ru.ytkab0bp.slicebeam.utils

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AnycubicAceRoutingPolicyTest {
    @Test fun keepsUnusedHolesAndRoutesUsedToolsToTheirDefaultLoadedGates() {
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

    @Test fun routesSelectedPhysicalGatesThroughFirmwareToolMap() {
        val routing = AnycubicAceRoutingPolicy.resolve(
            toolCount = 4,
            usedTools = intArrayOf(0, 1),
            loadedGates = intArrayOf(0, 1, 2, 3),
            // Firmware tool order differs from physical ACE gate order.
            toolToGate = intArrayOf(0, 2, 3, 1),
            selectedGates = intArrayOf(1, 3, 2, 0),
        )

        assertNull(routing.unavailableTool)
        assertNull(routing.sharedGateTool)
        assertArrayEquals(intArrayOf(3, 2, 1, 0), routing.toolCommands)
        assertArrayEquals(intArrayOf(1, 3, 2, 0), routing.gates)
    }

    @Test fun rejectsDistinctActiveToolsAssignedToTheSamePhysicalGate() {
        val routing = AnycubicAceRoutingPolicy.resolve(
            toolCount = 2,
            usedTools = intArrayOf(0, 1),
            loadedGates = intArrayOf(0, 1, 2, 3),
            toolToGate = intArrayOf(0, 1, 2, 3),
            selectedGates = intArrayOf(3, 3),
        )

        assertNull(routing.unavailableTool)
        assertEquals(1, routing.sharedGateTool)
    }

    @Test fun rejectsASelectedGateThatIsNoLongerLoadedInsteadOfSubstitutingAnother() {
        val routing = AnycubicAceRoutingPolicy.resolve(
            toolCount = 2,
            usedTools = intArrayOf(0, 1),
            loadedGates = intArrayOf(0, 1),
            toolToGate = intArrayOf(0, 1, 2, 3),
            selectedGates = intArrayOf(0, 3),
        )

        assertEquals(1, routing.unavailableTool)
    }
}
