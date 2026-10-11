package ru.ytkab0bp.slicebeam.components

import ru.ytkab0bp.eventbus.EventHandler
import ru.ytkab0bp.slicebeam.events.SlicingProgressEvent

/** Kotlin event-bus adapter for the slice progress sheet. */
class SliceProgressEventBridge(private val callback: Callback) {
    fun interface Callback {
        fun onProgressChanged(event: SlicingProgressEvent)
    }

    @EventHandler(runOnMainThread = true)
    fun onProgressChanged(event: SlicingProgressEvent) {
        callback.onProgressChanged(event)
    }
}
