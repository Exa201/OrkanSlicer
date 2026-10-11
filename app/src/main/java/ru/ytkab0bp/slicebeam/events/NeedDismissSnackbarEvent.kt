package ru.ytkab0bp.slicebeam.events

import ru.ytkab0bp.eventbus.Event

@Event
open class NeedDismissSnackbarEvent(@JvmField val tag: String?)
