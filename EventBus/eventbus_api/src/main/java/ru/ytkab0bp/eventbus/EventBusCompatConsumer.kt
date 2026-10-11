package ru.ytkab0bp.eventbus

/** Compatibility callback used by generated listener implementations. */
interface EventBusCompatConsumer {
    /** Delivers an event to a listener. */
    fun onEvent(obj: Any?, event: Any?)
}
