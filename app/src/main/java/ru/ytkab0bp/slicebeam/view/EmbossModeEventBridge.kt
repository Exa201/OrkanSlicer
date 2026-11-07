package ru.ytkab0bp.slicebeam.view

import ru.ytkab0bp.eventbus.EventHandler
import ru.ytkab0bp.slicebeam.events.EmbossSurfaceClickedEvent

/** Kotlin event-bus adapter for emboss-surface clicks. */
class EmbossModeEventBridge(private val callback: Callback) {
    fun interface Callback {
        fun onEmbossSurfaceClicked(event: EmbossSurfaceClickedEvent)
    }

    // Surface-click events are emitted on GL. Keep the callback off the UI looper;
    // the controller snapshots parameters and queues geometry work back onto GL.
    @EventHandler
    fun onEmbossSurfaceClicked(event: EmbossSurfaceClickedEvent) {
        callback.onEmbossSurfaceClicked(event)
    }
}
