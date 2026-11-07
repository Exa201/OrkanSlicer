package ru.ytkab0bp.slicebeam.components.bed_menu

import kotlin.math.sqrt

enum class CameraPreset {
    ISOMETRIC,
    TOP,
    BOTTOM,
    FRONT,
    BACK,
    LEFT,
    RIGHT,
}

internal data class CameraVector(val x: Double, val y: Double, val z: Double)

internal data class CameraPose(val origin: CameraVector, val position: CameraVector)

/** Pure camera-target geometry, kept separate from the renderer and Compose menu. */
internal object CameraPresetGeometry {
    fun resolve(preset: CameraPreset, min: CameraVector, max: CameraVector): CameraPose {
        val center = CameraVector(
            (min.x + max.x) / 2.0,
            (min.y + max.y) / 2.0,
            (min.z + max.z) / 2.0,
        )
        val sizeX = max.x - min.x
        val sizeY = max.y - min.y
        val sizeZ = max.z - min.z
        val origin = CameraVector(center.x, center.y, 0.0)
        val position = when (preset) {
            CameraPreset.ISOMETRIC -> CameraVector(
                center.x - center.z * 2.0,
                center.y - center.z * 2.0,
                min.z + sqrt(center.z * center.z * 8.0),
            )
            CameraPreset.TOP -> CameraVector(center.x, center.y - 1.0, max.z + sizeZ)
            CameraPreset.BOTTOM -> CameraVector(center.x, center.y - 1.0, min.z - sizeZ)
            CameraPreset.FRONT -> CameraVector(center.x, min.y - sizeY, 0.0)
            CameraPreset.BACK -> CameraVector(center.x, max.y + sizeY, 0.0)
            CameraPreset.LEFT -> CameraVector(min.x - sizeX, center.y, 0.0)
            CameraPreset.RIGHT -> CameraVector(max.x + sizeX, center.y, 0.0)
        }
        return CameraPose(origin, position)
    }
}
