package ru.ytkab0bp.slicebeam.events

/** Signals that the selected model object changed. Drag pickup should not open the action bar. */
open class SelectedObjectChangedEvent @JvmOverloads constructor(val showActionBar: Boolean = true)
