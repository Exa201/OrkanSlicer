package ru.ytkab0bp.slicebeam.view

import android.content.Context
import android.content.res.ColorStateList
import android.view.MotionEvent
import com.google.android.material.materialswitch.MaterialSwitch
import ru.ytkab0bp.slicebeam.R
import ru.ytkab0bp.slicebeam.theme.IThemeView
import ru.ytkab0bp.slicebeam.theme.ThemesRepo

class BeamSwitch(context: Context) : MaterialSwitch(context), IThemeView {
    init {
        onApplyTheme()
    }

    override fun onApplyTheme() {
        setTrackTintList(
            ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_enabled, android.R.attr.state_checked),
                    intArrayOf(android.R.attr.state_enabled, -android.R.attr.state_checked),
                ),
                intArrayOf(
                    ThemesRepo.getColor(android.R.attr.colorAccent),
                    ThemesRepo.getColor(R.attr.switchThumbUncheckedColor),
                ),
            ),
        )
    }

    override fun isLaidOut(): Boolean = true

    override fun dispatchTouchEvent(event: MotionEvent): Boolean = false
}
