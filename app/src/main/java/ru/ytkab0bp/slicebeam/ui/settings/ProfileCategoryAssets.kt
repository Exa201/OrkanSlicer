package ru.ytkab0bp.slicebeam.ui.settings

import androidx.annotation.DrawableRes
import ru.ytkab0bp.slicebeam.R

/** Multicolor isometric category art shared by print, filament, and printer profiles. */
internal object ProfileCategoryAssets {
    @DrawableRes
    fun iconFor(title: String, @DrawableRes fallback: Int): Int = when (title.trim().lowercase()) {
        "layers and perimeters", "top/bottom layers", "top / bottom layers", "quality" -> R.drawable.menu3d_layers
        "infill", "skirt and brim", "skirt, brim", "support", "support material" -> R.drawable.menu3d_fill_bed
        "speed", "advanced", "dependencies", "filament overrides" -> R.drawable.menu3d_tune
        "cooling", "cooling and fan" -> R.drawable.menu3d_purge_volume
        "multiple extruders", "extruder" -> R.drawable.menu3d_prime_tower
        "output options", "custom g-code" -> R.drawable.menu3d_file_export
        "filament" -> R.drawable.menu3d_spool
        "general", "printer" -> R.drawable.menu3d_printer
        "machine limits" -> R.drawable.menu3d_tune
        "notes" -> R.drawable.menu3d_file_import
        "physical printer" -> R.drawable.menu3d_printer
        else -> fallback
    }
}
