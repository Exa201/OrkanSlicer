package ru.ytkab0bp.slicebeam.events

import ru.ytkab0bp.eventbus.Event

@Event
open class LongClickTranslationEvent(
    @JvmField val x: Double,
    @JvmField val y: Double,
    @JvmField val visual: Boolean
)
