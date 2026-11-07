package ru.ytkab0bp.slicebeam.slic3r

/** Axes accepted by the native model mirror operation. */
enum class ModelMirrorAxis(internal val nativeValue: Int) {
    X(0),
    Y(1),
    Z(2),
}
