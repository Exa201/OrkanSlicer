package ru.ytkab0bp.slicebeam.utils

/** Kobra 3 G-code tool numbers address ACE slots by the same zero-based index. */
object AnycubicAceRoutingPolicy {
    data class Routing(val toolCommands: IntArray, val gates: IntArray, val unavailableTool: Int?)

    @JvmStatic
    fun resolve(toolCount: Int, usedTools: IntArray, loadedGates: IntArray, toolToGate: IntArray): Routing {
        val count = toolCount.coerceAtLeast(1)
        val unavailable = usedTools.firstOrNull { tool ->
            tool !in 0 until count || tool !in loadedGates || toolToGate.getOrNull(tool) != tool
        }
        // Keep holes in the palette. Never compress or substitute another physical gate.
        val directIndices = IntArray(count) { it }
        return Routing(directIndices, directIndices.copyOf(), unavailable)
    }
}
