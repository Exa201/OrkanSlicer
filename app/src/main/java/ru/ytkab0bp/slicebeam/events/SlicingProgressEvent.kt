package ru.ytkab0bp.slicebeam.events

open class SlicingProgressEvent(
    @JvmField val progress: Int,
    @JvmField val message: String?,
)
