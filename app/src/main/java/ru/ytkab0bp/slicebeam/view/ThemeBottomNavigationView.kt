package ru.ytkab0bp.slicebeam.view

import android.content.Context
import android.content.res.ColorStateList
import com.google.android.material.bottomnavigation.BottomNavigationView
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.theme.IThemeView
import ru.ytkab0bp.slicebeam.theme.ThemesRepo

class ThemeBottomNavigationView(context: Context) : BottomNavigationView(context), IThemeView {
    init {
        setItemTextAppearanceInactive(R.style.Theme_SliceBeam_NavigationTextFix)
        setItemTextAppearanceActive(R.style.Theme_SliceBeam_NavigationTextFixActive)
        onApplyTheme()
    }

    override fun onApplyTheme() {
        setBackgroundColor(ThemesRepo.getColor(android.R.attr.windowBackground))
        setItemActiveIndicatorColor(ColorStateList.valueOf(ThemesRepo.getColor(android.R.attr.colorAccent)))
        setItemTextColor(
            ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_enabled, android.R.attr.state_checked),
                    intArrayOf(android.R.attr.state_enabled, -android.R.attr.state_checked),
                ),
                intArrayOf(
                    ThemesRepo.getColor(android.R.attr.textColorPrimary),
                    ThemesRepo.getColor(android.R.attr.textColorSecondary),
                ),
            ),
        )
        setItemIconTintList(
            ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_enabled, android.R.attr.state_checked),
                    intArrayOf(android.R.attr.state_enabled, -android.R.attr.state_checked),
                ),
                intArrayOf(
                    ThemesRepo.getColor(R.attr.textColorOnAccent),
                    ThemesRepo.getColor(android.R.attr.textColorSecondary),
                ),
            ),
        )
        setItemRippleColor(ColorStateList.valueOf(ThemesRepo.getColor(android.R.attr.colorControlHighlight)))
    }

    override fun getMaxItemCount(): Int = 5
}
