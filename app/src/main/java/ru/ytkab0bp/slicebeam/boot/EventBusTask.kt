package ru.ytkab0bp.slicebeam.boot

import ru.ytkab0bp.eventbus.EventBus

open class EventBusTask : BootTask(Runnable {
    // The processor generates handlers in the Java namespace, not applicationId.
    EventBus.registerImpl("ru.ytkab0bp.slicebeam")
}) {
    init {
        onWorker()
    }
}
