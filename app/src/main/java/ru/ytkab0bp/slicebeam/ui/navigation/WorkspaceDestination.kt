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
    PROJECT(R.string.MenuProject, R.drawable.folder_simple_outline_28),
    MODEL(R.string.MenuModel, R.drawable.view_in_ar_24),
    CAMERA(R.string.MenuCamera, R.drawable.camera_outline_28),
    EDIT(R.string.MenuToolbar, R.drawable.menu_scale_28, R.string.MenuToolbarNavigation),
    TOOLS(R.string.MenuOptions, R.drawable.settings_outline_28),
    SLICE(R.string.MenuSlice, R.drawable.magic_wand_outline_28),

    ;

    companion object {
        /** Camera presets stay on the view cube; projection and gesture settings live under App. */
        val visibleNavigationDestinations: List<WorkspaceDestination> = values().filterNot { it == CAMERA }
    }
}
