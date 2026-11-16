package ru.ytkab0bp.slicebeam.components.bed_menu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ru.ytkab0bp.slicebeam.utils.AnycubicAceRoutingPolicy

class SlicePrintPreparationRoutingTest {
    @Test
    fun unavailableGcodeToolOutsideThePaletteReturnsSafeWarningWithoutIndexingPastRoutes() {
        val routing = AnycubicAceRoutingPolicy.resolve(
            toolCount = 1,
            usedTools = intArrayOf(0, 1),
            loadedGates = intArrayOf(0),
            toolToGate = intArrayOf(0),
        )
        assertEquals(PrintPreparationWarning.UnavailableTool(tool = 1, gate = null),
            SlicePrintPreparationDialog.unavailableToolWarning(routing))
    }

    @Test
    fun inRangeUnavailableToolStillIdentifiesItsRequestedAceGate() {
        val routing = AnycubicAceRoutingPolicy.resolve(
            toolCount = 2,
            usedTools = intArrayOf(1),
            loadedGates = intArrayOf(0),
            toolToGate = intArrayOf(0, 3),
        )
        assertEquals(PrintPreparationWarning.UnavailableTool(tool = 1, gate = 3),
            SlicePrintPreparationDialog.unavailableToolWarning(routing))
    }

    @Test
    fun validRoutingDoesNotCreateUnavailableToolWarning() {
        val routing = AnycubicAceRoutingPolicy.resolve(
            toolCount = 2,
            usedTools = intArrayOf(0, 1),
            loadedGates = intArrayOf(0, 1),
            toolToGate = intArrayOf(0, 1),
        )
        assertNull(SlicePrintPreparationDialog.unavailableToolWarning(routing))
    }
}
