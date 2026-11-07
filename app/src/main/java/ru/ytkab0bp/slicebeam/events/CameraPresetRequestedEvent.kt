package ru.ytkab0bp.slicebeam.events

import ru.ytkab0bp.slicebeam.components.bed_menu.CameraPreset

/** Routes an App settings camera preset selection to the live workspace renderer. */
class CameraPresetRequestedEvent(val preset: CameraPreset)
