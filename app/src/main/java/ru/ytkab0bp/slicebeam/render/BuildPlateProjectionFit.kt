package ru.ytkab0bp.slicebeam.render

/** Keeps the complete build plate in view when the renderer viewport changes shape. */
internal object BuildPlateProjectionFit {
    private const val PADDING = 1.08

    /**
     * Returns the orthographic half extents needed to contain the plate's projected half
     * bounds. The same scale is applied in both axes so resizing a docked paint panel does
     * not crop the plate or distort its perspective.
     */
    fun halfExtents(
        plateHalfWidth: Double,
        plateHalfHeight: Double,
        aspectRatio: Double,
        zoom: Double,
    ): Pair<Double, Double> {
        val aspect = aspectRatio.takeIf { it.isFinite() && it > 0.0 } ?: 1.0
        val safeZoom = zoom.takeIf { it.isFinite() && it > 0.0 } ?: 1.0
        val horizontalMultiplier = maxOf(1.0, aspect)
        val verticalMultiplier = maxOf(1.0, 1.0 / aspect)
        val scale = maxOf(
            plateHalfWidth.coerceAtLeast(0.0) / horizontalMultiplier,
            plateHalfHeight.coerceAtLeast(0.0) / verticalMultiplier,
        ) * PADDING / safeZoom
        return scale * horizontalMultiplier to scale * verticalMultiplier
    }
}
