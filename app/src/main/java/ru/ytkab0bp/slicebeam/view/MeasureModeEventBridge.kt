package ru.ytkab0bp.slicebeam.view

import ru.ytkab0bp.eventbus.EventHandler
import ru.ytkab0bp.slicebeam.events.MeasurePointsChangedEvent

/** Kotlin event-bus adapter for measurement overlay changes. */
class MeasureModeEventBridge(private val callback: Callback) {
    fun interface Callback {
        fun onMeasurePointsChanged(event: MeasurePointsChangedEvent)
    }

    @EventHandler(runOnMainThread = true)
    fun onMeasurePointsChanged(event: MeasurePointsChangedEvent) {
        callback.onMeasurePointsChanged(event)
    }
}
