package ru.ytkab0bp.eventbus.demo.events

import ru.ytkab0bp.eventbus.Event

@Event
open class TestEvent {
    @JvmField
    var startMs: Long = System.currentTimeMillis()
}
