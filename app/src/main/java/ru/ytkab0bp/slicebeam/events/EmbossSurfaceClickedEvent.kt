package ru.ytkab0bp.slicebeam.events

import ru.ytkab0bp.slicebeam.utils.Vec3d

open class EmbossSurfaceClickedEvent(
    @JvmField val position: Vec3d,
    @JvmField val normal: Vec3d,
)
