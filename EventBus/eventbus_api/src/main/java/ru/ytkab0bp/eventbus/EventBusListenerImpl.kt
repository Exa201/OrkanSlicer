package ru.ytkab0bp.eventbus

import kotlin.jvm.JvmDefaultWithCompatibility

/** @hide */
@JvmDefaultWithCompatibility
interface EventBusListenerImpl {
    /** Delivers events to the listener. */
    fun onEvent(obj: Any?, event: Any?)

    /** Returns whether the event may be delivered to listeners of a parent event. */
    fun canFireParent(clz: Class<*>): Boolean = false
}
