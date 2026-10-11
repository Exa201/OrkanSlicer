package ru.ytkab0bp.slicebeam.components.bed_menu

internal data class ModelSelectionAvailability(
    val canArrange: Boolean,
    val hasSelectedObject: Boolean,
)

/** Shares the legacy menu's exact model/selection enablement rule across Compose menus. */
internal fun modelSelectionAvailability(modelLoaded: Boolean, selectedObjectIndex: Int): ModelSelectionAvailability =
    ModelSelectionAvailability(
        canArrange = modelLoaded,
        hasSelectedObject = modelLoaded && selectedObjectIndex != -1,
    )
