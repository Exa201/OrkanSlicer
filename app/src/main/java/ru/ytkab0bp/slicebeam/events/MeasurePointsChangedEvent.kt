package ru.ytkab0bp.slicebeam.events

import ru.ytkab0bp.slicebeam.utils.Vec3d

open class MeasurePointsChangedEvent(
    @JvmField val pointA: Vec3d?,
    @JvmField val pointB: Vec3d?,
)
