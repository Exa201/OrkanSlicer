package ru.ytkab0bp.slicebeam.events

import ru.ytkab0bp.eventbus.Event

@Event
class TemporaryProjectSettingsChangedEvent(val settingsChanged: Boolean = true)
