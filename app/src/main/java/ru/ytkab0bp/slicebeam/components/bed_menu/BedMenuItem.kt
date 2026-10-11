package ru.ytkab0bp.slicebeam.components.bed_menu

import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

/** Toggle listener for a Compose-rendered workspace menu action. */
fun interface BedMenuItemCheckedChangeListener {
    fun onCheckedChanged(checked: Boolean)
}

/** Action data consumed by the adaptive Compose workspace menus. */
open class BedMenuItem(
    @StringRes val titleRes: Int,
    @DrawableRes val iconRes: Int,
) {
    var isEnabled: Boolean = true
        private set

    var isChecked: Boolean = false
        internal set

    var isCheckable: Boolean = false
        private set

    var sectionBreakBefore: Boolean = false
        private set

    var clickListener: ((View) -> Unit)? = null
        private set

    var checkedChangeListener: BedMenuItemCheckedChangeListener? = null
        private set

    open fun onClick(listener: ((View) -> Unit)?): BedMenuItem = apply {
        clickListener = listener
    }

    open fun setCheckable(
        listener: BedMenuItemCheckedChangeListener?,
        checked: Boolean,
    ): BedMenuItem = apply {
        checkedChangeListener = listener
        isCheckable = true
        isChecked = checked
    }

    open fun setEnabled(enabled: Boolean): BedMenuItem = apply {
        isEnabled = enabled
    }

    open fun setSectionBreakBefore(enabled: Boolean = true): BedMenuItem = apply {
        sectionBreakBefore = enabled
    }
}
