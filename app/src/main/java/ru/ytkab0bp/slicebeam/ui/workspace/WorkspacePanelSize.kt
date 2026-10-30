package ru.ytkab0bp.slicebeam.ui.workspace

/** Describes a tool panel in Compose dp constraints rather than Android View pixels. */
internal data class WorkspacePanelSize(
    val portrait: WorkspacePanelDimension = WorkspacePanelDimension.Fraction(0.6f),
    val landscape: WorkspacePanelDimension = WorkspacePanelDimension.Fraction(0.6f),
)

internal sealed interface WorkspacePanelDimension {
    data class Fraction(val fraction: Float) : WorkspacePanelDimension
    data class Fixed(val dp: Float) : WorkspacePanelDimension
    data object FillAvailable : WorkspacePanelDimension
    data class ClampedFraction(
        val fraction: Float,
        val minDp: Float = 0f,
        val maxDp: Float = Float.POSITIVE_INFINITY,
    ) : WorkspacePanelDimension
}

/** Pure sizing policy used by the Compose host and JVM tests. */
internal object WorkspacePanelSizePolicy {
    fun resolve(dimension: WorkspacePanelDimension, availableDp: Float): Float {
        val available = availableDp.takeIf(Float::isFinite)?.coerceAtLeast(0f) ?: 0f
        val requested = when (dimension) {
            is WorkspacePanelDimension.Fraction -> available * dimension.fraction.finiteOrZero().coerceIn(0f, 1f)
            is WorkspacePanelDimension.Fixed -> dimension.dp.finiteOrZero().coerceAtLeast(0f)
            WorkspacePanelDimension.FillAvailable -> available
            is WorkspacePanelDimension.ClampedFraction -> {
                val min = dimension.minDp.finiteOrZero().coerceAtLeast(0f)
                val max = dimension.maxDp.takeIf(Float::isFinite)?.coerceAtLeast(min) ?: Float.POSITIVE_INFINITY
                (available * dimension.fraction.finiteOrZero().coerceAtLeast(0f)).coerceIn(min, max)
            }
        }
        return requested.coerceIn(0f, available)
    }

    private fun Float.finiteOrZero() = takeIf(Float::isFinite) ?: 0f
}
