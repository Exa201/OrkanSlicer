package ru.ytkab0bp.slicebeam.view

import ru.ytkab0bp.eventbus.EventHandler
import ru.ytkab0bp.slicebeam.events.EmbossSvgSelectedEvent

/** Bridges SAF file selection into the active Compose emboss panel/controller. */
internal class EmbossSvgSelectionBridge(private val callback: (String) -> Unit) {
    @EventHandler
    fun onSvgSelected(event: EmbossSvgSelectedEvent) = callback(event.absolutePath)
}
