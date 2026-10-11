package ru.ytkab0bp.slicebeam.view

import ru.ytkab0bp.eventbus.EventHandler
import ru.ytkab0bp.slicebeam.events.EmbossSurfaceClickedEvent

/** Kotlin event-bus adapter for emboss-surface clicks. */
class EmbossModeEventBridge(private val callback: Callback) {
    fun interface Callback {
        fun onEmbossSurfaceClicked(event: EmbossSurfaceClickedEvent)
    }

    @EventHandler(runOnMainThread = true)
    fun onEmbossSurfaceClicked(event: EmbossSurfaceClickedEvent) {
        callback.onEmbossSurfaceClicked(event)
    }
}
