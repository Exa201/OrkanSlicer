package ru.ytkab0bp.slicebeam.utils

/** One filament in the multi-color palette: an ARGB color and optional material name. */
class FilamentSlot(color: Int, @JvmField var type: String?) {
    @JvmField
    var color: Int = color or 0xFF000000.toInt()
}
