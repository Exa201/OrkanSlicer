package ru.ytkab0bp.slicebeam.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import ru.ytkab0bp.slicebeam.R

/** Stable destinations for the model workspace and its adaptive navigation. */
internal enum class WorkspaceDestination(
    @StringRes val titleRes: Int,
    @DrawableRes val iconRes: Int,
    @StringRes val navigationTitleRes: Int = titleRes,
) {
    PROJECT(R.string.MenuProject, R.drawable.vk_folder_simple_outline_24),
    MODEL(R.string.MenuModel, R.drawable.vk_cube_box_outline_24),
    CAMERA(R.string.MenuCamera, R.drawable.vk_camera_outline_24),
    EDIT(R.string.MenuToolbar, R.drawable.vk_sliders_outline_20, R.string.MenuToolbarNavigation),
    TOOLS(R.string.MenuOptions, R.drawable.vk_gear_outline_24),
    SLICE(R.string.MenuSlice, R.drawable.vk_magic_wand_outline_24),

    ;

    companion object {
        /** Camera presets stay on the view cube; projection and gesture settings live under App. */
        val visibleNavigationDestinations: List<WorkspaceDestination> = values().filterNot { it == CAMERA }
    }
}
