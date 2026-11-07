package ru.ytkab0bp.slicebeam.events

import ru.ytkab0bp.slicebeam.utils.Vec3d
import ru.ytkab0bp.slicebeam.slic3r.BooleanOperation

/** A precise tap on the chosen target while placing a selected object assembly. */
open class ObjectSurfacePlacementTapEvent(
    @JvmField val targetObjectIndex: Int,
    @JvmField val position: Vec3d,
    @JvmField val normal: Vec3d,
    @JvmField val booleanOperation: BooleanOperation? = null,
)
