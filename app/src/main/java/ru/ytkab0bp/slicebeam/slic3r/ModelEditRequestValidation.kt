package ru.ytkab0bp.slicebeam.slic3r

import ru.ytkab0bp.slicebeam.utils.Vec3d

/** Rejects malformed edit requests before they cross the JNI/native geometry boundary. */
internal object ModelEditRequestValidation {
    data class TextPlacementSnapshot(val position: Vec3d, val normal: Vec3d)

    /** Copy mutable request inputs before validation so validation and JNI observe one snapshot. */
    fun distinctObjectIndicesSnapshot(indices: IntArray, objectCount: Int): IntArray? =
        indices.copyOf().takeIf { hasDistinctObjectIndices(it, objectCount) }

    fun snapshotTextPlacement(position: Vec3d, normal: Vec3d) =
        TextPlacementSnapshot(Vec3d(position), Vec3d(normal))

    fun hasDistinctObjectIndices(indices: IntArray, objectCount: Int): Boolean =
        indices.size >= 2 && objectCount >= 2 &&
            indices.all { it in 0 until objectCount } && indices.toSet().size == indices.size

    fun isValidTextRequest(
        fontPath: String,
        text: String,
        size: Float,
        depth: Float,
        position: Vec3d,
        normal: Vec3d,
        targetObjectIndex: Int,
        objectCount: Int,
    ): Boolean {
        if (fontPath.isBlank() || text.isBlank() || !size.isFinite() || size <= 0f ||
            !depth.isFinite() || depth <= 0f
        ) return false
        if (!position.x.isFinite() || !position.y.isFinite() || !position.z.isFinite() ||
            !normal.x.isFinite() || !normal.y.isFinite() || !normal.z.isFinite()
        ) return false
        val normalLengthSquared = normal.x * normal.x + normal.y * normal.y + normal.z * normal.z
        if (!normalLengthSquared.isFinite() || normalLengthSquared <= 1e-18) return false
        return targetObjectIndex == -1 || targetObjectIndex in 0 until objectCount
    }
}
