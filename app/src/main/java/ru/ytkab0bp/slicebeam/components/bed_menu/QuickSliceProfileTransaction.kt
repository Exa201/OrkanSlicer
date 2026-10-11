package ru.ytkab0bp.slicebeam.components.bed_menu

import ru.ytkab0bp.slicebeam.config.ConfigObject
import ru.ytkab0bp.slicebeam.config.ProfileConfigSnapshot
import ru.ytkab0bp.slicebeam.config.TemporaryProjectSettings
import ru.ytkab0bp.slicebeam.config.ProfileSettingsRepository
import ru.ytkab0bp.slicebeam.slic3r.Slic3rConfigWrapper

/** Applies a Quick Slice profile as one recoverable write. */
internal object QuickSliceProfileTransaction {
    const val PROFILE_NAME = "OrkanSlicer Quick Slice"

    enum class Result {
        SAVED,
        PRINT_PROFILE_MISSING,
        SAVE_FAILED,
    }

    data class Values(
        val layerHeight: String,
        val infillPercent: String,
        val supports: Boolean,
        val brim: Boolean,
    )

    fun apply(
        config: Slic3rConfigWrapper,
        values: Values,
        persist: () -> Boolean,
    ): Result {
        if (TemporaryProjectSettings.active) {
            ProfileSettingsRepository.setPendingValues(ConfigObject.PROFILE_LIST_PRINT, mapOf(
                "layer_height" to values.layerHeight,
                "sparse_infill_density" to "${values.infillPercent}%",
                "enable_support" to if (values.supports) "1" else "0",
                "brim_width" to if (values.brim) "5" else "0",
                "brim_type" to if (values.brim) "outer_only" else "no_brim",
            ))
            return Result.SAVED
        }
        val presets = config.presets ?: return Result.PRINT_PROFILE_MISSING
        val base = config.findPrint(presets["print"]) ?: return Result.PRINT_PROFILE_MISSING
        val snapshot = ProfileConfigSnapshot(config)

        return try {
            val quick = config.findPrint(PROFILE_NAME) ?: ConfigObject(base).also {
                it.setTitle(PROFILE_NAME)
                config.printConfigs.add(it)
            }
            quick.put("layer_height", values.layerHeight)
            quick.put("fill_density", "${values.infillPercent}%")
            quick.put("support_material", if (values.supports) "1" else "0")
            quick.put("brim_width", if (values.brim) "5" else "0")
            quick.put("brim_type", if (values.brim) "outer_only" else "no_brim")
            presets.put("print", PROFILE_NAME)

            if (!persist()) {
                snapshot.restore()
                Result.SAVE_FAILED
            } else {
                Result.SAVED
            }
        } catch (_: Exception) {
            snapshot.restore()
            Result.SAVE_FAILED
        }
    }
}
