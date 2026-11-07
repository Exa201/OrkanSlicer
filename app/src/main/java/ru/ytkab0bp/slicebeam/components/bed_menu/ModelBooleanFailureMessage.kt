package ru.ytkab0bp.slicebeam.components.bed_menu

import androidx.annotation.StringRes
import ru.ytkab0bp.slicebeam.R

/** Maps the stable negative status codes returned by Native.model_boolean_objects to UI guidance. */
internal object ModelBooleanFailureMessage {
    @StringRes
    fun resourceFor(status: Int): Int? = when (status) {
        -2 -> R.string.ModelBooleanUnsupportedInstances
        -3 -> R.string.ModelBooleanInvalidMesh
        -4 -> R.string.ModelBooleanInvalidResult
        -5 -> R.string.ModelBooleanResultCreationFailed
        -6 -> R.string.ModelBooleanEngineFailed
        -7 -> R.string.ModelBooleanNoCut
        else -> null
    }
}
