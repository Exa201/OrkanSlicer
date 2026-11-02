package ru.ytkab0bp.slicebeam.utils

/** One filament in the multi-color palette: an ARGB color and optional material name. */
class FilamentSlot @JvmOverloads constructor(
    color: Int,
    @JvmField var type: String?,
    /** A loaded MMU spool whose RFID/material could not be identified; this flag is transient. */
    @JvmField val unknownLoadedSpool: Boolean = false,
) {
    @JvmField
    var color: Int = color or 0xFF000000.toInt()
}
